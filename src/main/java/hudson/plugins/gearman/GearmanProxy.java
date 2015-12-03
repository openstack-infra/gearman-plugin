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
import hudson.model.Run;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    private static GearmanProxy gearmanProxy;

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    // handles to gearman workers
    private final List<ExecutorWorkerThread> gewtHandles;
    private final List<ManagementWorkerThread> gmwtHandles;
    private final String masterName;

    // Singleton instance
    public static synchronized GearmanProxy getInstance() {
        if (gearmanProxy == null) {
            gearmanProxy = new GearmanProxy();
        }
        return gearmanProxy;
    }

    // constructor
    private GearmanProxy() {
        gewtHandles = Collections.synchronizedList(new ArrayList<ExecutorWorkerThread>());
        gmwtHandles = Collections.synchronizedList(new ArrayList<ManagementWorkerThread>());

        Computer master = null;
        String hostname = Constants.GEARMAN_DEFAULT_EXECUTOR_NAME;
        // query Jenkins for master's name
        try {
            master = Jenkins.getActiveInstance().getComputer("");
            hostname = master.getHostName();
        } catch (Exception e) {
            logger.warn("Exception while getting hostname", e);
        }
        // master node may not be enabled so get masterName from system
        if (master == null) {
            try {
                hostname = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.warn("Exception while getting hostname", e);
            }
        }

        masterName = hostname;
    }

    /*
     * This method is for unit tests only.
     */
    protected void testResetHandles() {
        gmwtHandles.clear();
        gewtHandles.clear();
    }

    /*
     * This method initializes the  gearman workers.
     */
    public void initWorkers() {
        /*
         * Purpose here is to create a 1:1 mapping of 'gearman worker':'jenkins
         * executor' then use the gearman worker to execute builds on that
         * jenkins nodes
         */

        /*
         * Spawn management executor worker. This worker does not need any
         * executors. It only needs to work with gearman.
         */
        createManagementWorker();

        /*
         * Spawn executors for the jenkins master Need to treat the master
         * differently than slaves because the master is not the same as a
         * slave
         */
        // first make sure master is enabled (or has executors)
        Node masterNode = null;
        try {
            masterNode = Jenkins.getActiveInstance().getComputer("").getNode();
        } catch (NullPointerException npe) {
            logger.info("---- Master is offline");
        } catch (Exception e) {
            logger.error("Exception while finding master", e);
        }

        if (masterNode != null) {
            Computer computer = masterNode.toComputer();
            if (computer != null) {
                createExecutorWorkersOnNode(computer);
            }
        }

        /*
         * Spawn executors for the jenkins slaves
         */
        List<Node> nodes = Jenkins.getActiveInstance().getNodes();
        if (!nodes.isEmpty()) {
            for (Node node : nodes) {
                Computer computer = node.toComputer();
                if (computer != null) {
                    // create a gearman worker for every executor on the slave
                    createExecutorWorkersOnNode(computer);
                }
            }
        }

        logger.info("---- Num of executors running = " + getNumExecutors());
    }

    /*
     * Spawn management executor workers. This worker does not need any
     * executors. It only needs to connect to gearman.
     */
    public void createManagementWorker() {
        ManagementWorkerThread gwt;

        synchronized (gmwtHandles) {
            if (!gmwtHandles.isEmpty()) {
                return;
            }

            gwt = new ManagementWorkerThread(
                GearmanPluginConfig.get().getHost(),
                GearmanPluginConfig.get().getPort(),
                masterName + "_manager",
                masterName, new NoopAvailabilityMonitor());
            gmwtHandles.add(gwt);
            gwt.start();
        }

        logger.info("---- Num of executors running = " + getNumExecutors());
    }

    /*
     * Spawn workers for each executor on a node.
     */
    public void createExecutorWorkersOnNode(Computer computer) {

        // find the computer in the executor workers list
        synchronized(gewtHandles) {
            for (ExecutorWorkerThread t : gewtHandles) {
                if (t.getComputer() == computer) {
                    logger.info("---- Executor thread already running for " + computer.getName());
                    return;
                }
            }

            AvailabilityMonitor availability = new NodeAvailabilityMonitor(computer);

            int executors = computer.getExecutors().size();
            for (int i = 0; i < executors; i++) {
                String nodeName = null;

                nodeName = GearmanPluginUtil.getRealName(computer);
                if (nodeName == "master") {
                    nodeName = masterName;
                }

                ExecutorWorkerThread ewt = new ExecutorWorkerThread(
                    GearmanPluginConfig.get().getHost(),
                    GearmanPluginConfig.get().getPort(),
                    nodeName+"_exec-"+Integer.toString(i),
                    computer, masterName, availability);

                ewt.start();
                gewtHandles.add(ewt);
            }
        }

        logger.info("---- Num of executors running = " + getNumExecutors());

    }

    /*
     * This method stops all gearman workers
     */
    public void stopAll() {
        // stop gearman executors
        List<AbstractWorkerThread> stopHandles;

        synchronized(gewtHandles) {
            stopHandles = new ArrayList<AbstractWorkerThread>(gewtHandles);
            gewtHandles.clear();
        }

        for (AbstractWorkerThread wt : stopHandles) { // stop executors
            wt.stop();
        }

        synchronized(gmwtHandles) {
            stopHandles = new ArrayList<AbstractWorkerThread>(gmwtHandles);
            gmwtHandles.clear();
        }

        for (AbstractWorkerThread wt : stopHandles) { // stop executors
            wt.stop();
        }

        logger.info("---- Num of executors running = " + getNumExecutors());
    }

    /*
     * This method stops all threads on the gewtHandles list that
     * is used to  service the jenkins slave/computer
     *
     *
     * @param Node
     *      The Computer to stop
     *
     */
    public void stop(Computer computer) {
        logger.info("---- Stop computer " + computer);
        List<ExecutorWorkerThread> workers = new ArrayList<ExecutorWorkerThread>();

        synchronized(gewtHandles) {
            // find the computer in the executor workers list and stop it
            for (Iterator<ExecutorWorkerThread> it = gewtHandles.iterator(); it.hasNext(); ) {
                ExecutorWorkerThread t = it.next();
                if (t.getComputer() == computer) {
                    workers.add(t);
                    it.remove();
                }
            }
        }

        for (ExecutorWorkerThread t : workers) {
            t.stop();
        }

        logger.info("---- Num of executors running = " + getNumExecutors());
    }

    /*
     * This method returns the total number of gearman executor threads
     */
    public int getNumExecutors() {
        return gmwtHandles.size() + gewtHandles.size();
    }

    public void onBuildFinalized(Run r) {
        Computer computer = r.getExecutor().getOwner();
        // A build just finished, so let the AvailabilityMonitor
        // associated with its node wake up any workers who may be
        // waiting for the lock.

        AvailabilityMonitor availability = null;
        synchronized(gewtHandles) {
            for (ExecutorWorkerThread t : gewtHandles) {
                if (t.getComputer() == computer) {
                    availability = t.getAvailability();
                }
            }
        }
        if (availability != null) {
            availability.wake();
        }
    }

    public AvailabilityMonitor getAvailabilityMonitor(Computer computer) {
        synchronized (gewtHandles) {
            for (ExecutorWorkerThread t : gewtHandles) {
                if (t.getComputer() == computer) {
                    return t.getAvailability();
                }
            }
        }
        return null;
    }

    public CauseOfBlockage canTake(Node node,
                                   Queue.BuildableItem item) {
        // Ask the AvailabilityMonitor for this node if it's okay to
        // run this build.
        ExecutorWorkerThread workerThread = null;

        synchronized(gewtHandles) {
            Computer computer = node.toComputer();
            for (ExecutorWorkerThread t : gewtHandles) {
                if (t.getComputer() == computer) {
                    workerThread = t;
                    break;
                }
            }
        }

        if (workerThread != null) {
            if (workerThread.getAvailability().canTake(item)) {
                return null;
            } else {
                return new CauseOfBlockage.BecauseNodeIsBusy(node);
            }
        }
        return null;
    }

    public void registerJobs() {
        synchronized(gewtHandles) {
            for (ExecutorWorkerThread worker : gewtHandles) {
                worker.registerJobs();
            }
        }
    }
}
