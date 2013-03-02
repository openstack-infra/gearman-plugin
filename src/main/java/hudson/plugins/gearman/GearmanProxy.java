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

import java.util.List;
import java.util.Stack;

import jenkins.model.Jenkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanProxy {



    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    // handles to gearman workers
//    public static List<AbstractWorkerThread> gewtHandles;
//    public static List<AbstractWorkerThread> gmwtHandles;
    public static Stack<AbstractWorkerThread> gewtHandles;
    public static Stack<AbstractWorkerThread> gmwtHandles;
    public static int numExecutorNodes;

    public GearmanProxy() {
        // TODO Auto-generated constructor stub

//        gewtHandles = new ArrayList<AbstractWorkerThread>();
//        gmwtHandles = new ArrayList<AbstractWorkerThread>();
        gewtHandles = new Stack<AbstractWorkerThread>();
        gmwtHandles = new Stack<AbstractWorkerThread>();
        numExecutorNodes = 0;


    }

    public void init_worker(String host, int port) {
        /*
         * Purpose here is to create a 1:1 mapping of 'gearman
         * worker':'jenkins executor' then use the gearman worker to execute
         * builds on that jenkins nodes
         */
        if (gmwtHandles.isEmpty() && gewtHandles.isEmpty()) {

            /*
             * Spawn management executor.  This worker does not need any
             * executors.  It only needs to work with gearman.
             */
            AbstractWorkerThread gwt = null;
            gwt = new ManagementWorkerThread(host, port, host);
            gwt.registerJobs();
            gwt.start();
            gmwtHandles.add(gwt);

            /*
             * Spawn executors for the jenkins master
             * Need to treat the master differently than slaves because
             * the master is not the same as a slave
             */
            // make sure master is enabled (or has executors)
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
                for (int i=0; i<executors; i++) {
                    // create a gearman executor for every jenkins executor
                    gwt  = new ExecutorWorkerThread(host, port,
                            "master-exec"+Integer.toString(i), masterNode);
                    gwt.registerJobs();
                    gwt.start();
                    gewtHandles.add(gwt);
                }
            }

            /*
             * Spawn executors for the jenkins slaves
             */
            List<Node> nodes = Jenkins.getInstance().getNodes();
            if (!nodes.isEmpty()) {
                for (Node node : nodes) {
                    Computer computer = node.toComputer();
//                    if (computer.isOnline()) {
                        // create a gearman executor for every jenkins executor
                        int slaveExecutors = computer.getExecutors().size();
                        for (int i=0; i<slaveExecutors; i++) {
                            gwt  = new ExecutorWorkerThread(host, port,
                                    node.getNodeName()+"-exec"+Integer.toString(i), node);
                            gwt.registerJobs();
                            gwt.start();
                            gewtHandles.add(gwt);
                        }
//                    }
                    numExecutorNodes++;
                }
            }
        }
        logger.info("--- Num of executors running = "+getRunningExecutors());

    }

    public void stop_all() {
        //stop gearman executors
//        for (int i=0; i<gewtHandles.size(); i++) {
//            gewtHandles.get(i).stop();
//            gewtHandles.remove(i);
//        }
//        for (AbstractWorkerThread gewtHandle : gewtHandles) {
//            gewtHandle.stop();
//        }
//        gewtHandles.clear();

//        for (int i=0; i<gmwtHandles.size(); i++) {
//            gmwtHandles.get(i).stop();
//            gmwtHandles.remove(i);
//        }

//        for (AbstractWorkerThread gmwtHandle : gmwtHandles) {
//            gmwtHandle.stop();
//        }
//        gmwtHandles.clear();

        for (AbstractWorkerThread gewtHandle : gewtHandles) {
            gewtHandles.pop().stop();
        }
          for (AbstractWorkerThread gmwtHandle : gmwtHandles) {
              gmwtHandles.pop().stop();
          }

        logger.info("--- Num of executors running = "+getRunningExecutors());
    }

    public int getRunningExecutors() {

        return gmwtHandles.size()+gewtHandles.size();

    }

}
