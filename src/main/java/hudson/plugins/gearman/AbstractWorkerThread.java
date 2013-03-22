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

import java.util.Date;

import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.worker.GearmanWorker;
import org.gearman.worker.GearmanWorkerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base object for gearman worker threads
 *
 *
 * @author Khai Do
 */

public abstract class AbstractWorkerThread implements Runnable {

    public static final String DEFAULT_EXECUTOR_NAME = "anonymous";
    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    protected String host;
    protected int port;
    protected String name;
    protected GearmanWorker worker;
    private final GearmanNIOJobServerConnection conn;
    private Thread thread;

    public AbstractWorkerThread(String host, int port) {
        this(host, port, DEFAULT_EXECUTOR_NAME);
    }

    public AbstractWorkerThread(String host, int port, String name) {
        setHost(host);
        setPort(port);
        setName(name);
        worker = new GearmanWorkerImpl();
        conn = new GearmanNIOJobServerConnection(host, port);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*
     * Register jobs with the gearman worker.
     * This method should be overriden.
     */
    public void registerJobs() {

        logger.info("---- AbstractorWorker registerJobs function ----");

    }

    /*
     * Start the thread
     */
    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    /*
     * Stop the thread
     */
    public void stop() {
        // Interrupt the thread so it unblocks any blocking call


        if (worker.isRunning()) {
            try {
                logger.info("---- Stopping " + getName() +" (" + new Date().toString() + ")");
                worker.unregisterAll();
                worker.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        thread.interrupt();

        // Wait until the thread exits
        try {

            thread.join();
        } catch (InterruptedException ex) {
            // Unexpected interruption
            ex.printStackTrace();
        }


    }

    /*
     * Execute the thread (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        if (!worker.isRunning()) {
            logger.info("---- Starting Worker "+ getName() +" ("+new Date().toString()+")");
            worker.setWorkerID(name);
            worker.setJobUniqueIdRequired(true);
            worker.addServer(conn);
            worker.work();
        }

        // Thread exits
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

}
