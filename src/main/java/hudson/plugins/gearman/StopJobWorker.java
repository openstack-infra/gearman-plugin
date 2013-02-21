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
import java.util.Map;

import jenkins.model.Jenkins;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is a gearman function that will cancel/abort jenkins builds
 *
 * @author Khai Do
 */
public class StopJobWorker extends AbstractGearmanFunction {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_EXECTUOR_LOGGER_NAME);


    /*
     * The Gearman Function
     * @see org.gearman.worker.AbstractGearmanFunction#executeFunction()
     */
    @Override
    public GearmanJobResult executeFunction() {

        String decoded = null;

        logger.info("---- Running executeFunction  in " + this.getName()+ " -------");

        try {
            decoded = new String((byte[]) this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Map<String, String> inParams = gson.fromJson(decoded,
                new TypeToken<Map<String, String>>() {
                }.getType());

        // need to pass on uuid from client.
        // temporarily passing uuid as a build parameter due to
        // issue: https://answers.launchpad.net/gearman-java/+question/218865
        String inUuid = null;
        for (Map.Entry<String, String> entry : inParams.entrySet()) {
            if (entry.getKey().equals("uuid")) {
                inUuid = entry.getValue();
                break;
            }

        }

        boolean abortResult = false;
        if (inUuid != null) {
            // Abort running jenkins build that contain matching uuid
            abortResult = abortBuild(inUuid);
        }

        //TODO: build might be on gearman queue if it's not currently
        // running by jenkins, need to check the gearman queue for the
        // job and remove it.

        String jobResultMsg = "";
        String jobResultEx = "";
        boolean jobResult = true;
        if (abortResult){
            jobResultMsg = "Canceled jenkins build " + inUuid;
        } else {
            jobResultMsg = "Did not cancel jenkins build " + inUuid;
            jobResultEx = "Could not cancel build " + inUuid;
        }

        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), new byte[0], jobResultEx.getBytes(), 0, 0);
        return gjr;
    }

    /**
     * Function to cancel a jenkins build from the jenkins queue
     *
     * @param uuid
     *      The build uuid
     * @return
     *      true if build was cancel, otherwise false
     */
    private boolean cancelBuild (String uuid) {

        Queue queue = Jenkins.getInstance().getQueue();

        if (uuid.isEmpty() || uuid == null){    //NOOP
            return false;
        }

        if (queue.isEmpty()) {  // do nothing if queue is empty
            return false;
        }

        // locate the build with matching uuid
        Queue.Item[] qItems = queue.getItems();
        for (Queue.Item qi : qItems) {
            List<NodeParametersAction> actions = qi
                    .getActions(NodeParametersAction.class);

            for (NodeParametersAction gpa : actions) {

                String jenkinsJobId = gpa.getUuid();

                if (jenkinsJobId.equals(uuid)) {
                    // Cancel jenkins job from the jenkins queue
                    logger.info("---- Cancelling Jenkins build " + jenkinsJobId
                            + " -------");
                    return queue.cancel(qi);
                }
            }
        }
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
