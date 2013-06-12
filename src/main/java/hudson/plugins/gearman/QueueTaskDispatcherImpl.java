/*
 *
 * Copyright 2013 Hewlett-Packard Development Company, L.P.
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

import hudson.Extension;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Extension
public class QueueTaskDispatcherImpl extends QueueTaskDispatcher {

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);


    @Override
    public CauseOfBlockage canTake(Node node,
                                   Queue.BuildableItem item) {
        // update only when gearman-plugin is enabled
        if (!GearmanPluginConfig.get().enablePlugin()) {
            return null;
        }

        return GearmanProxy.getInstance().canTake(node, item);
    }
}
