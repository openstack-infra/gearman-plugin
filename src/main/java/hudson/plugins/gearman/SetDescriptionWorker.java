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
import hudson.model.Run;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
 * This is a gearman function to set a jenkins build
 * description
 *
 *
 * @author Khai Do
 */
public class SetDescriptionWorker extends AbstractGearmanFunction {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    /*
     * The Gearman Function
     * @see org.gearman.worker.AbstractGearmanFunction#executeFunction()
     */
    @Override
    public GearmanJobResult executeFunction() {

        // check job results
        boolean jobResult = false;
        String jobExceptionMsg = "";
        String jobWarningMsg = "";
        String jobResultMsg = "";

        try {
            // decode json
            String decodedData = new String((byte[]) this.data, "UTF-8");
            // convert parameters passed in from client to hash map
            Gson gson = new Gson();
            Map<String, String> data = gson.fromJson(decodedData,
                    new TypeToken<Map<String, String>>() {
                    }.getType());

            // get build description
            String buildDescription = data.get("description");
            // get build id
            String buildId = data.get("build_id");
            String[] idToken = buildId.split(":");
            if (idToken.length != 2 || buildDescription == null || buildId == null) {
                jobExceptionMsg = "Invalid Unique Id";
                throw new IllegalArgumentException(jobExceptionMsg);
            } else {
                String jobName = idToken[0];
                String jobId = idToken[1];
                if (!jobName.isEmpty() && !jobId.isEmpty()) {
                    // find build then update its description
                    Run<?,?> build = findBuild(jobName, jobId);
                    if (build != null) {
                        build.setDescription(buildDescription);
                        jobResultMsg = "Description for Jenkins build " +buildId+" was pdated to " + buildDescription;
                        jobResult = true;
                    } else {
                        jobExceptionMsg = "Cannot find build with id " + buildId;
                        throw new IllegalArgumentException(jobExceptionMsg);
                    }
                } else {
                    jobExceptionMsg = "Build id is invalid or not specified";
                    throw new IllegalArgumentException(jobExceptionMsg);
                }
            }
        } catch (UnsupportedEncodingException e) {
            jobExceptionMsg = "Error decoding parameters";
        } catch (NullPointerException e) {
            jobExceptionMsg = "Error decoding parameters";
        } catch (IOException e) {
            jobExceptionMsg = "Error setting build description";
        } catch (IllegalArgumentException e) {
            jobExceptionMsg = e.getMessage();
        }

        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), jobWarningMsg.getBytes(),
                jobExceptionMsg.getBytes(), 0, 0);
        return gjr;
    }


    /**
     * Function to finds the build with the unique build id.
     *
     * @param jobName
     *      The jenkins job or project name
     * @param uuid
     *      The jenkins job id
     * @return
     *      the build Run if found, otherwise return null
     */
    private Run<?,?> findBuild(String jobName, String jobId) {

        AbstractProject<?,?> project = Jenkins.getInstance().getItemByFullName(jobName, AbstractProject.class);
        if (project != null){
            Run<?,?> run = project.getBuild(jobId);
            if (run != null) {
                return run;
            }
        }
        return null;
    }
}
