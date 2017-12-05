package net.floodlightcontroller.assignment;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class LSfromHub implements IFloodlightModule, IOFMessageListener {

    protected IFloodlightProviderService floodlightProvider;
    protected Set<Long> macAddresses;
    protected static Logger logger;
    protected static int c = 0;
    protected OFPort port2 = OFPort.FLOOD;
    protected long startTime;
    Hashtable<String, Hashtable<String, OFPort>> flowtable = new Hashtable<>();

    @Override
    public String getName() {
        return LSfromHub.class.getSimpleName();

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
        macAddresses = new ConcurrentSkipListSet<Long>();
        logger = (Logger) LoggerFactory.getLogger(LSfromHub.class);
        // logger = LoggerFactory.getLogger(MACTracker.class);
        // System.out.println("************************/n/n/n/nn/********************");
        long timeMillis = System.currentTimeMillis();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);


    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        logger.info("================START==========================");

        switch (msg.getType()) {

            case PACKET_IN:

                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
                OFPacketIn pi = (OFPacketIn) msg;
                OFMessage outMessage;

                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                OFPort inPort = pi.getMatch().get(MatchField.IN_PORT);
                OFPort outPort = OFPort.FLOOD;
                logger.info("-------------------------");
                logger.info("Source MAC address = " + srcMac + "Destination MAC address ==== " + dstMac);
                logger.info("Packet in port..... = " + pi.getMatch().get(MatchField.IN_PORT));
                logger.info("Packet Match = " + pi.getMatch());
                logger.info("Switch {}, switch id = {}", sw.getId().toString(), sw.getId().getLong());
                logger.info("-------------------------");

			if (c== 0) {
				port2 = inPort;
				c++;
			}
			port2 = inPort;

                if (flowtable.containsKey(sw.getId().toString())) {

                    // Check whether Destination MAC is in the table
                    if (flowtable.get(sw.getId().toString()).containsKey(eth.getDestinationMACAddress().toString())) {
                        logger.info("Destination MAC is in the table. ");
                        outPort = flowtable.get(sw.getId().toString()).get(eth.getDestinationMACAddress().toString());
                        //outMessage = createHubPacketOut2(sw, msg, outPort);
                        //sw.write(outMessage);
                        outMessage = createHubFlowMod2(sw, msg, inPort, outPort, cntx); // bu kisida outPort da olabilir bir dene

                    }


                    // Table does not contain Destination MAC Address
                    // Set the port to flood and Send packet_out action to the switch

                    else {

                        logger.info("Destination MAC is not in the table !!!!!!!!!!");
                        flowtable.get(sw.getId().toString()).put(srcMac.toString(),
                                inPort);

                        outMessage = createHubPacketOut2(sw, msg, outPort);

                    }

                } else {// record switch flow table

                    logger.info("Destination MAC is not in the table !!!!!!!!!!");
                    Hashtable<String, OFPort> sFlowTable = new Hashtable<>();
                    sFlowTable.put(srcMac.toString(), inPort);
                    flowtable.put(sw.getId().toString(), sFlowTable);
                    outMessage = createHubPacketOut2(sw, msg, outPort);

                }

                sw.write(outMessage);

                break;
            default:
                break;
        }

        logger.info("===============FINISH===========================");

        return Command.CONTINUE;
    }

    private OFMessage createHubFlowMod(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
        fmb.setBufferId(pi.getBufferId()).setXid(pi.getXid());



        // set actions
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        actionBuilder.setPort(OFPort.FLOOD);
        fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

        return fmb.build();
    }

    private OFMessage createHubPacketOut(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
        pob.setBufferId(pi.getBufferId()).setXid(pi.getXid())
                .setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort()
                        : pi.getMatch().get(MatchField.IN_PORT)));

        // pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort(OFPort.FLOOD);
        // set actions
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        actionBuilder.setPort(OFPort.FLOOD);
        pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

        // set data if it is included in the packetin
        if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
            byte[] packetData = pi.getData();
            pob.setData(packetData);
        }
        return pob.build();

    }

    private OFMessage createHubPacketOut2(IOFSwitch sw, OFMessage msg, OFPort port) {

        logger.info("createHubPacketOut2 port ********** = " + port);

        OFPacketIn pi = (OFPacketIn) msg;
        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
        pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort((pi.getMatch().get(MatchField.IN_PORT)));

        // pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort(OFPort.FLOOD);
        // set actions
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        actionBuilder.setPort(port);
        pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

        // set data if it is included in the packetin
        if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
            byte[] packetData = pi.getData();
            pob.setData(packetData);
        }
        return pob.build();
    }

    private OFMessage createHubFlowMod2(IOFSwitch sw, OFMessage msg, OFPort inPort, OFPort outPort, FloodlightContext cntx) {

        logger.info("createHubFlowMod2 port********** = " + outPort);

        OFPacketIn pi = (OFPacketIn) msg;
        OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();


        fmb.setBufferId(pi.getBufferId()).setXid(pi.getXid());
        fmb.setMatch( createMatchFromPacket(sw, inPort, cntx));
        fmb.setIdleTimeout(1000000);
        fmb.setOutPort(outPort);


        // set actions
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        actionBuilder.setPort(outPort);
        fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));


        return fmb.build();
    }

    private OFMessage createHubFlowMod3(IOFSwitch sw, OFMessage msg, OFPort inPort, OFPort outPort, FloodlightContext cntx) {

        logger.info("createHubFlowMod2 port********** = " + outPort);

        OFPacketIn pi = (OFPacketIn) msg;
        OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
        fmb.setBufferId(pi.getBufferId()).setXid(pi.getXid());



        fmb.setMatch( createMatchFromPacket(sw, inPort, cntx));
        fmb.setOutPort(outPort);
        // set actions
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        actionBuilder.setPort(outPort);


        fmb.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

        return fmb.build();
    }

    protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
        // The packet in match will only contain the port number.
        // We need to add in specifics for the hosts we're routing between.
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();
        // inPort = pi.getMatch().get(MatchField.IN_PORT);

        Match.Builder mb = sw.getOFFactory().buildMatch();
        mb.setExact(MatchField.IN_PORT, inPort)
                .setExact(MatchField.ETH_SRC, srcMac)
                .setExact(MatchField.ETH_DST, dstMac);

        if (!vlan.equals(VlanVid.ZERO)) {
            mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
        }

        return mb.build();
    }


}
