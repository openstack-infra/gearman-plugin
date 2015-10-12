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

import hudson.model.Run;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

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
        String jobResultMsg = "";

        String decodedData;
        // decode json
        try {
            decodedData = new String((byte[]) this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported encoding exception in argument");
        }

        // convert parameters passed in from client to hash map
        Gson gson = new Gson();
        Map<String, String> data = gson.fromJson(decodedData,
                new TypeToken<Map<String, String>>() {
                }.getType());

        // get build description
        String buildDescription = data.get("html_description");
        // get build id
        String jobName = data.get("name");
        String buildNumber = data.get("number");
        if (!jobName.isEmpty() && !buildNumber.isEmpty()) {
            // find build then update its description
            Run<?,?> build = GearmanPluginUtil.findBuild(jobName, Integer.parseInt(buildNumber));
            if (build != null) {
                try {
                    GearmanPluginUtil.setBuildDescription(build, buildDescription);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to set description for " +
                                                       jobName + ": " + buildNumber);
                }
                jobResultMsg = "Description for Jenkins build " +buildNumber+" was updated to " + buildDescription;
                jobResult = true;
            } else {
                throw new IllegalArgumentException("Cannot find build number " +
                                                   buildNumber);
            }
        } else {
            throw new IllegalArgumentException("Build id is invalid or not specified");
        }

        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, jobResult,
                jobResultMsg.getBytes(), null, null, 0, 0);
        return gjr;
    }
}
