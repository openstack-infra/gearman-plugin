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
     * This method returns the real node name.  Master node
     * by default has an empty string for the name.  But you
     * need to use "master" to tell jenkins to do stuff,
     * namely like schedule a build.
     *
     * @param Node
     *      The node to lookup
     *
     * @return
     *      "master" for the master node or assigned name of the slave node
     */
    public static String getRealName(Node node) {

        if (Computer.currentComputer() == node.toComputer()) {
            return "master";
        } else {
            return node.getNodeName();
        }
    }

}
