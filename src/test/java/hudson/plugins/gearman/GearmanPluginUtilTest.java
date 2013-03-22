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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Computer;
import hudson.slaves.DumbSlave;

import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.worker.GearmanWorker;
import org.gearman.worker.GearmanWorkerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * Test for the {@link GearmanPluginUtil} class.
 *
 * @author Khai Do
 */
@PrepareForTest(GearmanWorkerImpl.class)
public class GearmanPluginUtilTest extends HudsonTestCase {

    /**
   */
    @Before
    public void setUpTest() {
        GearmanWorker gearmanWorker = mock(GearmanWorker.class);
        GearmanNIOJobServerConnection conn = new GearmanNIOJobServerConnection("localhost", 4730);
        doNothing().when(gearmanWorker).work();
        when(gearmanWorker.addServer(conn)).thenReturn(true);
    }

    @After
    public void tearDownTest() throws Exception {
    }

    @Test
    public void testGetRealNameSlave() throws Exception {

        DumbSlave slave = createSlave();
        slave.setNodeName("oneiric-10");

        hudson.removeNode(slave);

        assertEquals("oneiric-10", GearmanPluginUtil.getRealName(slave));
    }

    @Test
    public void testGetRealNameMaster() throws Exception {

        assertEquals("master", GearmanPluginUtil.getRealName(Computer.currentComputer().getNode()));
    }

}
