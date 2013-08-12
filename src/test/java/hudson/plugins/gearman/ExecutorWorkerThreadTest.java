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

import hudson.maven.MavenModuleSet;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test for the {@link ExecutorWorkerThread} class.
 *
 * @author Khai Do
 */
public class ExecutorWorkerThreadTest extends HudsonTestCase {

    DumbSlave slave = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        slave = createOnlineSlave(new LabelAtom("oneiric-10"));

        // poll to make sure test slave is online before continuing
        long timeoutExpiredMs = System.currentTimeMillis() + 3000;
        while (true) {
            if (slave.getChannel() != null) {
                break;
            }
            this.wait(timeoutExpiredMs - System.currentTimeMillis());
            if (System.currentTimeMillis() >= timeoutExpiredMs) {
                fail("Could not start test slave");
            }
        }

        slave.setLabelString("ubuntu gcc python-2.4 linux");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        hudson.removeNode(slave);
        super.tearDown();
    }

    /*
     * This test verifies that gearman functions are correctly registered for a
     * project that contains a label matching a slave node's self label
     */
    @Test
    public void testRegisterJobs_NodeSelfLabel() throws Exception {


        Project<?, ?> apple = createFreeStyleProject("apple");
        apple.setAssignedLabel(new LabelAtom("oneiric-10"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

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

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

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

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("bogus"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that gearman functions correctly registered for a
     * project that contains no labels
     */
    @Test
    public void testRegisterJobs_ProjectNoLabel() throws Exception {

        Project<?, ?> lemon = createFreeStyleProject("lemon");

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        assertEquals(1, functions.size());
        assertTrue(functions.contains("build:lemon"));

    }

    /*
     * This test verifies that no gearman functions are registered
     * for disabled projects.
     */
    @Test
    public void testRegisterJobs_ProjectDisabled() throws Exception {

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));
        lemon.disable();

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that no gearman functions are registered
     * for slaves nodes that are offline.
     */
    @Test
    public void testRegisterJobs_SlaveOffline() throws Exception {

        DumbSlave offlineSlave = createSlave(new LabelAtom("oneiric-10"));
        offlineSlave.setLabelString("ubuntu gcc python-2.4 linux");

        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", offlineSlave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        assertEquals(0, functions.size());

    }

    /*
     * This test verifies that gearman functions is correctly registered
     * for maven projects
     */
    @Test
    public void testRegisterJobs_MavenProject() throws Exception {

        MavenModuleSet lemon = createMavenProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

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


        Project<?, ?> lemon = createFreeStyleProject("lemon");
        lemon.setAssignedLabel(new LabelAtom("!linux"));

        AbstractWorkerThread oneiric = new ExecutorWorkerThread("GearmanServer", 4730, "MyWorker", slave.toComputer(), "master", new NoopAvailabilityMonitor());
        oneiric.registerJobs();
        Set<String> functions = oneiric.worker.getRegisteredFunctions();

        assertEquals(0, functions.size());
    }
}
