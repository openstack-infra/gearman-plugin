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

import hudson.Launcher;
import hudson.Extension;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Descriptor;

import net.sf.json.JSONObject;

/**
 * GearmanPlugin {@link Builder}.
 *
 * <p>
 * This sets up the gearman plugin as another plugin in Jenkins
 * It will allow us to start and stop the gearman workers.
 * <p>
 *
 * @author Khai Do
 */
public class GearmanPlugin extends Builder {

    private static final Logger logger = LoggerFactory
            .getLogger(GearmanPlugin.class);
    private final String name;

    @DataBoundConstructor
    public GearmanPlugin(String name) {
        logger.info("--- GearmanPlugin Constructor ---" + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Builder> {
        
        private static final Logger logger = LoggerFactory
                .getLogger(DescriptorImpl.class);
        private boolean launchWorker; // launchWorker state (from UI checkbox)
        private String host; // gearman server host
        private int port; // gearman server port

        public DescriptorImpl() {
            logger.info("--- DescriptorImpl Constructor ---");
            logger.info("--- DescriptorImpl Constructor ---" + host);
            logger.info("--- DescriptorImpl Constructor ---" + port);

            load();

            /*
             * Not sure when to register gearman functions yet so for now always
             * initialize the launch worker flag to disabled state at jenkins
             * startup so we are always at a known state
             */
            this.launchWorker = false;
            save();
        }

        @Override
        public String getDisplayName() {
            return "Gearman Plugin";
        }

        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json)
                throws FormException {
            launchWorker = json.getBoolean("launchWorker");
            logger.info("--- DescriptorImpl Configure function ---"
                    + this.launchWorker());

            // set the gearman server host from value in jenkins config page
            try {
                host = json.getString("host");
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error getting the gearman host name");
            }

            // user input vaerification
            if (StringUtils.isEmpty(host) || StringUtils.isBlank(host))
                throw new RuntimeException("Invalid gearman host name");

            // set the gearman server port from value in jenkins config page
            try {
                port = Integer.parseInt(json.getString("port"));
            } catch (Exception e) {
                throw new RuntimeException("Invalid gearman port value");
            }

            // i believe gearman already checks for port range, just want to do
            // basic verification here
            if (port <= 0)
                throw new RuntimeException("Invalid gearman port value");

            logger.info("--- DescriptorImpl Configure function ---"
                    + this.getHost());
            logger.info("--- DescriptorImpl Configure function ---"
                    + this.getPort());

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
}
