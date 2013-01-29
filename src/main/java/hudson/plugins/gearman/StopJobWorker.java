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
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Executable;
import hudson.model.queue.SubTask;

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


        // Cancel jenkins jobs that contain matching uuid from client

        boolean jobResult = cancelBuild(inUuid);
        String jobResultMsg = null;
        if (jobResult){
            jobResultMsg = "Canceled jenkins build " + inUuid;

        } else {
            jobResultMsg = "Could not cancel build " + inUuid;

        }


        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), new byte[0], new byte[0], 0, 0);
        return gjr;
    }

    /**
     * Function to cancel a jenkins build from the jenkins queue
     *
     * @param id
     *      The build Id
     * @return
     *      true if build was cancel, otherwise false
     */
    private boolean cancelBuild (String id) {

        // Cancel jenkins job from the jenkins queue
        Queue queue = Jenkins.getInstance().getQueue();

        if (id.isEmpty() || id == null){ // error checking
            return false;
        }

        if (queue.isEmpty()) {  // do nothing if queue is empty
            return false;
        }

        Queue.Item[] qItems = queue.getItems();
        for (Queue.Item qi : qItems) {
            List<NodeParametersAction> actions = qi
                    .getActions(NodeParametersAction.class);

            for (NodeParametersAction gpa : actions) {

                String jenkinsJobId = gpa.getUuid();

                if (jenkinsJobId.equals(id)) {
                    logger.info("---- Cancelling Jenkins build " + jenkinsJobId
                            + " -------");
                    return queue.cancel(qi);
                }
            }
        }
        return false;
    }

    /**
     * Function to abort a running jenkins build
     *
     * @param id
     *      The build Id
     * @return
     *      true if build was aborted, otherwise false
     */
    private boolean abortBuild (String id) {

        if (id.isEmpty() || id == null){ // error checking
            return false;
        }


        /*
         * iterate over the executors on all the nodes then find the build
         * on that executor with the specified id.
         *
         * I'm able to iterate across the executors but not able to get
         * the build object from the executor to lookup the id parameter value
         */

        // abort running jenkins job
        List<Node> nodes = Jenkins.getInstance().getNodes();

        if (nodes.isEmpty()) {
            return false;
        }

        for (Node node: nodes){

            Computer computer = node.toComputer();
            if (computer.isIdle()) { // ignore all idle slaves
                continue;
            }

            List<Executor> executors = computer.getExecutors();

            for (Executor executor: executors) {

                if (executor.isIdle()) {
                    continue;
                }

                // lookup the running build with the id
                Executable executable = executor.getCurrentExecutable();
                SubTask subtask = executable.getParent();
                Label label = subtask.getAssignedLabel();
                List<NodeParametersAction> params = label.getActions(NodeParametersAction.class);

                for (NodeParametersAction param: params){
                    if (param.getUuid().equals(id)){
                        executor.interrupt();
                        if (executor.interrupted()){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
