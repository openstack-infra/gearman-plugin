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

import hudson.model.Queue;

public interface AvailabilityMonitor {

    // Reserve exclusive access for this worker.
    public void lock(MyGearmanWorkerImpl worker);

    // Release exclusive access for this worker.
    public void unlock(MyGearmanWorkerImpl worker);

    // Notify waiting workers that they should try again to get the
    // lock.
    public void wake();

    // A worker holding the lock has scheduled a build with this UUID.
    public void expectUUID(String UUID);

    // Called by Jenkins to decide if a build can run on this node.
    public boolean canTake(Queue.BuildableItem item);
}
