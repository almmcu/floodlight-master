package net.floodlightcontroller.assignment.forwarding;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.assignment.algorithms.dijsktra.model.Node;
import net.floodlightcontroller.assignment.model.Path;
import net.floodlightcontroller.assignment.topology.DiscoverNetworkTopology;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.util.OFMessageUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Forwarding2 implements IFloodlightModule, IOFMessageListener {

    protected IFloodlightProviderService floodlightProvider;
    protected static Logger logger;
    private Hashtable<String, Hashtable<String, OFPort>> flowTable = new Hashtable<>();
    private void writePacketOutForPacketIn(IOFSwitch sw, OFPacketIn packetInMessage, OFPort egressPort) {
        OFMessageUtils.writePacketOutForPacketIn(sw, packetInMessage, egressPort);
    }

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
        switch (msg.getType()) {

            case PACKET_IN:

                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
                OFPacketIn pi = (OFPacketIn) msg;

                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                OFPort inPort = pi.getMatch().get(MatchField.IN_PORT);
                OFPort outPort = OFPort.FLOOD;

                if (flowTable.containsKey(sw.getId().toString())) {

                    // Check whether Destination MAC is in the table
                    if (flowTable.get(sw.getId().toString()).containsKey(eth.getDestinationMACAddress().toString())) {
                        logger.info("Destination MAC is in the table. ");
                        outPort = flowTable.get(sw.getId().toString()).get(eth.getDestinationMACAddress().toString());

                        ShortestPath shortestPath = new ShortestPath();
                        List<Path> shortestPathList =   shortestPath.findShortestPaths(srcMac, dstMac, DiscoverNetworkTopology.deviceSet);
                        System.out.println(shortestPathList);

                    }

                    // Table does not contain Destination MAC Address
                    // Set the port to flood and Send packet_out action to the switch

                    else {

                        logger.info("Destination MAC is not in the table !!!!!!!!!!");
                        flowTable.get(sw.getId().toString()).put(srcMac.toString(),
                                inPort);
                        this.writePacketOutForPacketIn(sw, pi, OFPort.FLOOD);

                    }

                } else {// record switch flow table

                    logger.info("Destination MAC is not in the table !!!!!!!!!!");
                    Hashtable<String, OFPort> sFlowTable = new Hashtable<>();
                    sFlowTable.put(srcMac.toString(), inPort);
                    flowTable.put(sw.getId().toString(), sFlowTable);
                    this.writePacketOutForPacketIn(sw, pi, OFPort.FLOOD);
                }



                break;
            default:
                break;
        }
        return Command.CONTINUE;
    }

}