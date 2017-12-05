package net.floodlightcontroller.assignment.model;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

public class Device {
    DatapathId srcMACAddress;
    OFPort srcPort;

    DatapathId destMACAddress;
    OFPort destPort;

    String type;
    public Device(DatapathId srcMACAddress, OFPort srcPort, DatapathId destMACAddress, OFPort destPort, String type) {
        this.srcMACAddress = srcMACAddress;
        this.srcPort = srcPort;
        this.destMACAddress = destMACAddress;
        this.destPort = destPort;
        this.type  = type;
    }

    public DatapathId getSrcMACAddress() {
        return srcMACAddress;
    }

    public void setSrcMACAddress(DatapathId srcMACAddress) {
        this.srcMACAddress = srcMACAddress;
    }

    public OFPort getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(OFPort srcPort) {
        this.srcPort = srcPort;
    }

    public DatapathId getDestMACAddress() {
        return destMACAddress;
    }

    public void setDestMACAddress(DatapathId destMACAddress) {
        this.destMACAddress = destMACAddress;
    }

    public OFPort getDestPort() {
        return destPort;
    }

    public void setDestPort(OFPort destPort) {
        this.destPort = destPort;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Device{" +
                "srcMACAddress=" + srcMACAddress +
                ", srcPort=" + srcPort +
                ", destMACAddress=" + destMACAddress +
                ", destPort=" + destPort +
                ", type='" + type + '\'' +
                '}';
    }
}
