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

import hudson.slaves.DumbSlave;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test for the {@link ExecutorWorkerThread} class.
 *
 * @author Khai Do
 */
public class GearmanProxyTest extends HudsonTestCase {

    GearmanProxy gp;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        gp = GearmanProxy.getInstance();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        gp.testResetHandles();
        super.tearDown();
    }

    @Test
    public void testCreateManagementWorker() {

        assertEquals(0, gp.getNumExecutors());

        gp.createManagementWorker();

        // mgmt: 1 master
        assertEquals(1, gp.getNumExecutors());
        //        assertTrue(gp.getGmwtHandles().get(0).isAlive());
    }

    @Test
    public void testCreateExecutorWorkersOnNode() throws Exception {

        DumbSlave slave = createSlave();

        assertEquals(0, gp.getNumExecutors());

        gp.createExecutorWorkersOnNode(slave.toComputer());

        // exec: 1 master
        assertEquals(1, gp.getNumExecutors());
    }

    @Test
    public void testInitWorkers() {

        gp.initWorkers();

        // exec: 1 slave, 1 master + mgmnt: 1
        assertEquals(3, gp.getNumExecutors());
    }

    @Test
    public void testInitWorkers2() throws Exception {

        DumbSlave slave = createSlave();
        gp.initWorkers();

        // exec: 2 slaves, 1 master + mgmnt: 1
        assertEquals(4, gp.getNumExecutors());
    }
}
