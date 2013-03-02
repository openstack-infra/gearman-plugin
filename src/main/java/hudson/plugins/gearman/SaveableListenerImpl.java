package hudson.plugins.gearman;

import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveableListenerImpl extends SaveableListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    @Override
    public void onChange(Saveable o, XmlFile file) {
        logger.info("---- "+SaveableListenerImpl.class.getName()+":"+" onChange");
//        AbstractProject ab = (AbstractProject)o;
//        logger.info("----"+ab.getName());

    }



}
