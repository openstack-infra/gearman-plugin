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
import java.util.Map;

import hudson.model.Cause;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.labels.LabelAssignmentAction;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class StartJobWorker extends AbstractGearmanFunction {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_EXECTUOR_LOGGER_NAME);

    Node node;
    Project<?, ?> project;

    public StartJobWorker(Project<?, ?> project, Node node) {
        this.project = project;
        this.node = node;
    }

    public GearmanJobResult executeFunction() {
        logger.info("----- Registering management jobs on " + name + " ----");

        // decode the data
        String decoded = null;
        try {
            decoded = new String((byte[]) this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Map<String, String> params = gson.fromJson(decoded,
                new TypeToken<Map<String, String>>() {
                }.getType());

        // send build to node with parameters
        LabelAssignmentAction laa = new LabelAssignmentActionImpl(
                node.getNodeName());
        System.out.println("Sending " + getName() + " to " + node.getNodeName()
                + " with build params " + params);
        project.scheduleBuild2(0, new Cause.UserIdCause(), laa);

        GearmanJobResult gjr = new GearmanJobResultImpl(this.jobHandle, true,
                decoded.toString().getBytes(), new byte[0], new byte[0], 0, 0);
        return gjr;
    }
}
