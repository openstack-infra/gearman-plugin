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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * This a fake worker which is only used for testing
 *
 * @author Khai Do
 *
 */
public class FakeWorkerThread extends AbstractWorkerThread{

    private static final Logger logger = LoggerFactory
            .getLogger(Constants.PLUGIN_LOGGER_NAME);

    // constructor
    public FakeWorkerThread(String host, int port) {
        super(host, port);
    }

    public FakeWorkerThread(String host, int port, String name) {
        super(host, port, name);
    }

    /**
     * Fake registerJobs
     */
    @Override
    public void registerJobs() {

    }

    /**
     * Fake run
     */
    @Override
    public void run() {

    }

}
