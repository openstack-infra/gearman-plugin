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
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

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
    private boolean enablePlugin; // config to enable and disable plugin
    private String host; // gearman server host
    private int port; // gearman server port

    /**
     * Constructor.
     */
    public GearmanPluginConfig() {
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

        if (connectionIsAvailable(host, port, 5000)) {
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

        // save current plugin config so we can compare to new user settings
        String prevHost = this.host;
        int prevPort = this.port;
        boolean prevEnablePlugin = this.enablePlugin;

        // get the new gearman plugin configs from jenkins config page settings
        enablePlugin = json.getBoolean("enablePlugin");
        host = json.getString("host");
        port = json.getInt("port");

        if (!enablePlugin && prevEnablePlugin) {  // gearman-plugin goes from ON to OFF state
            GearmanProxy.getInstance().stopAll();

        } else if (enablePlugin && !prevEnablePlugin) { // gearman-plugin goes from OFF to ON state
            // check for a valid connection to server
            if (!connectionIsAvailable(host, port, 5000)) {
                enablePlugin = false;
                throw new FormException("Unable to connect to Gearman server. "
                            + "Please check the server connection settings and retry.",
                            "host");
            }

            // run workers
            GearmanProxy.getInstance().initWorkers();

        } else if (enablePlugin && prevEnablePlugin) { // gearman-plugin stays in the ON state
            // update connection for a plugin config change
            if (!host.equals(prevHost) || port != prevPort) {

                // stop the workers on the current connected
                GearmanProxy.getInstance().stopAll();

                // check for a valid connection to server
                if (!connectionIsAvailable(host, port, 5000)) {
                    enablePlugin = false;
                    throw new FormException("Unable to connect to Gearman server. "
                                + "Please check the server connection settings and retry.",
                                "host");
                }

                // run workers with new connection
                GearmanProxy.getInstance().initWorkers();
            }

        }

        req.bindJSON(this, json);
        save();
        return true;
    }


    /**
     * This method returns true if the global configuration says we should
     * enable the plugin.
     */
    public boolean enablePlugin() {
        return Objects.firstNonNull(enablePlugin, Constants.GEARMAN_DEFAULT_ENABLE_PLUGIN);
    }

    /**
     * This method returns the value from the server host text box
     */
    public String getHost() {
        return Objects.firstNonNull(host, Constants.GEARMAN_DEFAULT_TCP_HOST);
    }

    /**
     * This method returns the value from the server port text box
     */
    public int getPort() {

        if (port == 0){ // Change default value
            return Constants.GEARMAN_DEFAULT_TCP_PORT;
        } else {
            return port;
        }
    }

    /*
     * This method checks whether a connection is open and available
     * on $host:$port
     *
     * @param host
     *      the host name
     *
     * @param port
     *      the host port
     *
     * @param timeout
     *      the timeout (milliseconds) to try the connection
     *
     * @return boolean
     *      true if a socket connection can be established otherwise false
     */
    private boolean connectionIsAvailable(String host, int port,
            int timeout) {

        InetSocketAddress endPoint = new InetSocketAddress(host, port);
        Socket socket = new Socket();

        if (endPoint.isUnresolved()) {
            System.out.println("Failure " + endPoint);
        } else {
            try {
                socket.connect(endPoint, timeout);
                logger.info("Connection Success:    " + endPoint);
                return true;
            } catch (Exception e) {
                logger.info("Connection Failure:    " + endPoint + " message: "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        logger.info(e.getMessage());
                    }
                }
            }
        }
        return false;
    }

}
