package net.floodlightcontroller.assignment.forwarding;

import ch.qos.logback.classic.Logger;
import net.floodlightcontroller.assignment.http.Post;
import net.floodlightcontroller.assignment.model.Path;
import net.floodlightcontroller.assignment.model.Switch;
import net.floodlightcontroller.assignment.shortest_two_path.ShortestPath;
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
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
import net.floodlightcontroller.util.OFMessageUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class FastFailoverForwarding implements IFloodlightModule, IOFMessageListener {
    protected static Logger logger;
    protected IFloodlightProviderService floodlightProvider;
    protected IStaticEntryPusherService staticFlowEntryPusher;
    List<String> macAdressLink = new ArrayList<>();
    private Hashtable<String, Hashtable<String, OFPort>> flowTable = new Hashtable<>();
    private int counter = 1;

    private void writePacketOutForPacketIn(IOFSwitch sw, OFPacketIn packetInMessage, OFPort egressPort) {
        OFMessageUtils.writePacketOutForPacketIn(sw, packetInMessage, egressPort);
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        switch (msg.getType()) {

            case PACKET_IN:

                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
                OFPacketIn pi = (OFPacketIn) msg;
                OFMessage outMessage;

                MacAddress srcMac = eth.getSourceMACAddress();
                MacAddress dstMac = eth.getDestinationMACAddress();
                OFPort inPort = pi.getMatch().get(MatchField.IN_PORT);
                OFPort outPort = OFPort.FLOOD;

                if (flowTable.containsKey(sw.getId().toString())) {

                    // Check whether Destination MAC is in the table
                    if (flowTable.get(sw.getId().toString()).containsKey(eth.getDestinationMACAddress().toString())) {
                        logger.info("Destination MAC is in the table. ");
                        outPort = flowTable.get(sw.getId().toString()).get(eth.getDestinationMACAddress().toString());

                        String macc1 = srcMac.toString() + "-" + dstMac.toString();
                        String macc2 = dstMac.toString() + "-" + srcMac.toString();
                        if (!macAdressLink.contains(macc1) && !macAdressLink.contains(macc2)) {

                            macAdressLink.add(macc1);
                            macAdressLink.add(macc2);

                            // En kisa iki yol hesabi
                            ShortestPath shortestPath = new ShortestPath();
                            List<Path> shortestPathList = shortestPath.findShortestPaths(srcMac, dstMac, DiscoverNetworkTopology.deviceSet);

                            System.out.println(shortestPathList);
                            System.out.println(shortestPath.getPathTable());


                            // Kurallari post etmek icin
                            Post post = new Post();
                            JSONObject jsonObject = new JSONObject();

                            //AlternatePath
                            //shortestPath.getPathTable().get("MainPath");


                            ///////////////////////////////////
                            // Aradaki Switch ler icin kurallar
                            int i = 1;// ana yol switchler
                            for (i = 1; i < shortestPath.getPathTable().get("MainPath").size() - 1; i++) {

                                // inport paketin switch e geldigi port
                                OFPort inport = shortestPath.getPathTable().get("MainPath").get(i).getInPort();
                                //outport: bir sonraki switch e bagli olan port
                                OFPort outport = shortestPath.getPathTable().get("MainPath").get(i).getOutPort();

                                //Ana Yol Ustu Switchler
                                // 1. Grup tablosu ekleme

                                jsonObject = intraGroupAdd1(shortestPath.getPathTable().get("MainPath").get(i));
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // Gruba yonlendirmek icin Flow tablosuna kural ekle
                                jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("MainPath").get(i),
                                        inport.toString(),
                                        "group=" + counter);
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // Ana yol uzerinde TERS yone kurallar ekle


                                // 1. Grup

                                jsonObject = intraGroupAdd2(shortestPath.getPathTable().get("MainPath").get(i));
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // 2. Flow tablosuna gruba yonlendirmek icin kural
                                // source mac ile dest mac yer degistiriyor.
                                // inport = outport oluyor
                                // action = bir once olusturulan group oluyor
                                jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("MainPath").get(i),
                                        outport.toString(),
                                        "group=" + counter);
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                                // Son olarak link kotugunda
                                // geriye gelen paketler icin kural
                                // Gonderdigi paket gonderdigi porttan geri gelmisse

                                jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("MainPath").get(i),
                                        outport.toString(),
                                        "output=" + inport.toString());
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }// end for


                            // KAYNAK SWITCH icin

                            // 1. kural grup tablosu ekleme.
                            jsonObject = groupAdd2(shortestPath.getPathTable().get("MainPath").get(0),
                                    shortestPath.getPathTable().get("AlternatePath").get(0));

                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 2. Flow tablosuna bir once olusturulan
                            // grup icin kural ekleme

                            jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("MainPath").get(0),
                                    shortestPath.getPathTable().get("MainPath").get(0).getInPort().toString(),
                                    "group=" + counter);
                            System.out.println(jsonObject.toString());

                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 3. ve 4. kurallar kaynak switc deki host a gelen paketler icin

                            ///////////3///////////

                            jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("MainPath").get(0),
                                    shortestPath.getPathTable().get("MainPath").get(0).getOutPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("MainPath").get(0).getInPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ////////////4///////
                            jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("AlternatePath").get(0),
                                    shortestPath.getPathTable().get("AlternatePath").get(0).getOutPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("MainPath").get(0).getInPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 5. kural
                            // Gonderilen paket gonderdigi porttan geri gelmisse
                            // Alternatif yola yonlendir.

                            jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("MainPath").get(0),
                                    shortestPath.getPathTable().get("MainPath").get(0).getOutPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("AlternatePath").get(0).getOutPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // EXTRA
                            jsonObject = extraFlowAdd(shortestPath.getPathTable().get("MainPath").get(0),
                                    shortestPath.getPathTable().get("AlternatePath").get(0).getOutPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("MainPath").get(0).getInPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // EXTRA


                            // Alternatif yoldaki switcler icin
                            // Aradaki switcler..
                            ///////////////////////////////////
                            // Aradaki Switch ler icin kurallar
                            int j = 1;// ana yol switchler
                            for (j = 1; j < shortestPath.getPathTable().get("AlternatePath").size() - 1; j++) {

                                // inport paketin switch e geldigi port
                                OFPort inport = shortestPath.getPathTable().get("AlternatePath").get(j).getInPort();
                                //outport: bir sonraki switch e bagli olan port
                                OFPort outport = shortestPath.getPathTable().get("AlternatePath").get(j).getOutPort();

                                //Ana Yol Ustu Switchler
                                // 1. Grup tablosu ekleme

                                jsonObject = intraGroupAdd1(shortestPath.getPathTable().get("AlternatePath").get(j));
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // Gruba yonlendirmek icin Flow tablosuna kural ekle
                                jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("AlternatePath").get(j),
                                        inport.toString(),
                                        "group=" + counter);
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // Ana yol uzerinde TERS yone kurallar ekle


                                // 1. Grup

                                jsonObject = intraGroupAdd2(shortestPath.getPathTable().get("AlternatePath").get(j));
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // 2. Flow tablosuna gruba yonlendirmek icin kural
                                // source mac ile dest mac yer degistiriyor.
                                // inport = outport oluyor
                                // action = bir once olusturulan group oluyor
                                jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("AlternatePath").get(j),
                                        outport.toString(),
                                        "group=" + counter);
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // EXTRA
                                jsonObject = extraFlowAdd(shortestPath.getPathTable().get("AlternatePath").get(j),
                                        outport.toString(),
                                        "group=" + counter);
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // EXTRA

                                // Son olarak link kotugunda
                                // geriye gelen paketler icin kural
                                // Gonderdigi paket gonderdigi porttan geri gelmisse

                                jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("AlternatePath").get(j),
                                        outport.toString(),
                                        "output=" + inport.toString());
                                try {
                                    post.postFlow(jsonObject);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }// end for


                            // HEDEF SWITCH icin

                            // 1. kural grup tablosu ekleme.
                            jsonObject = groupAdd3(shortestPath.getPathTable().get("MainPath").get(i),
                                    shortestPath.getPathTable().get("AlternatePath").get(j));

                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 2. Flow tablosuna bir once olusturulan
                            // grup icin kural ekleme

                            jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("MainPath").get(i),
                                    shortestPath.getPathTable().get("MainPath").get(i).getOutPort().toString(),
                                    "group=" + counter);
                            System.out.println(jsonObject.toString());

                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 3. ve 4. kurallar kaynak switc deki host a gelen paketler icin

                            ///////////3///////////

                            jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("MainPath").get(i),
                                    shortestPath.getPathTable().get("MainPath").get(i).getInPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("MainPath").get(i).getOutPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ////////////4///////
                            jsonObject = flowAdd(srcMac, dstMac, shortestPath.getPathTable().get("AlternatePath").get(j),
                                    shortestPath.getPathTable().get("AlternatePath").get(j).getInPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("MainPath").get(i).getOutPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 5. kural
                            // Gonderilen paket gonderdigi porttan geri gelmisse
                            // Alternatif yola yonlendir.

                            jsonObject = flowAdd(dstMac, srcMac, shortestPath.getPathTable().get("MainPath").get(i),
                                    shortestPath.getPathTable().get("MainPath").get(i).getInPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("AlternatePath").get(j).getInPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // EXTRA
                            jsonObject = extraFlowAdd(shortestPath.getPathTable().get("AlternatePath").get(j),
                                    shortestPath.getPathTable().get("AlternatePath").get(j).getOutPort().toString(),
                                    "output=" + shortestPath.getPathTable().get("AlternatePath").get(j).getInPort().toString());
                            try {
                                post.postFlow(jsonObject);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // EXTRA

                            System.out.println("\n***************MAIN ROAD********************");
                            for (DatapathId mainRoadSwitch :
                                    shortestPathList.get(0).getSwitchList()) {
                                System.out.print(mainRoadSwitch.toString() + " -- ");
                            }

                            System.out.println("\n\n***************ALTERNATE ROAD********************");
                            for (DatapathId alternateRoadSwitch :
                                    shortestPathList.get(1).getSwitchList()) {
                                System.out.print(alternateRoadSwitch.toString() + " -- ");
                            }
                            System.out.println();
                            System.out.println();

                        } // enf if mac adress
                    }// end if flow table

                    // Table does not contain Destination MAC Address
                    // Set the port to flood and Send packet_out action to the switch

                    else {

                        //logger.info("Destination MAC is not in the table !!!!!!!!!!");
                        flowTable.get(sw.getId().toString()).put(srcMac.toString(),
                                inPort);
                        //this.writePacketOutForPacketIn(sw, pi, OFPort.FLOOD);
                        outMessage = createHubPacketOut(sw, msg);
                        sw.write(outMessage);

                    }

                } else {// record switch flow table

                    //logger.info("Destination MAC is not in the table !!!!!!!!!!");
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

    @Override
    public String getName() {
        return FastFailoverForwarding.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IStaticEntryPusherService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = (Logger) LoggerFactory.getLogger(FastFailoverForwarding.class);
        staticFlowEntryPusher = context.getServiceImpl(IStaticEntryPusherService.class);

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

    }

    JSONObject flowAdd(MacAddress srcMACAddress, MacAddress destMACAddress, Switch sw, String inport, String action) {
        JSONObject json = new JSONObject();
        json.put("switch", "" + sw.getSwitchId());
        json.put("name", "flow_mod_" + counter);
        json.put("cookie", "0");
        json.put("priority", "32768");
        json.put("in_port", "" + inport);
        json.put("eth_src", "" + srcMACAddress);
        json.put("eth_dst", "" + destMACAddress);
        json.put("active", "true");
        json.put("actions", action);// output=outPort or group=1
        counter++;
        return json;
    }


    JSONObject intraGroupAdd1(Switch sw) {
        JSONObject json = new JSONObject();
        JSONArray jsonBucket = new JSONArray();
        JSONObject jsonObjectBucket = new JSONObject();

        json.put("switch", "" + sw.getSwitchId());
        json.put("name", "group-mod-" + counter);
        json.put("entry_type", "group");
        json.put("group_type", "fast_failover");
        json.put("group_id", "" + counter);
        json.put("active", "true");

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "1");
        jsonObjectBucket.put("bucket_watch_port", "" + sw.getOutPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw.getOutPort().toString());
        jsonBucket.add(jsonObjectBucket);

        jsonObjectBucket = new JSONObject();

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "2");
        jsonObjectBucket.put("bucket_watch_port", "" + sw.getInPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=in_port");
        jsonBucket.add(jsonObjectBucket);

        json.put("group_buckets", jsonBucket);// output=outPort or group=1

        return json;
    }

    JSONObject intraGroupAdd2(Switch sw) {
        JSONObject json = new JSONObject();
        JSONArray jsonBucket = new JSONArray();
        JSONObject jsonObjectBucket = new JSONObject();

        json.put("switch", "" + sw.getSwitchId());
        json.put("name", "group-mod-" + counter);
        json.put("entry_type", "group");
        json.put("group_type", "fast_failover");
        json.put("group_id", "" + counter);
        json.put("active", "true");

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "1");
        jsonObjectBucket.put("bucket_watch_port", "" + sw.getInPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw.getInPort().toString());
        jsonBucket.add(jsonObjectBucket);

        jsonObjectBucket = new JSONObject();

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "2");
        jsonObjectBucket.put("bucket_watch_port", "" + sw.getOutPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=in_port");
        jsonBucket.add(jsonObjectBucket);

        json.put("group_buckets", jsonBucket);// output=outPort or group=1

        return json;
    }

    JSONObject groupAdd3(Switch sw1, Switch sw2) {
        JSONObject json = new JSONObject();
        JSONArray jsonBucket = new JSONArray();
        JSONObject jsonObjectBucket = new JSONObject();

        json.put("switch", "" + sw1.getSwitchId());
        json.put("name", "group-mod-" + counter);
        json.put("entry_type", "group");
        json.put("priority", "32768");
        json.put("group_type", "fast_failover");
        json.put("group_id", "" + counter);

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "1");
        jsonObjectBucket.put("bucket_watch_port", "" + sw1.getInPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw1.getInPort().toString());
        jsonBucket.add(jsonObjectBucket);

        jsonObjectBucket = new JSONObject();


        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "2");
        jsonObjectBucket.put("bucket_watch_port", "" + sw2.getInPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw2.getInPort().toString());
        jsonBucket.add(jsonObjectBucket);

        json.put("group_buckets", jsonBucket);// output=outPort or group=1

        return json;
    }

    JSONObject groupAdd2(Switch sw1, Switch sw2) {
        JSONObject json = new JSONObject();
        JSONArray jsonBucket = new JSONArray();
        JSONObject jsonObjectBucket = new JSONObject();

        json.put("switch", "" + sw1.getSwitchId());
        json.put("name", "group-mod-" + counter);
        json.put("entry_type", "group");
        json.put("priority", "32768");
        json.put("group_type", "fast_failover");
        json.put("group_id", "" + counter);

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "1");
        jsonObjectBucket.put("bucket_watch_port", "" + sw1.getOutPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw1.getOutPort().toString());
        jsonBucket.add(jsonObjectBucket);

        jsonObjectBucket = new JSONObject();

        jsonObjectBucket.put("bucket_watch_group", "any");
        jsonObjectBucket.put("bucket_id", "2");
        jsonObjectBucket.put("bucket_watch_port", "" + sw2.getOutPort().toString());
        jsonObjectBucket.put("bucket_actions", "output=" + sw2.getOutPort().toString());
        jsonBucket.add(jsonObjectBucket);

        json.put("group_buckets", jsonBucket);// output=outPort or group=1

        return json;
    }

    JSONObject extraFlowAdd(Switch sw, String inport, String action) {
        JSONObject json = new JSONObject();
        json.put("switch", "" + sw.getSwitchId());
        json.put("name", "flow_mod_" + counter);
        json.put("cookie", "0");
        json.put("priority", "32768");
        json.put("in_port", "" + inport);
        json.put("active", "true");
        json.put("actions", action);// output=outPort or group=1
        counter++;
        return json;
    }

    private OFMessage createHubPacketOut(IOFSwitch sw, OFMessage msg) {
        OFPacketIn pi = (OFPacketIn) msg;
        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
        pob.setBufferId(pi.getBufferId()).setXid(pi.getXid()).setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

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

}
