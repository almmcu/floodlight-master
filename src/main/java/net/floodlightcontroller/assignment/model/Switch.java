package net.floodlightcontroller.assignment.model;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

public class Switch {

    DatapathId switchId;
    OFPort outPort;
    OFPort inPort;

    public Switch(DatapathId switchId, OFPort outPort, OFPort inPort) {
        this.switchId = switchId;
        this.outPort = outPort;
        this.inPort = inPort;
    }

    public DatapathId getSwitchId() {
        return switchId;
    }

    public void setSwitchId(DatapathId destMACAddress) {
        this.switchId = destMACAddress;
    }

    public OFPort getOutPort() {
        return outPort;
    }

    public void setOutPort(OFPort outPort) {
        this.outPort = outPort;
    }

    public OFPort getInPort() {
        return inPort;
    }

    public void setInPort(OFPort inPort) {
        this.inPort = inPort;
    }
}
