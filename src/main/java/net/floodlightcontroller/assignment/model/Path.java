package net.floodlightcontroller.assignment.model;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.ArrayList;
import java.util.List;

public class Path {
    List<DatapathId> switchList = new ArrayList<>();
    List<OFPort> portList = new ArrayList<>();

    public List<DatapathId> getSwitchList() {
        return switchList;
    }

    public void setSwitchList(List<DatapathId> switchList) {
        this.switchList = switchList;
    }

    public List<OFPort> getPortList() {
        return portList;
    }

    public void setPortList(List<OFPort> portList) {
        this.portList = portList;
    }
}
