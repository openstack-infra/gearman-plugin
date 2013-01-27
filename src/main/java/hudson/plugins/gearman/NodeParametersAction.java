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

import java.util.List;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

/**
 * Action to send parameters to a jenkins build.
 *
 * @author Khai Do
 */
public class NodeParametersAction extends ParametersAction {


    String id;              // the id used to track the build job

    public NodeParametersAction(List<ParameterValue> parameters) {
        this(parameters, "");

    }

    public NodeParametersAction(List<ParameterValue> parameters, String id) {
        super(parameters);
        this.id = id;

    }

    public String getUuid() {
        return id;
    }

    public void setUuid(String uuid) {
        this.id = uuid;
    }


}
