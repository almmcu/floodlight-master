package net.floodlightcontroller.assignment.topology;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.assignment.algorithms.dijsktra.Dijkstra;
import net.floodlightcontroller.assignment.model.Device;
import net.floodlightcontroller.assignment.algorithms.dijsktra.model.Graph;
import net.floodlightcontroller.assignment.algorithms.dijsktra.model.Node;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Forwarding2 implements IFloodlightModule, IOFMessageListener {

    public static Hashtable <Node, Graph> graphTable;
    protected static Logger logger;
    protected static int c = 0;
    protected IFloodlightProviderService floodlightProvider;
    protected long startTime;
    Hashtable<String, Hashtable<String, OFPort>> flowtable = new Hashtable<>();
    Hashtable <String, Node> nodeTable;

    @Override
    public String getName() {
        return Forwarding2.class.getSimpleName();

    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = (Logger) LoggerFactory.getLogger(Forwarding2.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);


    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        long timeMillis = System.currentTimeMillis();

        if( c==0 ) {
            startTime = TimeUnit.MILLISECONDS.toSeconds(timeMillis);
            c=1;
        }

        try
        {
            long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);

            if (timeSeconds - startTime > 15 && c==1) {
                c=2;
                calculateShortestPathbwSwitches();

                /**
                 * graphTable includes graph objects
                 *
                 * every graph includes shortest path information between one switch to other switches
                 * every shortest path switches represented by a node object
                 *
                 * */
                System.out.println(graphTable);
            }// end if -- 15 seconds
        }
        catch(Exception ex)
        {

        }
        return Command.CONTINUE;
    }

    public Hashtable<Node, Graph> getGraphTable() {
        return graphTable;
    }

    public void setGraphTable(Hashtable<Node, Graph> graphTable) {
        this.graphTable = graphTable;
    }

    void calculateShortestPathbwSwitches(){
        nodeTable  = new Hashtable<>();
        graphTable = new Hashtable<>();

        System.out.println(DiscoverNetworkTopology.deviceSet);
        for (Device device :
                DiscoverNetworkTopology.deviceSet) {
            if (device.getType().equals("Switch")) {
                String switchId = device.getSrcMACAddress().toString();
                Node node = new Node(switchId);
                nodeTable.put(switchId, node);
            }
        }// end - foreach
        for (Device device :
                DiscoverNetworkTopology.deviceSet) {
            if (device.getType().equals("Switch")) {
                String switchId = device.getSrcMACAddress().toString();
                String destSwitchId = device.getDestMACAddress().toString();
                nodeTable.get(switchId).addDestination(nodeTable.get(destSwitchId), 1);

            }
        }// end - foreach

        Collection<Node> nodes = nodeTable.values();
        for (Node mainNode :
                nodes) {
            Hashtable <String, Node> tempNodeTable  = new Hashtable<>();
            for (Device device :
                    DiscoverNetworkTopology.deviceSet) {
                if (device.getType().equals("Switch")) {
                    String switchId = device.getSrcMACAddress().toString();
                    Node node = new Node(switchId);
                    tempNodeTable.put(switchId, node);
                }
            }// end - foreach tempNodeTable

            for (Device device :
                    DiscoverNetworkTopology.deviceSet) {
                if (device.getType().equals("Switch")) {
                    String switchId = device.getSrcMACAddress().toString();
                    String destSwitchId = device.getDestMACAddress().toString();
                    tempNodeTable.get(switchId).addDestination(tempNodeTable.get(destSwitchId), 1);

                }
            }// end - foreach


            Graph graph = new Graph();
            Dijkstra dijkstra = new Dijkstra();

            Collection<Node> tmpNodes = tempNodeTable.values();
            for (Node node :
                    tmpNodes) {

                graph.addNode(node);
            }

            for (Node node :
                    tmpNodes) {
                if (node.getName().equals(mainNode.getName()))
                    graph = dijkstra.calculateShortestPathFromSource(graph, node);

            }


            graphTable.put(mainNode, graph);

        }// end Main Nodes foreach-loop

    }
}