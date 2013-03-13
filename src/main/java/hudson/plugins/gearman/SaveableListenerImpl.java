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
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.listeners.SaveableListener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using the SaveableListener is required as a work around because
 * updating gearman function in ComputerListener.onConfigurationChange
 * doesn't work.
 */
@Extension
public class SaveableListenerImpl extends SaveableListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    /*
     * TODO:  this works, but is NOT GOOD!
     *  This listener fires for every change to an object.  So if you
     *  have 3 nodes in Jenkins it will fire 3 times even though
     *  the change was only applied to one of the nodes.
     *
     */
    @Override
    public void onChange(Saveable o, XmlFile file) {
        logger.info("---- " + SaveableListenerImpl.class.getName() + ":"
                + " onChange");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update for when any changes are applied to a project or node
        boolean doUpdate = o instanceof Node || o instanceof AbstractProject;
        if (doUpdate) {
            List<ExecutorWorkerThread> workers = GearmanProxy.getGewtHandles();
            synchronized(workers) {
                if (!workers.isEmpty()) {
                    for (AbstractWorkerThread worker : workers) {
                        worker.registerJobs();
                    }
                }
            }
        }
    }
}
