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

import hudson.model.Node;

import java.util.List;

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

    private Node node;

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

    }

}
