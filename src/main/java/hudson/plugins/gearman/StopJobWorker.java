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

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;

import java.io.UnsupportedEncodingException;
import java.util.List;

import jenkins.model.Jenkins;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a gearman function that will cancel/abort jenkins builds
 *
 * @author Khai Do
 */
public class StopJobWorker extends AbstractGearmanFunction {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    /*
     * The Gearman Function
     * @see org.gearman.worker.AbstractGearmanFunction#executeFunction()
     */
    @Override
    public GearmanJobResult executeFunction() {

        // decode the uniqueId from the client
        String decodedUniqueId = null;
        if (this.uniqueId != null) {
            try {
                decodedUniqueId = new String(this.uniqueId, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        boolean abortResult = false;
        if (decodedUniqueId != null) {
            // Abort running jenkins build that contain matching uuid
            abortResult = abortBuild(decodedUniqueId);
        }

        //TODO: build might be on gearman queue if it's not currently
        // running by jenkins, need to check the gearman queue for the
        // job and remove it.

        String jobResultMsg = "";
        String jobResultEx = "";
        boolean jobResult = true;
        if (abortResult){
            jobResultMsg = "Canceled jenkins build " + decodedUniqueId;
        } else {
            jobResultMsg = "Did not cancel jenkins build " + decodedUniqueId;
            jobResultEx = "Could not cancel build " + decodedUniqueId;
        }

        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), new byte[0], jobResultEx.getBytes(), 0, 0);
        return gjr;
    }

    /**
     * Function to cancel a Gearman job from the Gearman queue
     *
     * @param uuid
     *      The build uuid
     * @return
     *      true if job was cancel, otherwise false
     */
    private boolean cancelJob (String uuid) {

        //TODO:  Need to cancel job from gearman queue, not sure how to
        //      do it yet.
        return false;
    }

    /**
     * Function to abort a currently running Jenkins build
     * Running Jenkins builds are builds that actively being
     * executed by Jenkins
     *
     * @param uuid
     *      The build UUID
     * @return
     *      true if build was aborted, otherwise false
     */
    private boolean abortBuild (String uuid) {

        if (uuid.isEmpty() || uuid == null){ //NOOP
            return false;
        }

        /*
         * iterate over the executors on master and slave nodes to find the
         * build on the executor with the matching uuid
         */
        // look at executors on master
        Node masterNode = Computer.currentComputer().getNode();
        Computer masterComp = masterNode.toComputer();
        if (!masterComp.isIdle()) { // ignore idle master
            List<Executor> masterExecutors = masterComp.getExecutors();
            for (Executor executor: masterExecutors) {

                if (executor.isIdle()) {    // ignore idle executors
                    continue;
                }

                // lookup the running build with matching uuid
                Queue.Executable executable = executor.getCurrentExecutable();
                AbstractBuild<?, ?> currBuild = (AbstractBuild) executable;
                int buildNum = currBuild.getNumber();
                String buildId = currBuild.getId();
                String runNodeName = currBuild.getBuiltOn().getNodeName();
                NodeParametersAction param = currBuild.getAction(NodeParametersAction.class);
                String buildParams = param.getParameters().toString();

                if (param.getUuid().equals(uuid)) {

                    logger.info("Aborting build : "+buildNum+": "+buildId+" on " + runNodeName
                            +" with UUID " + uuid + " and build params " + buildParams);

                    // abort the running jenkins build
                    if (!executor.isInterrupted()) {
                        executor.interrupt();
                        return true;
                    }
                }
            }
        }

        // look at executors on slave nodes
        List<Node> nodes = Jenkins.getInstance().getNodes();
        if (nodes.isEmpty()) {  //NOOP
            return false;
        }

        for (Node node: nodes){

            Computer slave = node.toComputer();
            if (slave.isIdle()) { // ignore all idle slaves
                continue;
            }

            List<Executor> executors = slave.getExecutors();
            for (Executor executor: executors) {

                if (executor.isIdle()) {    // ignore idle executors
                    continue;
                }

                // lookup the running build with matching uuid
                Queue.Executable executable = executor.getCurrentExecutable();
                AbstractBuild<?, ?> currBuild = (AbstractBuild) executable;
                int buildNum = currBuild.getNumber();
                String buildId = currBuild.getId();
                String runNodeName = currBuild.getBuiltOn().getNodeName();
                NodeParametersAction param = currBuild.getAction(NodeParametersAction.class);
                String buildParams = param.getParameters().toString();

                if (param.getUuid().equals(uuid)) {

                    logger.info("Aborting build : "+buildNum+": "+buildId+" on " + runNodeName
                            +" with UUID " + uuid + " and build params " + buildParams);

                    // abort the running jenkins build
                    if (!executor.isInterrupted()) {
                        executor.interrupt();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
