/*
 *
 * Copyright 2013 OpenStack Foundation
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

import jenkins.model.Jenkins;
import hudson.model.Queue;
import hudson.model.Computer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeAvailabilityMonitor implements AvailabilityMonitor {
    private final Queue queue;
    private final Jenkins jenkins;
    private final Computer computer;
    private MyGearmanWorkerImpl workerHoldingLock = null;
    private String expectedUUID = null;

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    NodeAvailabilityMonitor(Computer computer)
    {
        this.computer = computer;
        queue = Queue.getInstance();
        jenkins = Jenkins.getInstance();
    }

    public Computer getComputer() {
        return computer;
    }

    public void lock(MyGearmanWorkerImpl worker)
        throws InterruptedException
    {
        logger.debug("AvailabilityMonitor lock request: " + worker);
        while (true) {
            boolean busy = false;

            // Synchronize on the Jenkins queue so that Jenkins is
            // unable to schedule builds while we try to acquire the
            // lock.
            synchronized(queue) {
                if (workerHoldingLock == null) {
                    if (computer.countIdle() == 0) {
                        // If there are no idle executors, we can not
                        // schedule a build.
                        busy = true;
                    } else if (jenkins.isQuietingDown()) {
                        busy = true;
                    } else {
                        logger.debug("AvailabilityMonitor got lock: " + worker);
                        workerHoldingLock = worker;
                        return;
                    }
                } else {
                    busy = true;
                }
            }
            if (busy) {
                synchronized(this) {
                    // We get synchronous notification when a
                    // build finishes, but there are lots of other
                    // reasons circumstances could change (adding
                    // an executor, canceling shutdown, etc), so
                    // we slowly busy wait to cover all those
                    // reasons.
                    this.wait(5000);
                }
            }
        }
    }

    public void unlock(MyGearmanWorkerImpl worker) {
        logger.debug("AvailabilityMonitor unlock request: " + worker);
        synchronized(queue) {
            if (workerHoldingLock == worker) {
                workerHoldingLock = null;
                expectedUUID = null;
                logger.debug("AvailabilityMonitor unlocked: " + worker);
            } else {
                logger.debug("Worker does not own AvailabilityMonitor lock: " +
                             worker);
            }
        }
        wake();
    }

    public void wake() {
        // Called when we know circumstances may have changed in a way
        // that may allow someone to get the lock.
        logger.debug("AvailabilityMonitor wake request");
        synchronized(this) {
            logger.debug("AvailabilityMonitor woken");
            notifyAll();
        }
    }

    public void expectUUID(String UUID) {
        // The Gearman worker which holds the lock is about to
        // schedule this build, so when Jenkins asks to run it, say
        // "yes".
        if (expectedUUID != null) {
            logger.error("AvailabilityMonitor told to expect UUID " +
                         UUID + "while already expecting " + expectedUUID);
        }
        expectedUUID = UUID;
    }

    public boolean canTake(Queue.BuildableItem item)
    {
        // Jenkins calls this from within the scheduler maintenance
        // function (while owning the queue monitor).  If we are
        // locked, only allow the build we are expecting to run.
        logger.debug("AvailabilityMonitor canTake request for " +
                     workerHoldingLock);

        NodeParametersAction param = item.getAction(NodeParametersAction.class);
        if (param != null) {
            logger.debug("AvailabilityMonitor canTake request for UUID " +
                         param.getUuid() + " expecting " + expectedUUID);

            if (expectedUUID == param.getUuid()) {
                return true;
            }
        }
        return (workerHoldingLock == null);
    }
}