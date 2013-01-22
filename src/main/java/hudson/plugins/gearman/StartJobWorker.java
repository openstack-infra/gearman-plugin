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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.Project;
import hudson.model.StringParameterValue;
import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/*
 * Overview: This is a gearman function that will start jenkins builds
 * Assumptions:  When this function is created it has an associated
 *      node and project.  The build will start a jenkins build
 *      on its assigned assigned project and node and pass along
 *      all of the parameters from the client.
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

    @Override
    public GearmanJobResult executeFunction() {
        logger.info("----- Running executeFunction in " + name + " ----");

        // decode the data
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


        // tried to use a RunParameterValue but not sure what
        // jenkins expects for a "valid id".  doing this way
        // fails with java.lang.IllegalArgumentException: Invalid id
//        buildParams.add(new RunParameterValue("uuid", "16"));

        String uuid = null;
        // create the build parameters that were passed in from client
        for (Map.Entry<String, String> entry : inParams.entrySet()) {
            buildParams.add(new StringParameterValue(entry.getKey(), entry.getValue()));
            // get the build id for debugging
            if (entry.getKey().equals("uuid")) {
                uuid = entry.getValue();
            }

        }

        // schedule the jenkins build
        logger.info("Scheduling build on " + node.getNodeName()
                + " with UUID " + uuid + " and build params " + inParams);

        project.scheduleBuild2(0, new Cause.UserIdCause(), new NodeParametersAction(buildParams, node.getNodeName()));


        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, true,
                decoded.toString().getBytes(), new byte[0], new byte[0], 0, 0);
        return gjr;
    }
}
