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

public class AvailabilityChecker {
    private boolean okayToGrabJob = true;
    private final boolean checkQuietingDown;

    AvailabilityChecker(boolean checkQuietingDown)
    {
        this.checkQuietingDown = checkQuietingDown;
    }

    public synchronized void setOkayToGrabJob(boolean value) {
        this.okayToGrabJob = value;
        this.notifyAll();
    }

    public void waitUntilOkayToGrabJob()
        throws InterruptedException
    {
        synchronized(this) {
            while (!okayToGrabJob) {
                this.wait();
            }
        }

        if (checkQuietingDown) {
            while (Jenkins.getInstance().isQuietingDown()) {
                Thread.sleep(5000);
            }
        }
    }
}