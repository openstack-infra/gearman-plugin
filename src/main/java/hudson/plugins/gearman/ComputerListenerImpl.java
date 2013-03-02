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

import jenkins.model.Jenkins;

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
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        // called when slave re-connects
        // called when new slaves are connecting for first time
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" preOnline");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // on creation of slave
      int currNumNodes = Jenkins.getInstance().getNodes().size();
      if (GearmanProxy.numExecutorNodes < currNumNodes) {
          Node node = c.getNode();
          int slaveExecutors = c.getExecutors().size();
          for (int i=0; i<slaveExecutors; i++) {
              AbstractWorkerThread gwt  = new ExecutorWorkerThread("15.185.117.66", 4730,
                      node.getNodeName()+"-exec"+Integer.toString(i), node);
              gwt.start();
              GearmanProxy.gewtHandles.add(gwt);
          }
          GearmanProxy.numExecutorNodes = currNumNodes;
          logger.info("---- numExecutorNodes = "+GearmanProxy.numExecutorNodes);
      }
    }

    @Override
    public void onConfigurationChange() {
        // gets called on any configuration change include new slave and delete slave
        // gets called when new slave is created.
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" onConfigurationChange");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        //TODO: adjust for an update to labels.
        // Problem: node-label reference are unchanged on this call.  I think
        // Jenkins internal state needs time to update before we can re-register
        // gearman functions

        //TODO: adjust for an update to executors.  Method does not provide the
        // computer to know which thread to remove or add
        int gearmanWorkers = GearmanProxy.gewtHandles.size();
        int currNumExecutors = Jenkins.getInstance().getNumExecutors();
        if (gearmanWorkers < currNumExecutors) { //executor added
            // spawn a thread for executor
        } else if (gearmanWorkers < currNumExecutors) { // executor removed
            // stop the thread for executor

        }

//        if (!GearmanProxy.gewtHandles.isEmpty()) {
//            for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
//                awt.registerJobs();
//            }
//        }
    }

    @Override
    public void onOffline(Computer c) {
        // gets called when existing slave dis-connects
        // gets called when slave is deleted.
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" onOffline");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // on deletion of slave
        int currNumNodes = Jenkins.getInstance().getNodes().size();
        if (GearmanProxy.numExecutorNodes > currNumNodes) {
            if (!GearmanProxy.gewtHandles.isEmpty()) {
                GearmanProxy.numExecutorNodes--;
                logger.info("---- numExecutorNodes = "+GearmanProxy.numExecutorNodes);
                for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
                    if (awt.name.contains(c.getName())) {
                        try {
                            awt.stop();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        GearmanProxy.gewtHandles.remove(awt);
                    }
                }
            }
        }

        // on disconnect of node
        if (!GearmanProxy.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        // gets called when existing slave re-connects
        // gets called when new slave is online.
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" onOnline");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // on re-connection of node
        if (!GearmanProxy.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
                awt.registerJobs();
            }
        }

    }

    @Override
    public void onTemporarilyOnline(Computer c) {
        // gets called when existing slave is re-enabled
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" onTemporarilyOnline");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        if (!GearmanProxy.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
                awt.registerJobs();
            }
        }

    }

    @Override
    public void onTemporarilyOffline(Computer c, OfflineCause cause) {
        // gets called when existing slave is dis-enabled
        logger.info("---- "+ComputerListenerImpl.class.getName()+":"+" onTemporarilyOffline");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        if (!GearmanProxy.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanProxy.gewtHandles) {
                awt.registerJobs();
            }
        }
    }


}
