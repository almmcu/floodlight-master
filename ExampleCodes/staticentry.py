# RUN from terminal: python staticentry.py 
import httplib
import json
 
class StaticEntryPusher(object):
 
    def __init__(self, server):
        self.server = server
 
    def get(self, data):
        ret = self.rest_call({}, 'GET')
        return json.loads(ret[2])
 
    def set(self, data):
        ret = self.rest_call(data, 'POST')
        return ret[0] == 200
 
    def remove(self, objtype, data):
        ret = self.rest_call(data, 'DELETE')
        return ret[0] == 200
 
    def rest_call(self, data, action):
        path = '/wm/staticentrypusher/json'
        headers = {
            'Content-type': 'application/json',
            'Accept': 'application/json',
            }
        body = json.dumps(data)
        conn = httplib.HTTPConnection(self.server, 8080)
        conn.request(action, path, body, headers)
        response = conn.getresponse()
        ret = (response.status, response.reason, response.read())
        print ret
        conn.close()
        return ret
 
pusher = StaticEntryPusher('localhost')
# switch 1

group1 = {
    "switch" : "00:00:00:00:00:00:00:01",
    "entry_type" : "group",
    "name" : "group-mod-1",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "1",
    "group_buckets" : [ 
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "1",
            "bucket_watch_port": "1",
            "bucket_actions":"output=1"
        },
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "2", 
            "bucket_watch_port": "2",
            "bucket_actions" : "output=2"
        }
    ]
}
flow1 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_1",
    "cookie":"0",
    "priority":"32768",
    "in_port":"3",
    "active":"true",
    "actions":"group=1"
    }
 
flow12 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_12",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "active":"true",
    "actions":"output=3"
    }
flow13 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_13",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"output=3"
    }
 

#switc3
group2 = {
    "switch" : "00:00:00:00:00:00:00:03",
    "entry_type" : "group",
    "name" : "group-mod-2",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "2",
    "group_buckets" : [ 
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "1",
            "bucket_watch_port": "1",
            "bucket_actions":"output=1"
        },
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "2", 
            "bucket_watch_port": "2",
            "bucket_actions" : "output=2"
        }
    ]
}

flow3 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_3",
    "cookie":"0",
    "priority":"32768",
    "in_port":"3",
    "active":"true",
    "actions":"group=2"
    }
 
flow32 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_32",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "active":"true",
    "actions":"output=3"
    }

flow33 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_33",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"output=3"
    }

#switch s2a
flows2a1 = {
    'switch':"00:00:00:00:00:00:00:2a",
    "name":"flow_mod_2a1",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "active":"true",
    "actions":"output=2"
    }
 
flows2a2 = {
    'switch':"00:00:00:00:00:00:00:2a",
    "name":"flow_mod_2a2",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"output=1"
    }

#switch s2b
flows2b1= {
    'switch':"00:00:00:00:00:00:00:2b",
    "name":"flow_mod_2b1",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "active":"true",
    "actions":"output=2"
    }
 
flows2b2 = {
    'switch':"00:00:00:00:00:00:00:2b",
    "name":"flow_mod_2b2",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"output=1"
    }

pusher.set(group1)
pusher.set(flow1)
pusher.set(flow12)
pusher.set(flow13)


pusher.set(group2)
pusher.set(flow3)
pusher.set(flow32)
pusher.set(flow33)

pusher.set(flows2b1)
pusher.set(flows2b2)
pusher.set(flows2a1)
pusher.set(flows2a2)

