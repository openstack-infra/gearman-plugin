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

import java.util.HashSet;
import java.util.Set;

import org.gearman.worker.DefaultGearmanFunctionFactory;
import org.gearman.worker.GearmanFunctionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a thread to run gearman management worker
 *
 * @author Khai Do
 */

public class ManagementWorkerThread extends AbstractWorkerThread{

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    private boolean registered = false;
    private final String masterName;

    public ManagementWorkerThread(String host, int port, String name, String masterName, AvailabilityMonitor availability){
        super(host, port, name, availability);
        this.masterName = masterName;
    }

    /**
     * Register gearman functions on this executor.  This will unregister all
     * functions before registering new functions.
     *
     * This executor only registers one function "stop:$hostname".
     *
     */
    @Override
    public void registerJobs(){
        if (!registered) {
            Set<GearmanFunctionFactory> functionSet = new HashSet<GearmanFunctionFactory>();

            functionSet.add(new DefaultGearmanFunctionFactory("stop:"+masterName,
                            StopJobWorker.class.getName()));
            functionSet.add(new DefaultGearmanFunctionFactory("set_description:"+masterName,
                    SetDescriptionWorker.class.getName()));

            updateJobs(functionSet);
            registered = true;
        }
    }
}
