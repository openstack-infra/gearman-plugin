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
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

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
        logger.info("---- Running executeFunction in " + name + " ----");

        // decode the uniqueId from the client
        String decodedUniqueId = null;
        if (this.uniqueId != null) {
            try {
                decodedUniqueId = new String(this.uniqueId, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        // create new parameter objects to pass to jenkins build
        List<ParameterValue> buildParams = new ArrayList<ParameterValue>();
        String decodedData = null;
        if (this.data != null) {
            // decode the data from the client
            try {
                decodedData = new String((byte[]) this.data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // convert parameters passed in from client to hash map
            Gson gson = new Gson();
            Map<String, String> inParams = gson.fromJson(decodedData,
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            // set build parameters that were passed in from client
            for (Map.Entry<String, String> entry : inParams.entrySet()) {
                buildParams.add(new StringParameterValue(entry.getKey(), entry.getValue()));
            }
        }

        /*
         * make this node build this project with unique id and build params from the client
         */
        String runNodeName = GearmanPluginUtil.getRealName(node);

        // create action to run on a specified node
        Action runNode = new NodeAssignmentAction(runNodeName);
        // create action for parameters
        Action params = new NodeParametersAction(buildParams, decodedUniqueId);
        Action [] actions = {runNode, params};

        // schedule jenkins to build project
        logger.info("---- Scheduling "+project.getName()+" build #" +
                project.getNextBuildNumber()+" on " + runNodeName
                + " with UUID " + decodedUniqueId + " and build params " + buildParams);
        Future<?> future = project.scheduleBuild2(0, new Cause.UserIdCause(), actions);

        // check build and pass results back to client
        boolean jobResult = true;
        String jobFailureMsg = "";
        String jobWarningMsg = "";
        String jobResultMsg = "";

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
                        + " with UUID " + decodedUniqueId + " and build params " + buildParams;
            } else if (result == Result.ABORTED) {
                jobResultMsg = "Build Aborted : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + decodedUniqueId + " and build params " + buildParams;
            } else if (result == Result.UNSTABLE) {
                jobFailureMsg = "Build Unstable : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + decodedUniqueId + " and build params " + buildParams;
                jobResult = false;
            } else if (result == Result.FAILURE) {
                jobFailureMsg = "Build failed : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + decodedUniqueId + " and build params " + buildParams;
                jobResult = false;
            } else if (result == Result.NOT_BUILT) {
                jobWarningMsg = "Build not done : "+buildNum+": "+buildId+" on " + runNodeName
                        + " with UUID " + decodedUniqueId + " and build params " + buildParams;
                jobResult = false;
            }

        } catch (InterruptedException e) {
            jobFailureMsg = e.getMessage();
            jobResult = false;
        } catch (ExecutionException e) {
            jobFailureMsg = e.getMessage();
            jobResult = false;
        }

        // return result to client
        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), jobWarningMsg.getBytes(),
                jobFailureMsg.getBytes(), 0, 0);
        return gjr;
    }
}
