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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractWorkerThread implements Runnable {

    public static final String DEFAULT_NAME = "anonymous";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 4730;
    private static final Logger logger = LoggerFactory
            .getLogger(AbstractWorkerThread.class);

    protected String host;
    protected int port;
    protected String name;
    private Thread thread;

    public AbstractWorkerThread() {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_NAME);
    }

    public AbstractWorkerThread(String host, int port) {
        this(host, port, DEFAULT_NAME);
    }

    public AbstractWorkerThread(String host, int port, String name) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public void registerJobs() {

        logger.info("----- AbstractorWorker registerJobs function ----");

    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        // Interrupt the thread so it unblocks any blocking call

        logger.info("Stopping " + name + " (" + new Date().toString() + ")");

        thread.interrupt();

        // Wait until the thread exits
        try {
            thread.join();
        } catch (InterruptedException ex) {
            // Unexpected interruption
            ex.printStackTrace();
            System.exit(1);
        }

        logger.info("Stopped " + name + " (" + new Date().toString() + ")");

    }

    @Override
    public void run() {

        logger.info("Starting Worker "+ name +" ("+new Date().toString()+")");

        while (!Thread.interrupted()) {

            // Running the Gearman Worker
            logger.info("Running Worker "+ name +" ("+new Date().toString()+")");

            try {
                Thread.sleep(1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }

        logger.info("Thread Stopped" + " (" + new Date().toString() + ")");

        // Thread exits
    }

}
