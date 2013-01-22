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

import hudson.model.Label;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.SubTask;

/*
 * Overview: This class provides the ability to send
 * a build with parameters to a specific jenkins node
 *
 */

public class NodeParametersAction extends ParametersAction {

    LabelAtom labelAtom;    // node label to assign build to a node


    public NodeParametersAction(List<ParameterValue> parameters, String label) {
        super(parameters);
        this.labelAtom = new LabelAtom(label);

    }

    public NodeParametersAction(List<ParameterValue> parameters) {
        super(parameters);
    }

    public NodeParametersAction(ParameterValue... parameters) {
        super(parameters);
    }

    @Override
    public Label getAssignedLabel(SubTask task) {
        return labelAtom;
    }


}
