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
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.slaves.ComputerListener;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update gearman workers when node changes
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    @Override
    public void onConfigurationChange() {
        // Bug: Configuration save occurs after this function is called
        // gets called on any configuration change
        // includes new slave and delete slave
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onConfigurationChange");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return;
        }

        // TODO: adjust for an update to executors. Method does not provide the
        // computer to know which thread to remove or add
    }

    @Override
    public void onOffline(Computer c) {
        // gets called when existing slave dis-connects
        // gets called when slave is deleted.
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onOffline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return;
        }

        // remove worker when jenkins slave is deleted or disconnected
        GearmanProxy gp = GearmanProxy.getInstance();
        List<AbstractWorkerThread> workers = gp.getGewtHandles();
        synchronized(workers) {
            for (AbstractWorkerThread worker : workers) {
                if (worker.name.contains(c.getName())) {
                    logger.info("---- stopping executor worker = "
                              + worker.getName());
                    gp.getGewtHandles().remove(worker);
                    worker.stop();
                }
            }
        }
    }

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException,
            InterruptedException {
        // gets called when master goes into online state
        // gets called when existing slave re-connects
        // gets called when new slave goes into online state
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onOnline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return;
        }

        // on creation of master
        GearmanProxy gp = GearmanProxy.getInstance();
        if (Computer.currentComputer() == c) { //check to see if this is master
            logger.info("---- This is master node, name is = "+c.getName());

            /*
             * Spawn management executor worker. This worker does not need any
             * executors. It only needs to work with gearman.
             */
            gp.createManagementWorker();

            /*
             * Spawn executors for the jenkins master Need to treat the master
             * differently than slaves because the master is not the same as a
             * slave
             */
            gp.createExecutorWorkersOnNode(c);
        }

        // on creation of new slave
        if (Computer.currentComputer() != c
                && !gp.getGewtHandles().contains(c)) {

            gp.createExecutorWorkersOnNode(c);
        }
    }
}
