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

import hudson.model.Label;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.SubTask;

public class LabelAssignmentActionImpl implements LabelAssignmentAction{

    LabelAtom labelAtom;

    public LabelAssignmentActionImpl(String label){
        this.labelAtom = new LabelAtom(label);
    }

    @Override
    public String getIconFileName(){
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDisplayName(){
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUrlName(){
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Label getAssignedLabel(SubTask task){
        // TODO Auto-generated method stub
        return labelAtom;
    }

}
