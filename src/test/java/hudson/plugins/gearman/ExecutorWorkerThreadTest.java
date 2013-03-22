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
import hudson.maven.MavenModuleSet;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;

import java.util.Set;

import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.worker.GearmanWorker;
import org.gearman.worker.GearmanWorkerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * Test for the {@link ExecutorWorkerThread} class.
 *
 * @author Khai Do
 */
@PrepareForTest(GearmanWorkerImpl.class)
public class ExecutorWorkerThreadTest extends HudsonTestCase {

    DumbSlave slave = null;

    @Before
    public void setUpTest() throws Exception {
        // mock out GearmanWorker
        GearmanWorker gearmanWorker = mock(GearmanWorker.class);
        GearmanNIOJobServerConnection conn = new GearmanNIOJobServerConnection("localhost", 4730);
        doNothing().when(gearmanWorker).work();
        doNothing().when(gearmanWorker).unregisterAll();
        when(gearmanWorker.addServer(conn)).thenReturn(true);
    }

    @After
    public void tearDownTest() {
    }

    /*
     * This test verifies that gearman functions are correctly registered for a
     * project that contains a label matching a slave node's self label
     */
    @Test
    public void testRegisterJobs_NodeSelfLabel() throws Exception {

        slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> apple = createFreeStyleProject("apple");
        apple.setAssignedLabel(new LabelAtom("oneiric-10"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(2, functions.size());
        assertTrue(functions.contains("build:apple"));
        assertTrue(functions.contains("build:apple:oneiric-10"));

    }

    /*
     * This test verifies that gearman functions are correctly registered for a
     * project that contains a single label matching a label on the slave node
     */
    @Test
    public void testRegisterJobs_ProjectSingleLabel() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(2, functions.size());
        assertTrue(functions.contains("build:lemon"));
        assertTrue(functions.contains("build:lemon:linux"));

    }

    /*
     * This test verifies that no gearman functions are registered
     * for projects that contain labels that do not match labels on a slave node
     */
    @Test
    public void testRegisterJobs_ProjectInvalidLabel() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("bogus"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that gearman functions correctly registered for a
     * project that contains no labels
     */
    @Test
    public void testRegisterJobs_ProjectNoLabel() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(1, functions.size());
        assertTrue(functions.contains("build:lemon"));

    }

    /*
     * This test verifies that no gearman functions are registered
     * for disabled projects.
     */
    @Test
    public void testRegisterJobs_ProjectDisabled() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));
        lemon.disable();

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that no gearman functions are registered
     * for slaves nodes that are offline.
     */
    @Test
    public void testRegisterJobs_SlaveOffline() throws Exception {

        DumbSlave slave = createSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that gearman functions is correctly registered
     * for maven projects
     */
    @Test
    public void testRegisterJobs_MavenProject() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        MavenModuleSet lemon = createMavenProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(2, functions.size());
        assertTrue(functions.contains("build:lemon"));
        assertTrue(functions.contains("build:lemon:linux"));

    }

    /*
     * This test verifies that gearman functions are correctly registered for a
     * project that contains a label that has a negate operator
     */
    @Test
    public void testRegisterJobs_ProjectNotLabel() throws Exception {

        DumbSlave slave = createOnlineSlave(new LabelAtom("oneiric-10"));
        slave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("!linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave);
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        hudson.removeNode(slave);

        assertEquals(0, functions.size());
    }
}
