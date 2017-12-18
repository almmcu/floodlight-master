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
    "eth_src":"10:00:00:00:00:01",
    "eth_dst":"10:00:00:00:00:02",
    "active":"true",
    "actions":"group=1"
    }
 
flow12 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_12",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"output=3"
    }

flow13 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_13",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"output=3"
    }

flow14 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_14",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_src":"10:00:00:00:00:01",
    "eth_dst":"10:00:00:00:00:02",
    "active":"true",
    "actions":"output=2"
    }
 

#switch3
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
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"group=2"
    }
 
flow32 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_32",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_src":"10:00:00:00:00:01",
    "eth_dst":"10:00:00:00:00:02",
    "active":"true",
    "actions":"output=3"
    }

flow33 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_33",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "eth_src":"10:00:00:00:00:01",
    "eth_dst":"10:00:00:00:00:02",
    "active":"true",
    "actions":"output=3"
    }

flow34 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_34",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"output=2"
    }


#switch s2a
flows2a1 = {
    'switch':"00:00:00:00:00:00:00:2a",
    "name":"flow_mod_2a1",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_dst":"10:00:00:00:00:02",
    "eth_src":"10:00:00:00:00:01",
    "active":"true",
    "actions":"group=3"
    }


group3 = {
    "switch" : "00:00:00:00:00:00:00:2a",
    "entry_type" : "group",
    "name" : "group-mod-3",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "3",
    "group_buckets" : [ 
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "1",
            "bucket_watch_port": "2",
            "bucket_actions":"output=2"
        },
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "2", 
            "bucket_watch_port": "1",
            "bucket_actions" : "output=in_port"
        }
    ]
}
 
flows2a2 = {
    'switch':"00:00:00:00:00:00:00:2a",
    "name":"flow_mod_2a2",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"group=4"
    }

group4 = {
    "switch" : "00:00:00:00:00:00:00:2a",
    "entry_type" : "group",
    "name" : "group-mod-4",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "4",
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
            "bucket_actions" : "output=in_port"
        }
    ]
}


#switch s2b
flows2b1 = {
    'switch':"00:00:00:00:00:00:00:2b",
    "name":"flow_mod_2b1",
    "cookie":"0",
    "priority":"32768",
    "in_port":"1",
    "eth_dst":"10:00:00:00:00:02",
    "eth_src":"10:00:00:00:00:01",
    "active":"true",
    "actions":"group=5"
    }


group5 = {
    "switch" : "00:00:00:00:00:00:00:2b",
    "entry_type" : "group",
    "name" : "group-mod-5",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "5",
    "group_buckets" : [ 
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "1",
            "bucket_watch_port": "2",
            "bucket_actions":"output=2"
        },
        {
	    "bucket_watch_group": "any",
            "bucket_id" : "2", 
            "bucket_watch_port": "1",
            "bucket_actions" : "output=in_port"
        }
    ]
}
 
flows2b2 = {
    'switch':"00:00:00:00:00:00:00:2b",
    "name":"flow_mod_2b2",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "eth_src":"10:00:00:00:00:02",
    "eth_dst":"10:00:00:00:00:01",
    "active":"true",
    "actions":"group=6"
    }

group6 = {
    "switch" : "00:00:00:00:00:00:00:2b",
    "entry_type" : "group",
    "name" : "group-mod-6",
    "active" : "true",
    "group_type" : "fast_failover",
    "group_id" : "6",
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
            "bucket_actions" : "output=in_port"
        }
    ]
}



####### EXTRA #############
flow121 = {
    'switch':"00:00:00:00:00:00:00:01",
    "name":"flow_mod_121",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "eth_src":"10:00:00:00:00:02",
    "active":"true",
    "actions":"output=3"
    }

flow321 = {
    'switch':"00:00:00:00:00:00:00:03",
    "name":"flow_mod_321",
    "cookie":"0",
    "priority":"32768",
    "in_port":"3",
    "active":"true",
    "actions":"output=2"
    }

flows2a4 = {
    'switch':"00:00:00:00:00:00:00:2a",
    "name":"flow_mod_2a4",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"group=4"
    }

flows2b4 = {
    'switch':"00:00:00:00:00:00:00:2b",
    "name":"flow_mod_2b4",
    "cookie":"0",
    "priority":"32768",
    "in_port":"2",
    "active":"true",
    "actions":"group=6"
    }

pusher.set(group1)
pusher.set(flow1)
pusher.set(flow12)
pusher.set(flow13)
pusher.set(flow14)


pusher.set(group2)
pusher.set(flow3)
pusher.set(flow32)
pusher.set(flow33)
pusher.set(flow34)


pusher.set(group4)
pusher.set(group3)
pusher.set(flows2a1)
pusher.set(flows2a2)


pusher.set(group5)
pusher.set(group6)
pusher.set(flows2b1)
pusher.set(flows2b2)



####
pusher.set(flow121)
pusher.set(flow321)
#pusher.set(flows2a4)
pusher.set(flows2b4)

