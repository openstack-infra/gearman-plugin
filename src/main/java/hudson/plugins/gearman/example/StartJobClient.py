'''

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

'''

'''
 A python example of how to start a jenkins job using a Gearman client

 author: Khai Do
'''



from gearman import GearmanClient
import simplejson
import uuid

server = '127.0.0.1:4730'
client = GearmanClient([server])
function = 'build:pep8:precise'
build_id = uuid.uuid4().hex
build_params = {'param1':"red", 'param2':"white", 'param3':"blue"}

# Submit a synchronous job request to the job server
print 'Sending job ' + build_id + ' to ' + server + ' with params ' + str(build_params)
request = client.submit_job(function,
                            simplejson.dumps(build_params),
                            poll_timeout=60,
                            unique=build_id)

if (request.exception) :
    print request.exception
else:
    print ', '.join(request.warning_updates)
    print request.result
    print 'Work complete with state %s' % request.state
