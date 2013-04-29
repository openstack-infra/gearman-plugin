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

/*
 * This is adapted from gearman-java with the following license
 *
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the bsd.txt file in the parent directory for full text.
 */

package hudson.plugins.gearman;

import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Project;

import java.lang.reflect.Constructor;

import org.gearman.common.Constants;
import org.gearman.worker.DefaultGearmanFunctionFactory;
import org.gearman.worker.GearmanFunction;
import org.slf4j.LoggerFactory;

public class CustomGearmanFunctionFactory extends DefaultGearmanFunctionFactory {

    private final AbstractProject<?,?> project;
    private final Node node;
    private final String theClass;
    private final String masterName;

    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_WORKER_LOGGER_NAME);

    public CustomGearmanFunctionFactory(String functionName, String className,
                                        AbstractProject<?,?> project, Node node,
                                        String masterName) {
        super(functionName, className);
        this.theClass = className;
        this.project = project;
        this.node = node;
        this.masterName = masterName;
    }


    @Override
    public GearmanFunction getFunction() {
        return createFunctionInstance(theClass, project, node, masterName);
    }




    private static GearmanFunction createFunctionInstance(String className, AbstractProject<?,?> project, Node node, String masterName) {

        GearmanFunction f = null;
        try {

            Class<?> c = Class.forName(className);
            Constructor<?> con = c.getConstructor(new Class[]{Project.class, Node.class, String.class});
            Object o = con.newInstance(new Object[] {project, node, masterName});

            if (o instanceof GearmanFunction) {
                f = (GearmanFunction) o;
            } else {
                LOG.warn("Specified class " + className +
                        " is not a Gearman Function ");
            }
        } catch (Exception e) {
            LOG.warn("Unable to create instance of " +
                    "Function: " + className, e);
        }
        return f;
    }

}
