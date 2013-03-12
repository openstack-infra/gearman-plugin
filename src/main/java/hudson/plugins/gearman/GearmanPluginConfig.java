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

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to set the global configuration for the gearman-plugin It
 * is also used to enable/disable the gearman plugin.
 *
 * @author Khai Do
 */
@Extension
public class GearmanPluginConfig extends GlobalConfiguration {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);
    private boolean launchWorker; // enable/disable plugin
    private String host; // gearman server host
    private int port; // gearman server port
    GearmanProxy gearmanProxy;

    /**
     * Constructor.
     */
    public GearmanPluginConfig() {
        logger.info("---- GearmanPluginConfig Constructor ---");

        gearmanProxy = new GearmanProxy();
        load();
    }

    public static GearmanPluginConfig get() {
        return GlobalConfiguration.all().get(GearmanPluginConfig.class);
    }

    /*
     * This method runs when user clicks Test Connection button.
     *
     * @return message indicating whether connection test passed or failed
     */
    public FormValidation doTestConnection(
            @QueryParameter("host") final String host,
            @QueryParameter("port") final int port) throws IOException,
            ServletException {

        if (GearmanPluginUtil.connectionIsAvailable(host, port, 5000)) {
            return FormValidation.ok("Success");
        } else {
            return FormValidation.error("Failed: Unable to Connect");
        }
    }

    /*
     * This method runs when user saves the configuration form
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws Descriptor.FormException {

        // set the gearman config from user entered values in jenkins config
        // page
        launchWorker = json.getBoolean("launchWorker");
        host = json.getString("host");
        port = json.getInt("port");

        if (launchWorker) {

            // check for a valid connection to gearman server
            logger.info("---- Check connection to Gearman Server " + host + ":"
                    + port);
            if (!GearmanPluginUtil.connectionIsAvailable(host, port, 5000)) {
                launchWorker = false;
                throw new FormException("Unable to connect to Gearman server. "
                        + "Please check the server connection settings and retry.",
                        "host");
            }

            gearmanProxy.init_worker(host, port);

        } else {
            gearmanProxy.stop_all();
        }

        req.bindJSON(this, json);
        save();
        return true;
    }

    /**
     * This method returns true if the global configuration says we should
     * launch worker.
     */
    public boolean launchWorker() {
        return launchWorker;
    }

    /**
     * This method returns the value from the server host text box
     */
    public String getHost() {
        return host;
    }

    /**
     * This method returns the value from the server port text box
     */
    public int getPort() {
        return port;
    }

}
