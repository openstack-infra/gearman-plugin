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
import hudson.model.Node;

import java.net.InetSocketAddress;
import java.net.Socket;

import jenkins.model.Jenkins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains some useful utilities for this plugin
 *
 * @author Khai Do
 */
public class GearmanPluginUtil {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    /*
     * This method checks whether a connection can be made to a host:port
     *
     * @param host the host name
     *
     * @param port the host port
     *
     * @param timeout the timeout (milliseconds) to try the connection
     *
     * @return true if a socket connection can be established otherwise false
     */
    public static boolean connectionIsAvailable(String host, int port,
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

    /*
     * This method returns the total number of nodes that are active in jenkins,
     * active mean that it's been created, but not necessarily online.
     */
    public static int getNumTotalNodes() {

        // check whether master is enabled
        Node masterNode = null;
        try {
            masterNode = Computer.currentComputer().getNode();
        } catch (Exception e) {
        }

        if (masterNode != null) { // master is enabled, count it
            return Jenkins.getInstance().getNodes().size() + 1;
        } else { // only slaves, no master
            return Jenkins.getInstance().getNodes().size();
        }
    }

}
