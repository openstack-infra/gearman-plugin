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
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Project;

import java.util.List;
import java.util.Scanner;

import jenkins.model.Jenkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This is thread to run gearman executors
 * Executors are used to initiate jenkins builds
 */
public class ExecutorWorkerThread extends AbstractWorkerThread{

    private static final Logger logger = LoggerFactory
            .getLogger(AbstractWorkerThread.class);

    private final Node node;

    public ExecutorWorkerThread(String host, int port, String nodeName){
        super(host, port, nodeName);
        this.node = findNode(nodeName);

    }

    /*
     * This function finds the node with the corresponding node name Returns the
     * node if found, otherwise returns null
     */
    private Node findNode(String nodeName){

        Jenkins jenkins = Jenkins.getInstance();
        List<Node> nodes = jenkins.getNodes();
        Node myNode = null;

        for (Node node : nodes) {
            if (node.getNodeName().equals(nodeName)){
                myNode = node;
            }
        }

        return myNode;
    }

    public ExecutorWorkerThread(String host, int port, String name, Node node){
        super(host, port, name);
        this.node = node;

    }

    @Override
    public void registerJobs() {

        logger.info("----- Registering executor jobs on " + name + " ----");

        /*
         * We start with an empty worker.
         */
        worker.unregisterAll();

        /*
         * Now register or re-register all functions.
         */
        Jenkins jenkins = Jenkins.getInstance();

        List<Project> allProjects = jenkins.getProjects();
        // this call dies with NPE if there is a project without any labels
        // List<AbstractProject> projects =
        // jenkins.getAllItems(AbstractProject.class);
        for (Project<?, ?> project : allProjects) {
            String projectName = project.getName();

            // ignore all disabled projects
            if (!project.isDisabled()) {

                Label label = project.getAssignedLabel();

                if (label == null) { // project has no label -> so register
                                     // "build:projectName"
                    String jobFunctionName = "build:" + projectName;
                    logger.info("Registering job " + jobFunctionName + " on "
                            + node.getNodeName());
                    worker.registerFunctionFactory(new CustomGearmanFunctionFactory(
                            jobFunctionName, StartJobWorker.class.getName(),
                            project, node));

                } else { // register "build:projectName:nodeName"

                    // make sure node is online
                    Computer c = node.toComputer();
                    boolean nodeOnline = c.isOnline();

                    if (nodeOnline) {
                        // get and iterate thru them to build the functions
                        // to register with the worker
                        String projectLabelString = label.getExpression();
                        Scanner projectLabels = new Scanner(projectLabelString);
                        try {
                            projectLabels.useDelimiter("\\|\\|");
                            while (projectLabels.hasNext()) {
                                String projectLabel = projectLabels.next();
                                String jobFunctionName = "build:" + projectName
                                        + ":" + projectLabel;
                                logger.info("Registering job "
                                        + jobFunctionName + " on "
                                        + node.getNodeName());
                                worker.registerFunctionFactory(new CustomGearmanFunctionFactory(
                                        jobFunctionName, StartJobWorker.class
                                                .getName(), project, node));
                            }
                        } finally {
                            projectLabels.close();
                        }
                    }
                }
            }
        }
    }
}
