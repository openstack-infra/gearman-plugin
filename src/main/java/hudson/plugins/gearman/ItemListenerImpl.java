/*
 *
 * Copyright 2014 Hewlett-Packard Development Company, L.P.
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
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This handles events for generic items in Jenkins. We also extended
 * the SaveableListener to catch any events that this one misses.
 */
@Extension
public class ItemListenerImpl extends ItemListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    @Override
    public void onCopied(Item src, Item item) {
        // Called after a new job is created by copying from an existing job
        registerJobs();
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        // Called after a job is renamed
        registerJobs();
    }

    @Override
    public void onLoaded() {
        registerJobs();
    }

    @Override
    public void onCreated(Item item) {
        registerJobs();
    }

    @Override
    public void onUpdated(Item item) {
        registerJobs();
    }

    @Override
    public void onDeleted(Item item) {
        registerJobs();
    }

    @Override
    public void onLocationChanged(Item item, String oldFullName, String newFullName)  {
        registerJobs();
    }

    // register gearman functions
    private void registerJobs() {
        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return;
        }
        GearmanProxy.getInstance().registerJobs();
    }
}
