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

import hudson.model.Computer;
import hudson.model.Node;

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to startup and shutdown the gearman workers.
 * It is also used to keep gearman plugin state info.
 *
 * @author Khai Do
 */
public class GearmanProxy {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    // handles to gearman workers
    private static List<AbstractWorkerThread> gewtHandles;
    private static List<AbstractWorkerThread> gmwtHandles;

    // keep track of number of computers that are tied to gearman workers
    private static int numWorkerNodes;


    // constructor
    public GearmanProxy() {
        logger.info("--- GearmanProxy Constructor ---");

        gewtHandles = new ArrayList<AbstractWorkerThread>();
        gmwtHandles = new ArrayList<AbstractWorkerThread>();
        numWorkerNodes = 0;
    }


    /*
     * This method initializes the  gearman workers.
     *
     * @param host the host name
     *
     * @param port the host port
     *
     */
    public void init_worker(String host, int port) {

        /*
         * Purpose here is to create a 1:1 mapping of 'gearman worker':'jenkins
         * executor' then use the gearman worker to execute builds on that
         * jenkins nodes
         */
        if (getNumExecutors() == 0) {

            /*
             * Spawn management executor worker. This worker does not need any
             * executors. It only needs to work with gearman.
             */
            AbstractWorkerThread gwt = new ManagementWorkerThread(host, port, host);
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
                if (computer != null) {
                    int executors = computer.getExecutors().size();
                    for (int i = 0; i < executors; i++) {
                        // create a gearman worker for every executor on the master
                        gwt = new ExecutorWorkerThread(host, port, "master-exec"
                                + Integer.toString(i), masterNode);
                        gwt.registerJobs();
                        gwt.start();
                        gewtHandles.add(gwt);
                    }
                    numWorkerNodes++;
                }
            }

            /*
             * Spawn executors for the jenkins slaves
             */
            List<Node> nodes = Jenkins.getInstance().getNodes();
            if (!nodes.isEmpty()) {
                for (Node node : nodes) {
                    Computer computer = node.toComputer();
                    if (computer != null) {
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
                    }
                    numWorkerNodes++;
                }
            }
        }

        logger.info("--- Num of executors running = " + getNumExecutors());
    }


    /*
     * This method stops all gearman workers
     */
    public void stop_all() {
        // stop gearman executors
        for (AbstractWorkerThread gewtHandle : gewtHandles) { // stop executors
            gewtHandle.stop();
        }
        gewtHandles.clear();

        for (AbstractWorkerThread gmwtHandle : gmwtHandles) { // stop executors
            gmwtHandle.stop();
        }
        gmwtHandles.clear();
        numWorkerNodes = 0;

        logger.info("--- Num of executors running = " + getNumExecutors());
    }

    /*
     * This method returns the total number of gearman executor threads
     */
    public int getNumExecutors() {
        return gmwtHandles.size() + gewtHandles.size();
    }

    /*
     * This method returns the list of gearman executor workers
     */
    public static synchronized List<AbstractWorkerThread> getGewtHandles() {
        return gewtHandles;
    }

    /*
     * This method returns the list of gearman management workers
     */
    public static synchronized List<AbstractWorkerThread> getGmwtHandles() {
        return gmwtHandles;
    }

    /*
     * This method returns the number of worker nodes
     */
    public static synchronized int getNumWorkerNodes() {
        return numWorkerNodes;
    }

    /*
     * This method sets the number of worker nodes
     */
    public static synchronized void setNumWorkerNodes(int numWorkerNodes) {
        GearmanProxy.numWorkerNodes = numWorkerNodes;
    }

}
