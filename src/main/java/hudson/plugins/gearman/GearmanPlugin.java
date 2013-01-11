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
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

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

    private final String name;

    @DataBoundConstructor
    public GearmanPlugin(String name) {
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
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "GearmanPlugin";
        }

        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            
            save();
            return true;
        }

    }
}
