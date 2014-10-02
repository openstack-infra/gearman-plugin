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
import hudson.model.listeners.SaveableListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using the SaveableListener is required as a work around because
 * the itemListener.onUpdate event does not fire on changes to
 * project updates using the Jenkins REST API
 * Bug: https://issues.jenkins-ci.org/browse/JENKINS-25175
 */
@Extension
public class SaveableListenerImpl extends SaveableListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    @Override
    // This works but is NOT good because this event is a catch all
    // for just about any change that happens in Jenkins. This event
    // also doesn't provide much detail on what has changed.
    public void onChange(Saveable o, XmlFile file) {
        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return;
        }

        // only look for changes to projects, specifically for project
        // label changes.  Node changes are handled in ComputerListenerImpl
        if (o instanceof AbstractProject) {
            GearmanProxy.getInstance().registerJobs();
        }
    }
}
