# Fast-Failover-Demo Topology
##### Run From Terminal #########
## Terminal and the .py file should be in the same directory
#sudo mn --mac --custom ff2.py --topo mytopo --controller=remote,ip=127.0.0.1,port=6653 --switch=ovsk,protocols=OpenFlow13
from mininet.topo import Topo 
from mininet.cli import CLI 
from mininet.net import Mininet 
from mininet.link import TCLink 
from mininet.util import irange,dumpNodeConnections 
from mininet.log import setLogLevel 
 
class Fast_Failover_Demo_Topo(Topo): 
    "Topology for fast failover demo of OpenFlow 1.3 groups." 
 
    def __init__(self): 
        # Initialize topology and default options 
        Topo.__init__(self) 
        
        s1 = self.addSwitch('s1',dpid='0000000000000001') 
        s2a = self.addSwitch('s2a',dpid='000000000000002a') 
        s2b = self.addSwitch('s2b',dpid='000000000000002b') 
        

	s2c = self.addSwitch('s2c',dpid='000000000000002c') 
        s2d = self.addSwitch('s2d',dpid='000000000000002d') 
        
	s3 = self.addSwitch('s3',dpid='0000000000000003') 
        
	self.addLink(s1, s2a) 
        self.addLink(s1, s2b)

	self.addLink(s2a, s2c) 
        self.addLink(s2b, s2d)

 
        self.addLink(s3, s2c) 
        self.addLink(s3, s2d) 
        host_1 = self.addHost('h1',ip='10.0.0.1',mac='10:00:00:00:00:01') 
        host_2 = self.addHost('h2',ip='10.0.0.2',mac='10:00:00:00:00:02')        
        self.addLink(host_1, s1) 
        self.addLink(host_2, s3)   
                    
topos = { 'mytopo': ( lambda: Fast_Failover_Demo_Topo() ) }
