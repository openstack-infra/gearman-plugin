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
import java.util.UUID;

import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.worker.GearmanWorker;
import org.gearman.worker.GearmanWorkerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Thread to run gearman worker
 */

public abstract class AbstractWorkerThread implements Runnable {

    public static final String DEFAULT_EXECUTOR_NAME = "anonymous";
    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_EXECTUOR_LOGGER_NAME);

    protected String host;
    protected int port;
    protected String name;
    protected GearmanWorker worker;
    private GearmanNIOJobServerConnection conn;
    private Thread thread;


    public AbstractWorkerThread(String host, int port, String name) {
        this.name = name;
        this.host = host;
        this.port = port;
        worker = new GearmanWorkerImpl();
        conn = new GearmanNIOJobServerConnection(host, port);

    }

    /*
     * Register jobs with the gearman worker. This method should be overriden.
     */
    public void registerJobs() {

        logger.info("----- AbstractorWorker registerJobs function ----");

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
            logger.info("Stopping " + name + " (" + new Date().toString() + ")");
            worker.stop();
            logger.info("Stopped " + name + " (" + new Date().toString() + ")");
        }

        thread.interrupt();

        // Wait until the thread exits
        try {

            thread.join();
        } catch (InterruptedException ex) {
            // Unexpected interruption
            ex.printStackTrace();
            System.exit(1);
        }

    }

    /*
     * Execute the thread (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        if (!worker.isRunning()) {
            logger.info("Starting Worker " + name + " ("
                    + new Date().toString() + ")");
            worker.setWorkerID(UUID.randomUUID().toString());
            worker.addServer(conn);
            // blocking call.. https://answers.launchpad.net/gearman-java/+question/219175
            worker.work();
        }

        while (!Thread.interrupted()) {

            // Running the Gearman Worker
            logger.info("Running Worker "+ name +" ("+new Date().toString()+")");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }

        logger.info("Thread Stopped" + " (" + new Date().toString() + ")");

        // Thread exits
    }

}
