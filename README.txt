---------------------------------------------------------------------
Copyright 2013 Hewlett-Packard Development Company, L.P.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
---------------------------------------------------------------------

Jenkins does not support multiple masters.  You can setup multiple Jenkins
masters but there is no coordination between them.

One problem with scheduling builds on Jenkins master (“MasterA”) server
is that MasterA only knows about its connected slaves.  If all slaves on
MasterA are busy then MasterA will just put the next scheduled build on
its queue.  Now MasterA needs to wait for an available slave to run
the build.  This will be very in-efficient if your builds take a long
time to run.  So..what if there is another Jenkins master (“MasterB”)
that has free slaves to service the next scheduled build? Your probably
saying “Then slaves on MasterB should run the build”.  However MasterB
will never service the builds on MasterA's queue.  The client that schedules
the builds must know about MasterB and then schedule builds on MasterB.
This is what we mean by lack of coordination between masters.
The gearman-plugin attempts to fill this gap.

This plugin integrates Gearman with Jenkins and will make it so that
any Jenkins slave on any Jenkins master can service a job in the queue.
It will essentially replace the Jenkins build queue with the Gearman
job queue.  The job should stay in the gearman queue until there is a
Jenkins node that can run that job.


This is the typical workflow:

1) On a 'Launch Workers', we spawn a Gearman worker for each Jenkins
executor.  We'll call these "executor worker threads".

Each executor worker thread is associated 1:1 with a Jenkins node (slave or master)

2) Now we register jobs for each Gearman executor depending on
projects and nodes. View the image to see the mapping.

3) On a 'Launch Workers', we spawn one more thread to be a Gearman
worker to handle job management for this Jenkins master.  We'll call
it the "management worker thread" and register the following function:

  stop:$hostname

4) Any Gearman client can connect to the Gearman server and send a
request to build a Jenkins project or cancel a project.  View the
examples to see how this can be done.

5) The Gearman workers will service any client request that come
through to start/cancel a Jenkins build.
