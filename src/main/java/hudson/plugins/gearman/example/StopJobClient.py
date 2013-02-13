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
 A python example of how to stop a jenkins job using a Gearman client

 author: Khai Do
'''

from gearman import GearmanClient
import simplejson
import uuid

server = '127.0.0.1'
port = '4730'
conn = server + ":" + port
client = GearmanClient([conn])
function = "stop:" + server
build_id = uuid.UUID('{5ac10210-0405-0607-4809-1a0b0c0d0e0f}').hex
build_params = {'uuid':build_id}

# Submit a synchronous job request to the job server
print 'Sending job ' + function + ' to ' + conn + ' with id ' + build_id
request = client.submit_job(function,
                            simplejson.dumps(build_params),
                            poll_timeout=60,
                            unique=build_id)

print request.result
