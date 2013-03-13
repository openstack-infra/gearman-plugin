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
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update gearman workers when project changes
 */
@Extension
public class ProjectListener extends ItemListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    @Override
    public void onUpdated(Item item) {
        logger.info("---- " + ProjectListener.class.getName() + ":"
                + " onUpdated");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<ExecutorWorkerThread> workers = GearmanProxy.getGewtHandles();
        synchronized(workers) {
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    worker.registerJobs();
                }
            }
        }
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        logger.info("---- " + ProjectListener.class.getName() + ":"
                + " onRenamed");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<ExecutorWorkerThread> workers = GearmanProxy.getGewtHandles();
        synchronized(workers) {
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    worker.registerJobs();
                }
            }
        }
    }

    @Override
    public void onDeleted(Item item) {
        logger.info("---- " + ProjectListener.class.getName() + ":"
                + " onDeleted");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<ExecutorWorkerThread> workers = GearmanProxy.getGewtHandles();
        synchronized(workers) {
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    worker.registerJobs();
                }
            }
        }
    }

    @Override
    public void onCreated(Item item) {
        logger.info("---- " + ProjectListener.class.getName() + ":"
                + " onCreated");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<ExecutorWorkerThread> workers = GearmanProxy.getGewtHandles();
        synchronized(workers) {
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    worker.registerJobs();
                }
            }
        }
    }

    @Override
    public void onCopied(Item src, Item item) {
        logger.info("---- " + ProjectListener.class.getName() + ":"
                + " onCopied");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
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