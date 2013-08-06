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
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.OfflineCause;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gearman.client.GearmanIOEventListener;
import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.common.GearmanJobServerSession;
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
    AbstractProject<?, ?> project;
    String masterName;
    MyGearmanWorkerImpl worker;

    public StartJobWorker(AbstractProject<?, ?> project, Node node, String masterName,
                          MyGearmanWorkerImpl worker) {
        this.project = project;
        this.node = node;
        this.masterName = masterName;
        this.worker = worker;
    }

   private String buildStatusData(AbstractBuild<?, ?> build) {
       Hudson hudson = Hudson.getInstance();
       AbstractProject<?, ?> project = build.getProject();

       Map data = new HashMap<String, String>();

       data.put("name", project.getName());
       data.put("number", build.getNumber());
       data.put("manager", masterName);
       data.put("worker", this.worker.getWorkerID());

       String rootUrl = Hudson.getInstance().getRootUrl();
       if (rootUrl != null) {
           data.put("url", rootUrl + build.getUrl());
       }

       Result result = build.getResult();
       if (result != null) {
           data.put("result", result.toString());
       }
       Gson gson = new Gson();
       return gson.toJson(data);
   }


    /*
     * The Gearman Function
     * @see org.gearman.worker.AbstractGearmanFunction#executeFunction()
     */
    @Override
    public GearmanJobResult executeFunction() {
        try {
            return safeExecuteFunction();
        } catch (Exception inner) {
            RuntimeException outer = new RuntimeException(inner);
            throw outer;
        }
    }

    private GearmanJobResult safeExecuteFunction()
        throws Exception
    {
        // decode the uniqueId from the client
        String decodedUniqueId = null;
        if (this.uniqueId != null) {
            decodedUniqueId = new String(this.uniqueId, "UTF-8");
        }

        // create new parameter objects to pass to jenkins build
        List<ParameterValue> buildParams = new ArrayList<ParameterValue>();
        String decodedData = null;
        boolean offlineWhenComplete = false;
        if (this.data != null) {
            // decode the data from the client
            decodedData = new String((byte[]) this.data, "UTF-8");
            // convert parameters passed in from client to hash map
            Gson gson = new Gson();
            Map<String, String> inParams = gson.fromJson(decodedData,
                    new TypeToken<Map<String, String>>() {
                    }.getType());
            // set build parameters that were passed in from client
            for (Map.Entry<String, String> entry : inParams.entrySet()) {
                buildParams.add(new StringParameterValue(entry.getKey(), entry.getValue()));
            }
            String offline = inParams.get("OFFLINE_NODE_WHEN_COMPLETE");
            if (offline != null) {
                if (offline.equals("1") || offline.equals("true") ||
                    offline.equals("True") || offline.equals("TRUE")) {
                    offlineWhenComplete = true;
                }
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

        AvailabilityMonitor availability =
            GearmanProxy.getInstance().getAvailabilityMonitor(node);

        availability.expectUUID(decodedUniqueId);

        // schedule jenkins to build project
        logger.info("---- Worker " + this.worker + " scheduling " +
                    project.getName()+" build #" +
                    project.getNextBuildNumber()+" on " + runNodeName
                    + " with UUID " + decodedUniqueId + " and build params " + buildParams);
        QueueTaskFuture<?> future = project.scheduleBuild2(0, new Cause.UserIdCause(), actions);

        // check build and pass results back to client
        boolean jobResult = true;
        String jobFailureMsg = "";
        String jobData;

        // This is a hack that relies on implementation knowledge.  In
        // order to actually send a WORK_STATUS packet before the
        // completion of work, we need to directly drive the session
        // IO, which requires a session object.  We happen to know
        // that's what our event listener is.
        GearmanJobServerSession sess = null;

        for (GearmanIOEventListener listener : listeners) {
            if (listener instanceof GearmanJobServerSession) {
                sess = (GearmanJobServerSession)listener;
            }
        }

        // wait for start of build
        Queue.Executable exec = future.getStartCondition().get();
        AbstractBuild<?, ?> currBuild = (AbstractBuild<?, ?>) exec;

        long now = new Date().getTime();
        int duration = (int) (now - currBuild.getStartTimeInMillis());
        int estimatedDuration = (int) currBuild.getEstimatedDuration();
        jobData = buildStatusData(currBuild);

        sendData(jobData.getBytes());
        sess.driveSessionIO();
        sendStatus(estimatedDuration, duration);
        sess.driveSessionIO();

        while (!future.isDone()) {
            // wait for jenkins build to complete
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                now = new Date().getTime();
                duration = (int) (now - currBuild.getStartTimeInMillis());
                estimatedDuration = (int) currBuild.getEstimatedDuration();
                if (sess != null) {
                    sendStatus(estimatedDuration, duration);
                    sess.driveSessionIO();
                }
            }
        }

        exec = future.get();

        jobData = buildStatusData(currBuild);
        if (sess != null) {
            sendData(jobData.getBytes());
            sess.driveSessionIO();
        }

        if (offlineWhenComplete) {
            Computer computer = node.toComputer();
            if (computer == null) {
                logger.error("---- Worker " + this.worker + " has no " +
                             "computer while trying to take node offline.");
            } else {
                logger.info("---- Worker " + this.worker + " setting " +
                            "node offline.");
                computer.setTemporarilyOffline(true,
                    new OfflineCause.ByCLI("Offline due to Gearman request"));
            }
        }

        // check Jenkins build results
        Result result = currBuild.getResult();
        if (result == Result.SUCCESS) {
            jobResult = true;
        } else {
            jobResult = false;
        }

        // return result to client
        GearmanJobResult gjr = new GearmanJobResultImpl(
                this.jobHandle, jobResult,
                "".getBytes(), "".getBytes(),
                jobFailureMsg.getBytes(), 0, 0);
        return gjr;
    }
}
