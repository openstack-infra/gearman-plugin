/*
 *
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package hudson.plugins.gearman;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Stack;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to set the global configuration for the gearman-plugin It
 * is also used to launch gearman workers on this Jenkins server
 *
 * @author Khai Do
 */
@Extension
public class GearmanPluginConfig extends GlobalConfiguration {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);
    public static boolean launchWorker; // launchWorker state (from UI checkbox)
    private String host; // gearman server host
    private int port; // gearman server port

    // handles to gearman workers
    public static List<AbstractWorkerThread> gewtHandles;
    public static List<AbstractWorkerThread> gmwtHandles;

    public static int numExecutorNodes;

    /**
     * Constructor.
     */
    public GearmanPluginConfig() {
        logger.info("--- GearmanPluginConfig Constructor ---");

        gewtHandles = new Stack<AbstractWorkerThread>();
        gmwtHandles = new Stack<AbstractWorkerThread>();
        numExecutorNodes = 0;

        load();

        /*
         * Not sure when to register gearman functions yet so for now always
         * initialize the launch worker flag to disabled state at jenkins
         * startup so we are always at a known state
         */
        this.launchWorker = Constants.GEARMAN_DEFAULT_LAUNCH_WORKER;
        save();
    }

    /*
     * This method checks whether a connection can be made to a host:port
     *
     * @param host
     *  the host name
     *
     * @param port
     *  the host port
     *
     * @param timeout
     *  the timeout (milliseconds) to try the connection
     *
     * @return
     *  true if a socket connection can be established otherwise false
     */
    public boolean connectionIsAvailable(String host, int port, int timeout) {

        InetSocketAddress endPoint = new InetSocketAddress(host, port);
        Socket socket = new Socket();

        if (endPoint.isUnresolved()) {
            System.out.println("Failure " + endPoint);
        } else {
            try {
                socket.connect(endPoint, timeout);
                logger.info("Connection Success:    "+endPoint);
                return true;
            } catch (Exception e) {
                logger.info("Connection Failure:    "+endPoint+" message: "
                        +e.getClass().getSimpleName()+" - "
                        +e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        logger.info(e.getMessage());
                    }
                }
            }
        }
        return false;
    }

    /*
     * This method runs when user clicks Test Connection button.
     *
     * @return
     *  message indicating whether connection test passed or failed
     */
    public FormValidation doTestConnection(
            @QueryParameter("host") final String host,
            @QueryParameter("port") final int port) throws IOException,
            ServletException {

        if (connectionIsAvailable(host, port, 5000)) {
            return FormValidation.ok("Success");
        } else {
            return FormValidation.error("Failed: Unable to Connect");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws Descriptor.FormException {

        // set the gearman config from user entered values in jenkins config page
        launchWorker = json.getBoolean("launchWorker");
        host = json.getString("host");
        port = json.getInt("port");

        /*
         * Purpose here is to create a 1:1 mapping of 'gearman worker':'jenkins
         * executor' then use the gearman worker to execute builds on that
         * jenkins nodes
         */
        if (launchWorker && gmwtHandles.isEmpty() && gewtHandles.isEmpty()) {

            // check for a valid connection to gearman server
            logger.info("--- Check connection to Gearman Server " + getHost() + ":"
                    + getPort());
            if (!connectionIsAvailable(host, port, 5000)) {
                this.launchWorker = false;
                throw new RuntimeException(
                        "Could not get connection to Gearman Server " + getHost()
                                + ":" + getPort());
            }

            /*
             * Spawn management executor worker. This worker does not need any
             * executors. It only needs to work with gearman.
             */
            AbstractWorkerThread gwt = null;
            gwt = new ManagementWorkerThread(host, port, host);
            gwt.registerJobs();
            gwt.start();
            gmwtHandles.add(gwt);

            /*
             * Spawn executors for the jenkins master Need to treat the master
             * differently than slaves because the master is not the same as a
             * slave
             */
            // first make sure master is enabled (or has executors)
            Node masterNode = null;
            try {
                masterNode = Computer.currentComputer().getNode();
            } catch (NullPointerException npe) {
                logger.info("--- Master is offline");
            } catch (Exception e) {
                logger.info("--- Can't get Master");
                e.printStackTrace();
            }

            if (masterNode != null) {
                Computer computer = masterNode.toComputer();
                int executors = computer.getExecutors().size();
                for (int i = 0; i < executors; i++) {
                    // create a gearman worker for every executor on the master
                    gwt = new ExecutorWorkerThread(host, port, "master-exec"
                            + Integer.toString(i), masterNode);
                    gwt.registerJobs();
                    gwt.start();
                    gewtHandles.add(gwt);
                }
                numExecutorNodes++;
            }

            /*
             * Spawn executors for the jenkins slaves
             */
            List<Node> nodes = Jenkins.getInstance().getNodes();
            if (!nodes.isEmpty()) {
                for (Node node : nodes) {
                    Computer computer = node.toComputer();
                    // create a gearman worker for every executor on the slave
                    int slaveExecutors = computer.getExecutors().size();
                    for (int i = 0; i < slaveExecutors; i++) {
                        gwt = new ExecutorWorkerThread(host, port,
                                node.getNodeName() + "-exec"
                                        + Integer.toString(i), node);
                        gwt.registerJobs();
                        gwt.start();
                        gewtHandles.add(gwt);
                    }
                    numExecutorNodes++;
                }
            }
        }

        // stop gearman workers
        if (!launchWorker) {
            for (AbstractWorkerThread gewtHandle : gewtHandles) { // stop executors
                gewtHandle.stop();
            }
            gewtHandles.clear();

            for (AbstractWorkerThread gmwtHandle : gmwtHandles) { // stop executors
                gmwtHandle.stop();
            }
            gmwtHandles.clear();
            numExecutorNodes = 0;
        }

        int runningExecutors = gmwtHandles.size() + gewtHandles.size();
        logger.info("--- Num of executors running = " + runningExecutors);

        req.bindJSON(this, json);
        save();
        return true;
    }

    /**
     * This method returns true if the global configuration says we should
     * launch worker.
     */
    public boolean launchWorker() {
        return launchWorker;
    }

    /**
     * This method returns the value from the server host text box
     */
    public String getHost() {
        return host;
    }

    /**
     * This method returns the value from the server port text box
     */
    public int getPort() {
        return port;
    }

}
