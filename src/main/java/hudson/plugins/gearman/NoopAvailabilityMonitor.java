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

public class NoopAvailabilityMonitor implements AvailabilityMonitor {

    public void lock(MyGearmanWorkerImpl worker) {
    }

    public void unlock(MyGearmanWorkerImpl worker) {
    }

    public void wake() {
    }

    public void expectUUID(String UUID) {
    }

    public boolean canTake(Queue.BuildableItem item)
    {
        return (true);
    }
}
