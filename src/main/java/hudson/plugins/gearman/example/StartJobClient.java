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

package hudson.plugins.gearman.example;

import java.util.UUID;

import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.client.GearmanJobResult;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;


/**
 * A java example of how to start a jenkins job using a Gearman client
 *
 * @author Khai Do
 */
public class StartJobClient {


    /**
     * @param args
     */
    public static void main(String[] args) {

        // setup connection settings
        String host = "127.0.0.1";
        int port = 4730;

        // setup job parameters
        String function = "build:pep8:precise";
        String uniqueId = UUID.randomUUID().toString();
        String params = "{param1:red, param2:white, param3:blue}";

        // connect client to server
        GearmanClient client = new GearmanClientImpl();
        GearmanNIOJobServerConnection conn = new GearmanNIOJobServerConnection(host, port);
        client.addJobServer(conn);

        // send job request
        byte[] data = ByteUtils.toUTF8Bytes(params);
        GearmanJob job = GearmanJobImpl.createJob(function, data, uniqueId);
        client.submit(job);

        // get results
        String value = "";
        GearmanJobResult res = null;
        try {
            res = job.get();
            value = ByteUtils.fromUTF8Bytes(res.getResults());
            System.out.println("Job Result: "+value);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // close client
        client.shutdown();

    }


}
