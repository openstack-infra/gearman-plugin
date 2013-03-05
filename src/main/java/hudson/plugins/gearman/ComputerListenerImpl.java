package hudson.plugins.gearman;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;

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
    public void preOnline(Computer c, Channel channel, FilePath root,
            TaskListener listener) throws IOException, InterruptedException {
        // called when slave re-connects
        // called when new slaves are connecting for first time
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " preOnline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // on creation of a slave
        int prevNumNodes = GearmanProxy.getNumWorkerNodes();
        int currNumNodes = GearmanPluginUtil.getNumTotalNodes();
        logger.info("----  prevNumNodes = " + prevNumNodes);
        logger.info("----  currNumNodes = " + currNumNodes);
        if (prevNumNodes < currNumNodes) {
            Node node = c.getNode();
            int slaveExecutors = c.getExecutors().size();
            for (int i = 0; i < slaveExecutors; i++) {
                AbstractWorkerThread gwt = new ExecutorWorkerThread(
                        GearmanPluginConfig.get().getHost(),
                        GearmanPluginConfig.get().getPort(), node.getNodeName()
                                + "-exec" + Integer.toString(i), node);
                gwt.start();
                GearmanProxy.getGewtHandles().add(gwt);
            }
            GearmanProxy.setNumWorkerNodes(currNumNodes);
            logger.info("---- numWorkerNodes = "
                    + GearmanProxy.getNumWorkerNodes());
        }
    }

    @Override
    public void onConfigurationChange() {
        // gets called on any configuration change 
        // includes new slave and delete slave
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onConfigurationChange");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // TODO: adjust for an update to labels.
        // Problem: node-label reference are unchanged on this call. I think
        // Jenkins internal state needs time to update before we can re-register
        // gearman functions

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
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // on deletion of slave
        int prevNumNodes = GearmanProxy.getNumWorkerNodes();
        int currNumNodes = GearmanPluginUtil.getNumTotalNodes();
        logger.info("----  prevNumNodes = " + prevNumNodes);
        logger.info("----  currNumNodes = " + currNumNodes);
        if (prevNumNodes > currNumNodes) {
            List<AbstractWorkerThread> workers = GearmanProxy.getGewtHandles();
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    if (worker.name.contains(c.getName())) {
                        logger.info("---- stopping executor worker = "
                                + worker.getName());
                        GearmanProxy.getGewtHandles().remove(worker);
                        worker.stop();
                    }
                }
                GearmanProxy.setNumWorkerNodes(currNumNodes);
                logger.info("---- numWorkerNodes = "
                        + GearmanProxy.getNumWorkerNodes());
            }
        } else {
            // on disconnect of node
            // update gearman worker functions on existing threads
            List<AbstractWorkerThread> workers = GearmanProxy.getGewtHandles();
            if (!workers.isEmpty()) {
                for (AbstractWorkerThread worker : workers) {
                    worker.registerJobs();
                }
            }
        }
    }

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException,
            InterruptedException {
        // gets called when existing slave re-connects
        // gets called when new slave goes into online state.
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onOnline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // on re-connection of node
        // update gearman worker functions on existing threads
        List<AbstractWorkerThread> workers = GearmanProxy.getGewtHandles();
        if (!workers.isEmpty()) {
            for (AbstractWorkerThread worker : workers) {
                worker.registerJobs();
            }
        }
    }

    @Override
    public void onTemporarilyOnline(Computer c) {
        // gets called when existing slave is re-enabled
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onTemporarilyOnline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<AbstractWorkerThread> workers = GearmanProxy.getGewtHandles();
        if (!workers.isEmpty()) {
            for (AbstractWorkerThread worker : workers) {
                worker.registerJobs();
            }
        }
    }

    @Override
    public void onTemporarilyOffline(Computer c, OfflineCause cause) {
        // gets called when existing slave is dis-enabled
        logger.info("---- " + ComputerListenerImpl.class.getName() + ":"
                + " onTemporarilyOffline");

        // update functions only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().launchWorker()) {
            return;
        }

        // update gearman worker functions on existing threads
        List<AbstractWorkerThread> workers = GearmanProxy.getGewtHandles();
        if (!workers.isEmpty()) {
            for (AbstractWorkerThread worker : workers) {
                worker.registerJobs();
            }
        }
    }

}
