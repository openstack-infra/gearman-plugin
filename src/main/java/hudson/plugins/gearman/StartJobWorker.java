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

import hudson.model.Action;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.Executable;
import hudson.model.StringParameterValue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is a gearman function that will start jenkins builds
 *
 * Assumptions:  When this function is created it has an associated
 *      node and project.  The build will start a jenkins build
 *      on its assigned assigned project and node and pass along
 *      all of the parameters from the client.
 *
 * @author Khai Do
 */
public class StartJobWorker extends AbstractGearmanFunction {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_EXECTUOR_LOGGER_NAME);

    Node node;
    Project<?, ?> project;

    public StartJobWorker(Project<?, ?> project, Node node) {
        this.project = project;
        this.node = node;
    }

    /*
     * The Gearman Function
     * @see org.gearman.worker.AbstractGearmanFunction#executeFunction()
     */
    @Override
    public GearmanJobResult executeFunction() {
        logger.info("----- Running executeFunction in " + name + " ----");

        // decode the data from the client
        String decoded = null;
        try {
            decoded = new String((byte[]) this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // convert parameters passed in from client to hash map
        Gson gson = new Gson();
        Map<String, String> inParams = gson.fromJson(decoded,
                new TypeToken<Map<String, String>>() {
                }.getType());

        // need to pass on uuid from client.
        // temporarily passing uuid as a build parameter due to
        // issue: https://answers.launchpad.net/gearman-java/+question/218865

        /*
         * make this node build this project with build params from the client
         */

        // create new parameter objects to pass to jenkins build
        List<ParameterValue> buildParams = new ArrayList<ParameterValue>();
        String uuid = null;
        // create the build parameters that were passed in from client
        for (Map.Entry<String, String> entry : inParams.entrySet()) {
            buildParams.add(new StringParameterValue(entry.getKey(), entry.getValue()));
            // get the build id for debugging
            if (entry.getKey().equals("uuid")) {
                uuid = entry.getValue();
            }

        }

        // set the name of the node to execute build
        String runNodeName = node.getNodeName();
        if (runNodeName.isEmpty()) { // master node name is ""
            runNodeName = "master";
        }

        // create action to run on a specified node
        Action runNode = new NodeAssignmentAction(runNodeName);
        // create action for parameters
        Action params = new NodeParametersAction(buildParams, uuid);
        Action [] actions = {runNode, params};

        logger.info("Scheduling "+project.getName()+" build #" +
                project.getNextBuildNumber()+" on " + runNodeName
                + " with UUID " + uuid + " and build params " + buildParams);

        Future<?> future = project.scheduleBuild2(0, new Cause.UserIdCause(), actions);

        String jobException = "";
        String jobResultMsg = "";

        // jobResult does not change otherwise no results are returned:
        // https://answers.launchpad.net/gearman-java/+question/221348
        boolean jobResult = true;

        try {
            // wait for jenkins build to complete
            Queue.Executable exec = (Executable) future.get();

            // check Jenkins build results
            AbstractBuild<?, ?> currBuild = (AbstractBuild<?, ?>) exec;
            String buildId = currBuild.getId();
            int buildNum = currBuild.number;
            Result result = currBuild.getResult();
            if (result == Result.SUCCESS) {
                jobResultMsg = "Build Success : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + uuid + " and build params " + buildParams;
            } else if (result == Result.ABORTED) {
                jobResultMsg = "Build Aborted : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + uuid + " and build params " + buildParams;
                jobException = jobResultMsg;
            } else if (result == Result.UNSTABLE) {
                jobResultMsg = "Build Unstable : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + uuid + " and build params " + buildParams;
                jobException = jobResultMsg;
            } else if (result == Result.FAILURE) {
                jobResultMsg = "Build failed : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + uuid + " and build params " + buildParams;
                jobException = jobResultMsg;
            } else if (result == Result.NOT_BUILT) {
                jobResultMsg = "Build not done : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + uuid + " and build params " + buildParams;
                jobException = jobResultMsg;
            }

        } catch (InterruptedException e) {
            jobException = e.getMessage();
        } catch (ExecutionException e) {
            jobException = e.getMessage();
        }

        // return result to client
        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), new byte[0], jobException.getBytes(), 0, 0);
        return gjr;
    }
}
