<h1>SDN Course Assignment</h1>

<h2>With This Project;</h2>
<h3> Default Forwarding module is  removed from the floodlight.properties file.<br>
</h3>
<h4>
Our FastFailoverForwarding moule is added to the floodlight.properties file.<br>
This module does the forwarding.
- Finds two shortest path between source and destination.
- Insert rules to switches. 
- When the link on main path is broken, packets are forwarded from the alternate path with the help of fast failover type groups.. 
<br>
</h4>

<h4>In ExampleCodes folder; <br>
There are four topology example file and static entry pusher rules for ff1.py topology.</h4>


<br><h4>Mininet tcpdump Commands</h4>
<br>
<pre> mininet&gt; xterm h1 h2
</pre>



<p>Arrange each xterm so that they're all on the screen at once.  This may require reducing the height of to fit a cramped laptop screen.
</p>
<p>In the xterms for h2, run <code>tcpdump</code>, a utility to print the packets seen by a host:
</p>
<pre> # tcpdump -XX -n -i h2-eth0
</pre>

<p>In the xterm for h1, send a ping:
</p>
<pre> # ping -c1 10.0.0.2
</pre>