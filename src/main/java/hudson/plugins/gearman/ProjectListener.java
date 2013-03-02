package hudson.plugins.gearman;


import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update gearman workers when project changes
 */
@Extension
public class ProjectListener extends ItemListener
{

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    @Override
    public void onUpdated(Item item)
    {
        logger.info("---- "+ProjectListener.class.getName()+":"+" onUpdated");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // update gearman worker functions on existing threads
        if (!GearmanPluginConfig.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanPluginConfig.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName)
    {
        logger.info("---- "+ProjectListener.class.getName()+":"+" onRenamed");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // update gearman worker functions on existing threads
        if (!GearmanPluginConfig.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanPluginConfig.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

    @Override
    public void onDeleted(Item item)
    {
        logger.info("---- "+ProjectListener.class.getName()+":"+" onDeleted");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // update gearman worker functions on existing threads
        if (!GearmanPluginConfig.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanPluginConfig.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

    @Override
    public void onCreated(Item item)
    {
        logger.info("---- "+ProjectListener.class.getName()+":"+" onCreated");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // update gearman worker functions on existing threads
        if (!GearmanPluginConfig.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanPluginConfig.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

    @Override
    public void onCopied(Item src, Item item)
    {
        logger.info("---- "+ProjectListener.class.getName()+":"+" onCopied");

        if (!GearmanPluginConfig.launchWorker) {
            return;
        }

        // update gearman worker functions on existing threads
        if (!GearmanPluginConfig.gewtHandles.isEmpty()) {
            for (AbstractWorkerThread awt: GearmanPluginConfig.gewtHandles) {
                awt.registerJobs();
            }
        }
    }

}