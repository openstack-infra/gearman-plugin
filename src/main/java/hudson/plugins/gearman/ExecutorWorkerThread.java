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

import hudson.model.AbstractProject;
import hudson.model.Label;
import hudson.model.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import jenkins.model.Jenkins;

import org.gearman.worker.GearmanFunctionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * This is the thread to run gearman executors
 * Executors are used to initiate jenkins builds
 *
 * @author Khai Do
 *
 */
public class ExecutorWorkerThread extends AbstractWorkerThread{

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    private final Node node;

    private HashMap<String,GearmanFunctionFactory> functionMap;

    // constructor
    public ExecutorWorkerThread(String host, int port, String name, Node node) {
        super(host, port, name);
        this.node = node;
        this.functionMap = new HashMap<String,GearmanFunctionFactory>();
    }

    /**
     * This function tokenizes the labels in a label string
     * that is set in the jenkins projects
     *
     * @param label
     *      The label string.
     * @param pattern
     *      The pattern for tokenizing the label.
     * @return
     *      A list of labels, the list can be empty
     */
    private Set<String> tokenizeLabelString(String label, String pattern) {

        Set<String> labelSet = new HashSet<String>();

        if (pattern == null) {
            return labelSet;
        }

        if (pattern.isEmpty()) {
            return labelSet;
        }

        if (label != null) {

            Scanner slabel = new Scanner(label);
            try {
                slabel.useDelimiter(pattern);
                while (slabel.hasNext()) {
                    String newLabel = slabel.next();
                    labelSet.add(newLabel);

                }
            } finally {
                slabel.close();
            }
        }
        return labelSet;
    }

    /**
     * Register gearman functions on this node.  This will unregister all
     * functions before registering new functions.  Works for free-style
     * and maven projects but does not work for multi-config projects
     *
     * How functions are registered:
     *  - If the project has no label then we register the project with all
     *      nodes
     *
     *      build:pep8 on precise-123
     *      build:pep8 on oneiric-456
     *
     *  - If the project contains one label then we register with the node
     *      that contains the corresponding label. Labels with '&&' is
     *      considered just one label
     *
     *      build:pep8:precise on precise-123
     *      build:pep8 on precise-123
     *      build:pep8:precise on precise-129
     *      build:pep8 on precise-129
     *
     *  - If the project contains multiple labels separated by '||' then
     *      we register with the nodes that contain the corresponding labels
     *
     *      build:pep8:precise on precise-123
     *      build:pep8 on precise-123
     *      build:pep8:precise on precise-129
     *      build:pep8 on precise-129
     *      build:pep8:oneiric on oneiric-456
     *      build:pep8 on oneiric-456
     *      build:pep8:oneiric on oneiric-459
     *      build:pep8 on oneiric-459
     *
     */
    @Override
    public void registerJobs() {
        HashMap<String,GearmanFunctionFactory> newFunctionMap = new HashMap<String,GearmanFunctionFactory>();

        if (!this.node.toComputer().isOffline()) {
            List<AbstractProject> allProjects = Jenkins.getInstance().getAllItems(AbstractProject.class);
            for (AbstractProject<?, ?> project : allProjects) {

                if (project.isDisabled()) { // ignore all disabled projects
                    continue;
                }

                String projectName = project.getName();
                Label label = project.getAssignedLabel();

                if (label == null) { // project has no label -> so register
                                     // "build:projectName" on all nodes
                    String jobFunctionName = "build:" + projectName;
                    newFunctionMap.put(jobFunctionName, new CustomGearmanFunctionFactory(
                            jobFunctionName, StartJobWorker.class.getName(),
                            project, this.node));
                } else { // register "build:$projectName:$projectLabel" if this
                         // node matches a node from the project label

                    Set<Node> projectLabelNodes = label.getNodes();
                    String projectLabelString = label.getExpression();
                    Set<String> projectLabels = tokenizeLabelString(
                        projectLabelString, "\\|\\|");

                    // iterate thru all project labels and find matching nodes
                    for (String projectLabel : projectLabels) {
                        if (projectLabelNodes.contains(this.node)) {
                            String jobFunctionName = "build:" + projectName
                                + ":" + projectLabel;
                            // register with label (i.e. "build:$projectName:$projectLabel")
                            newFunctionMap.put(jobFunctionName, new CustomGearmanFunctionFactory(
                                    jobFunctionName, StartJobWorker.class.getName(),
                                    project, this.node));
                            jobFunctionName = "build:" + projectName;
                            // also register without label (i.e. "build:$projectName")
                            newFunctionMap.put(jobFunctionName, new CustomGearmanFunctionFactory(
                                    jobFunctionName, StartJobWorker.class.getName(),
                                    project, this.node));
                        }
                    }
                }
            }
        }
        if (!newFunctionMap.keySet().equals(functionMap.keySet())) {
            functionMap = newFunctionMap;
            Set<GearmanFunctionFactory> functionSet = new HashSet<GearmanFunctionFactory>(functionMap.values());
            updateJobs(functionSet);
        }
    }

    public synchronized Node getNode() {
        return node;
    }
}
