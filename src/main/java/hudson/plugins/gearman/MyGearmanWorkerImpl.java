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
/*
 * This is adapted from gearman-java with the following license
 *
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package hudson.plugins.gearman;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.gearman.common.Constants;
import org.gearman.common.GearmanException;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanJobServerIpConnectionFactory;
import org.gearman.common.GearmanJobServerSession;
import org.gearman.common.GearmanNIOJobServerConnectionFactory;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacket.DataComponentName;
import org.gearman.common.GearmanPacketImpl;
import org.gearman.common.GearmanPacketMagic;
import org.gearman.common.GearmanPacketType;
import org.gearman.common.GearmanServerResponseHandler;
import org.gearman.common.GearmanSessionEvent;
import org.gearman.common.GearmanSessionEventHandler;
import org.gearman.common.GearmanTask;

import org.gearman.worker.DefaultGearmanFunctionFactory;
import org.gearman.worker.GearmanFunction;
import org.gearman.worker.GearmanFunctionFactory;
import org.gearman.worker.GearmanWorker;

import org.gearman.util.ByteUtils;
import org.slf4j.LoggerFactory;

public class MyGearmanWorkerImpl implements GearmanSessionEventHandler {

    static public enum State {

        IDLE, RUNNING, SHUTTINGDOWN
    }
    private static final String DESCRIPION_PREFIX = "GearmanWorker";
    private Queue<GearmanFunction> functionList = null;
    private Selector ioAvailable = null;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_WORKER_LOGGER_NAME);
    private String id;
    private Map<String, FunctionDefinition> functionMap;
    private State state;
    private ExecutorService executorService;
    private Map<GearmanJobServerSession, GearmanTask> taskMap = null;
    private GearmanJobServerSession session = null;
    private final GearmanJobServerIpConnectionFactory connFactory = new GearmanNIOJobServerConnectionFactory();
    private volatile boolean jobUniqueIdRequired = false;
    private FunctionRegistry functionRegistry;
    private WaitBool okayToGrabJob = new WaitBool(true);

    class WaitBool {
        private boolean value;

        WaitBool(boolean value) {
            this.value = value;
        }

        public synchronized void set(boolean value) {
            this.value = value;
            this.notifyAll();
        }

        public synchronized void waitUntil(boolean value)
            throws InterruptedException
        {
            if (this.value == value)
                return;

            while (this.value != value) {
                this.wait();
            }
        }
    }

    class GrabJobEventHandler implements GearmanServerResponseHandler {

        private final GearmanJobServerSession session;
        private boolean isDone = false;

        GrabJobEventHandler(GearmanJobServerSession session) {
            super();
            this.session = session;
        }

        public void handleEvent(GearmanPacket event) throws GearmanException {
            handleSessionEvent(new GearmanSessionEvent(event, session));
            isDone = true;
        }

        public boolean isDone() {
            return isDone;
        }
    }

    static class FunctionDefinition {

        private final long timeout;
        private final GearmanFunctionFactory factory;

        FunctionDefinition(long timeout, GearmanFunctionFactory factory) {
            this.timeout = timeout;
            this.factory = factory;
        }

        long getTimeout() {
            return timeout;
        }

        GearmanFunctionFactory getFactory() {
            return factory;
        }
    }

    class FunctionRegistry {
        private Set<GearmanFunctionFactory> functions;
        private boolean updated = false;

        FunctionRegistry() {
            functions = new HashSet<GearmanFunctionFactory>();
        }

        public synchronized Set<GearmanFunctionFactory> getFunctions(){
            if (updated) {
                updated = false;
                return functions;
            } else {
                return null;
            }
        }

        public synchronized void setFunctions(Set<GearmanFunctionFactory> functions){
            this.functions = functions;
            this.updated = true;
        }

        public synchronized void setUpdated(boolean updated) {
            this.updated = updated;
        }

    }

    public void reconnect() {
        LOG.info("Starting reconnect for " + session.toString());
        try {
            session.initSession(ioAvailable, this);
            if (id != null) {
                sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                                                GearmanPacketType.SET_CLIENT_ID,
                                                ByteUtils.toUTF8Bytes(id)));
            }
            // this will cause a grab-job event
            functionRegistry.setUpdated(true);
        } catch (IOException e) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
            }
        }
        LOG.info("Ending reconnect for " + session.toString());
    }

    public MyGearmanWorkerImpl() {
        this (null);
    }

    public MyGearmanWorkerImpl(ExecutorService executorService) {
        functionList = new LinkedList<GearmanFunction>();
        id = DESCRIPION_PREFIX + ":" + Thread.currentThread().getId();
        functionMap = new HashMap<String, FunctionDefinition>();
        state = State.IDLE;
        this.executorService = executorService;
        taskMap = new HashMap<GearmanJobServerSession, GearmanTask>();
        functionRegistry = new FunctionRegistry();

        try {
            ioAvailable = Selector.open();
        } catch (IOException ioe) {
            LOG.warn("Failed to open IO selector.", ioe);
        }
    }

    @Override
    public String toString() {
        return id;
    }

    public void setFunctions(Set<GearmanFunctionFactory> functions) {
        functionRegistry.setFunctions(functions);
        ioAvailable.wakeup();
    }

    /**
     * This is a small lie -- it only returns the functions it has been
     * instructed to register, not the ones it has actually gotton around
     * to registering.  This is mostly here for tests.
    **/
    public Set getRegisteredFunctions() {
        Set<String> ret = new HashSet<String>();

        Set<GearmanFunctionFactory> functions = functionRegistry.getFunctions();
        if (functions == null) {
            return ret;
        }

        for (GearmanFunctionFactory factory: functions) {
            ret.add(factory.getFunctionName());
        }
        return ret;
    }

    private void registerFunctions() throws IOException{
        Set<GearmanFunctionFactory> functions = functionRegistry.getFunctions();

        if (functions == null) {
            return;
        }

        functionMap.clear();
        sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.RESET_ABILITIES, new byte[0]));
        session.driveSessionIO();

        for (GearmanFunctionFactory factory: functions) {
            FunctionDefinition def = new FunctionDefinition(0, factory);
            functionMap.put(factory.getFunctionName(), def);
            sendToAll(generateCanDoPacket(def));
            session.driveSessionIO();

            LOG.debug("Worker " + this + " has registered function " +
                      factory.getFunctionName());
        }

        // Simulate a NOOP packet which will kick off a GRAB_JOB cycle
        // if we're sleeping.  If we get a real NOOP in the mean time,
        // it should be fine because GearmanJobServerSession ignores a
        // NOOP if PRE_SLEEP is not on the stack.
        GearmanPacket p = new GearmanPacketImpl(GearmanPacketMagic.RES,
                                                GearmanPacketType.NOOP, new byte[0]);
        GearmanSessionEvent event = new GearmanSessionEvent(p, session);
        session.handleSessionEvent(event);
    }

    public void work() {
        LOG.info("Starting work");

        if (!state.equals(State.IDLE)) {
            throw new IllegalStateException("Can not call work while worker " +
                    "is running or shutting down");
        }

        state = State.RUNNING;
        boolean grabJobSent = false;

        while (isRunning()) {
            if (!session.isInitialized()) {
                reconnect();
                grabJobSent = false;
            }

            // if still disconnected, skip
            if (!session.isInitialized()) {
                continue;
            }

            try {
                registerFunctions();
            } catch (IOException io) {
                LOG.warn("Receieved IOException while" +
                         " registering function",io);
                session.closeSession();
                continue;
            }

            if (functionList.isEmpty()) {
                if (!grabJobSent) {
                    // send the initial GRAB_JOB on reconnection.
                    LOG.info("Worker " + this + " sending initial grab job");
                    try {
                        sendGrabJob(session);
                    } catch (InterruptedException e) {
                        LOG.warn("Interrupted while waiting for okay to send " +
                                 "grab job", e);
                        continue;
                    }
                    grabJobSent = true;
                    try {
                        session.driveSessionIO();
                    } catch (IOException io) {
                        LOG.warn("Receieved IOException while" +
                                 " sending initial grab job",io);
                        session.closeSession();
                        continue;
                    }
                }
            }

            if (functionList.isEmpty()) {
                int interestOps = SelectionKey.OP_READ;
                if (session.sessionHasDataToWrite()) {
                    interestOps |= SelectionKey.OP_WRITE;
                }
                session.getSelectionKey().interestOps(interestOps);

                try {
                    ioAvailable.select();
                } catch (IOException io) {
                    LOG.warn("Receieved IOException while" +
                             " selecting for IO",io);
                    session.closeSession();
                    continue;
                }
            }

            if (ioAvailable.selectedKeys().contains(session.getSelectionKey())) {
                if (!session.isInitialized()) {
                    continue;
                }
                try {
                    session.driveSessionIO();
                } catch (IOException ioe) {
                    LOG.warn("Received IOException while driving" +
                            " IO on session " + session, ioe);
                    session.closeSession();
                    continue;
                }
            }

            //For the time being we will execute the jobs synchronously
            //in the future, I expect to change this.
            if (!functionList.isEmpty()) {
                GearmanFunction fun = functionList.remove();
                submitFunction(fun);
                // Send another grab_job on the next loop
                grabJobSent = false;
            }
        }

        shutDownWorker(true);
    }

    public void setOkayToGrabJob(boolean value) {
        okayToGrabJob.set(value);
    }

    private void sendGrabJob(GearmanJobServerSession s) throws InterruptedException {
        okayToGrabJob.waitUntil(true);

        GearmanTask grabJobTask = new GearmanTask(
            new GrabJobEventHandler(s),
            new GearmanPacketImpl(GearmanPacketMagic.REQ,
                getGrabJobPacketType(), new byte[0]));
        taskMap.put(s, grabJobTask);
        s.submitTask(grabJobTask);
    }

    public void handleSessionEvent(GearmanSessionEvent event)
            throws IllegalArgumentException, IllegalStateException {
        GearmanPacket p = event.getPacket();
        GearmanJobServerSession s = event.getSession();
        GearmanPacketType t = p.getPacketType();
        LOG.debug("Worker " + this + " handling session event" +
                  " ( Session = " + s + " Event = " + t + " )");
        switch (t) {
            case JOB_ASSIGN:
                //TODO Figure out what the right behavior is if JobUUIDRequired was false when we submitted but is now true
                taskMap.remove(s);
                addNewJob(event);
                break;
            case JOB_ASSIGN_UNIQ:
                //TODO Figure out what the right behavior is if JobUUIDRequired was true when we submitted but is now false
                taskMap.remove(s);
                addNewJob(event);
                break;
            case NOOP:
                taskMap.remove(s);
                LOG.debug("Worker " + this + " sending grab job after wakeup");
                try {
                    sendGrabJob(s);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for okay to send " +
                             "grab job", e);
                }
                break;
            case NO_JOB:
                LOG.debug("Worker " + this + " sending pre sleep after no_job");
                GearmanTask preSleepTask = new GearmanTask(new GrabJobEventHandler(s),
                        new GearmanPacketImpl(GearmanPacketMagic.REQ,
                        GearmanPacketType.PRE_SLEEP, new byte[0]));
                taskMap.put(s, preSleepTask);
                s.submitTask(preSleepTask);
                break;
            case ECHO_RES:
                break;
            case OPTION_RES:
                break;
            case ERROR:
                s.closeSession();
                break;
            default:
                LOG.warn("Received unknown packet type " + t +
                        " from session " + s + ". Closing connection.");
                s.closeSession();
        }
    }

    public boolean addServer(String host, int port) {
        return addServer(connFactory.createConnection(host, port));
    }

    public boolean addServer(GearmanJobServerConnection conn)
            throws IllegalArgumentException, IllegalStateException {

        if (conn == null) {
            throw new IllegalArgumentException("Connection can not be null");
        }

        if (session != null) {
            return true;
        }

        session = new GearmanJobServerSession(conn);

        reconnect();

        LOG.debug("Added server " + conn + " to worker " + this);
        return true;
    }

    public void setWorkerID(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("Worker ID may not be null");
        }
        this.id = id;
        if (session.isInitialized()) {
            sendToAll(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                                            GearmanPacketType.SET_CLIENT_ID,
                                            ByteUtils.toUTF8Bytes(id)));
        }
    }

    public String getWorkerID() {
        return id;
    }

    public void stop() {
        state = State.SHUTTINGDOWN;
    }

    public List<Exception> shutdown() {
        return shutDownWorker(false);
    }

    public boolean isRunning() {
        return state.equals(State.RUNNING);
    }

    public void setJobUniqueIdRequired(boolean requiresJobUUID) {
        jobUniqueIdRequired = requiresJobUUID;
    }

    public boolean isJobUniqueIdRequired() {
        return jobUniqueIdRequired;
    }

    private GearmanPacket generateCanDoPacket(FunctionDefinition def) {
        GearmanPacketType pt = GearmanPacketType.CAN_DO;
        byte[] data = ByteUtils.toUTF8Bytes(def.getFactory().getFunctionName());
        return new GearmanPacketImpl(GearmanPacketMagic.REQ, pt, data);
    }

    private void sendToAll(GearmanPacket p) {
        sendToAll(null, p);
    }

    private void sendToAll(GearmanServerResponseHandler handler, GearmanPacket p) {
        GearmanTask gsr = new GearmanTask(handler, p);
        session.submitTask(gsr);
    }

    /*
     * For the time being this will always return an empty list of
     * exceptions because closeSession does not throw an exception
     */
    private List<Exception> shutDownWorker(boolean completeTasks) {
        LOG.info("Commencing shutdowm of worker " + this);

        ArrayList<Exception> exceptions = new ArrayList<Exception>();

        // This gives any jobs in flight a chance to complete
        if (executorService != null) {
            if (completeTasks) {
                executorService.shutdown();
            } else {
                executorService.shutdownNow();
            }
        }

        session.closeSession();
        try {
            ioAvailable.close();
        } catch (IOException ioe) {
            LOG.warn("Encountered IOException while closing selector for worker: ", ioe);
        }
        state = State.IDLE;
        LOG.info("Completed shutdowm of worker " + this);

        return exceptions;
    }

    private void addNewJob(GearmanSessionEvent event) {
        byte[] handle, data, functionNameBytes, unique;
        GearmanPacket p = event.getPacket();
        String functionName;
        handle = p.getDataComponentValue(
                GearmanPacket.DataComponentName.JOB_HANDLE);
        functionNameBytes = p.getDataComponentValue(
                GearmanPacket.DataComponentName.FUNCTION_NAME);
        data = p.getDataComponentValue(
                GearmanPacket.DataComponentName.DATA);
        unique = p.getDataComponentValue(DataComponentName.UNIQUE_ID);
        functionName = ByteUtils.fromUTF8Bytes(functionNameBytes);
        FunctionDefinition def = functionMap.get(functionName);
        if (def == null) {
            GearmanTask gsr = new GearmanTask(
                    new GearmanPacketImpl(GearmanPacketMagic.REQ,
                    GearmanPacketType.WORK_FAIL, handle));
            session.submitTask(gsr);
        } else {
            GearmanFunction function = def.getFactory().getFunction();
            function.setData(data);
            function.setJobHandle(handle);
            function.registerEventListener(session);
            if (unique != null && unique.length > 0) {
                function.setUniqueId(unique);
            }
            functionList.add(function);
        }
    }

    private void submitFunction(GearmanFunction fun) {
        try {
            if (executorService == null) {
                fun.call();
            } else {
                executorService.submit(fun);
            }
        } catch (Exception e) {
            LOG.warn("Exception while executing function " + fun.getName(), e);
        }
    }

    private GearmanPacketType getGrabJobPacketType() {
        if (jobUniqueIdRequired) {
            return GearmanPacketType.GRAB_JOB_UNIQ;
        }
        return GearmanPacketType.GRAB_JOB;
    }

}
