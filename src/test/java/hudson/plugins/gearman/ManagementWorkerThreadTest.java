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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.worker.GearmanWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * Test for the {@link ManagementWorkerThread} class.
 *
 * @author Khai Do
 */
@PrepareForTest(GearmanWorker.class)
public class ManagementWorkerThreadTest {

    /**
   */
    @Before
    public void setUp() {
        GearmanWorker gearmanWorker = mock(GearmanWorker.class);
        GearmanNIOJobServerConnection conn = new GearmanNIOJobServerConnection("localhost", 4730);
        doNothing().when(gearmanWorker).work();
        when(gearmanWorker.addServer(conn)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRegisterJobs() {
        AbstractWorkerThread manager = new ManagementWorkerThread("GearmanServer", 4730, "MyWorker" );
        manager.registerJobs();
        Set<String> functions = manager.worker.getRegisteredFunctions();
        assertEquals("stop:GearmanServer", functions.toArray()[0]);
    }
}
