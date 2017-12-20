package net.floodlightcontroller.assignment.topology;

import net.floodlightcontroller.assignment.model.Device;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.topology.ITopologyService;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.*;


/**
 * <h1> Topology Discovery</h1>
 * <p>
 * This class is for;
 * - Get link information and switch information. (Switch IDs and ports)
 * - Get Host information. (Which host connected which switch and which port.)
 * <p>
 * ------ Get Link  and Switch Information -----
 * ILinkDiscoveryListener interface for linkDiscoveryUpdate method and
 * this method gives us the link information between two switches
 * <p>
 * ----- Get Switch Information -----------
 * IDeviceListener interface
 * IDeviceListener interface deviceAdded method for getting host information
 */
public class DiscoverNetworkTopology implements IFloodlightModule, ILinkDiscoveryListener, IDeviceListener {

    /**
     * deviceSet keeps information about Switches (Link Info) and Hosts*
     * it is static variable so information inside it is gotten from other classes
     **/
    public static ArrayList<Device> deviceSet = new ArrayList<Device>();
    protected IDeviceService deviceManagerService;
    protected ILinkDiscoveryService linkService;
    protected ITopologyService topologyService;

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
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IDeviceService.class);
        l.add(ITopologyService.class);
        l.add(ILinkDiscoveryService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
        this.linkService = context.getServiceImpl(ILinkDiscoveryService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        linkService.addListener(this);
        deviceManagerService.addListener(this);
        topologyService.addListener(this::linkDiscoveryUpdate);
    }

    /**
     * This method gets links between switches. One link has four information.
     * 1-Source switch DatapathId
     * 2-Source switch port
     * 3-Destination switch DatapathId
     * 4-Destination switch port
     */
    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {

        Map a = topologyService.getAllLinks();
        for (LDUpdate u : updateList) {
            if (u.getOperation() == UpdateOperation.SWITCH_UPDATED) {
                if (a.size() != 0) {

                    for (Iterator<DatapathId> itr = a.keySet().iterator(); itr.hasNext(); ) {

                        DatapathId swDataPathId = itr.next();
                        HashSet<Link> links = (HashSet<Link>) a.get(swDataPathId);
                        Iterator itr1 = links.iterator();

                        while (itr1.hasNext()) {
                            Link link = (Link) itr1.next();

                            if (link.getSrc().toString().equals(swDataPathId.toString())) {
                                // Cift Yonlu olarak linkler elimizde
                                //System.out.println(link.getSrc() + "----------------" + link.getDst());
                                Boolean hasLink = false;
                                Device switchDevice = new Device(link.getSrc(), link.getSrcPort(), link.getDst(), link.getDstPort(), "Switch");


                                for (Device device :
                                        deviceSet) {
                                    if (
                                            device.getSrcMACAddress().toString().equals(switchDevice.getSrcMACAddress().toString()) &&
                                                    device.getDestMACAddress().toString().equals(switchDevice.getDestMACAddress().toString()) &&
                                                    device.getSrcPort().toString().equals(switchDevice.getSrcPort().toString())
                                            ) {
                                        hasLink = true;
                                        break;
                                    } else {
                                        hasLink = false;

                                    }
                                }
                                if (!hasLink)
                                    deviceSet.add(switchDevice);
                            }

                        }
                    }

                }

            }
        }
    }

    /**
     * This methods gives us host information.
     * Host MAC ADRESS and connected switch and port
     */
    @Override
    public void deviceAdded(IDevice device) {

        String macAddressString = device.getMACAddressString();
        DatapathId macAddress = DatapathId.of(device.getMACAddress());
        if (macAddressString.startsWith("00:00:") || macAddressString.startsWith("10:00:"))
            if (device.getAttachmentPoints().length == 1) {
                SwitchPort switchPort = device.getAttachmentPoints()[0];
                Device hostDevice = new Device(macAddress, switchPort.getPortId(), switchPort.getNodeId(), null, "Host");
                deviceSet.add(hostDevice);
            }


    }

    @Override
    public void deviceRemoved(IDevice device) {

    }

    @Override
    public void deviceMoved(IDevice device) {

    }

    @Override
    public void deviceIPV4AddrChanged(IDevice device) {

    }

    @Override
    public void deviceIPV6AddrChanged(IDevice device) {

    }

    @Override
    public void deviceVlanChanged(IDevice device) {

    }

    @Override
    public String getName() {
        return DiscoverNetworkTopology.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(String type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(String type, String name) {
        return false;
    }
}
