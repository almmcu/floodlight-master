package net.floodlightcontroller.assignment.shortest_two_path;

import net.floodlightcontroller.assignment.algorithms.dijsktra.Dijkstra;
import net.floodlightcontroller.assignment.algorithms.dijsktra.model.Graph;
import net.floodlightcontroller.assignment.algorithms.dijsktra.model.Node;
import net.floodlightcontroller.assignment.model.*;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.*;

public class ShortestPath {
    Hashtable<String, Node> nodeTable;
    Hashtable<Node, Graph> graphTable;
    Hashtable<String, List<Switch>> pathTable;


    public List<Path> findShortestPaths(MacAddress srcMACAddress, MacAddress destMACAddress, ArrayList<Device> deviceSet) {

        ArrayList<Device> tmpDeviceSet = new ArrayList<>();
        pathTable = new Hashtable<>();
        for (Device device :
                deviceSet) {
            Device tmpDevice = new Device(device.getSrcMACAddress(), device.getSrcPort(),
                    device.getDestMACAddress(), device.getDestPort(),
                    device.getType());
            tmpDeviceSet.add(tmpDevice);
        }

        List<Path> shortestPathList = new ArrayList<>();
        Path path = findShortestPathBwTwoHosts(srcMACAddress, destMACAddress, deviceSet, "MainPath");
        for (int i = 0; i < path.getSwitchList().size(); i++) {

            try {
                for (Device switchDevice :
                        deviceSet) {
                    if (switchDevice.getType().equals("Switch")) {
                        if (path.getSwitchList().get(i).toString().equals(switchDevice.getSrcMACAddress().toString())
                                && path.getPortList().get(i).toString().equals(switchDevice.getSrcPort().toString())) {

                            int index = 0;
                            for (Device tmpDevice :
                                    tmpDeviceSet) {
                                if (path.getSwitchList().get(i).toString().equals(tmpDevice.getSrcMACAddress().toString())
                                        && path.getPortList().get(i).toString().equals(tmpDevice.getSrcPort().toString()))
                                    index = tmpDeviceSet.indexOf(tmpDevice);
                            }
                            tmpDeviceSet.remove(index);


                        }

                        if (path.getSwitchList().get(i).toString().equals(switchDevice.getDestMACAddress().toString())
                                && path.getPortList().get(i).toString().equals(switchDevice.getDestPort().toString())) {
                            int index = 0;
                            for (Device tmpDevice :
                                    tmpDeviceSet) {
                                if (path.getSwitchList().get(i).toString().equals(tmpDevice.getDestMACAddress().toString())
                                        && path.getPortList().get(i).toString().equals(tmpDevice.getDestPort().toString()))
                                    index = tmpDeviceSet.indexOf(tmpDevice);
                            }
                            tmpDeviceSet.remove(index);


                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Path alternatePath = findShortestPathBwTwoHosts(srcMACAddress, destMACAddress, tmpDeviceSet, "AlternatePath");
        shortestPathList.add(path);
        shortestPathList.add(alternatePath);
        return shortestPathList;
    }

    private Path findShortestPathBwTwoHosts(MacAddress srcMACAddress, MacAddress destMACAddress, ArrayList<Device> deviceSet, String pathName) {

        nodeTable = new Hashtable<>();
        graphTable = new Hashtable<>();

        DatapathId sourceSwitch = null;
        DatapathId destinationSwitch = null;

        OFPort sourceHostPort = null;
        OFPort destinationHostPort = null;
        System.out.println(deviceSet);


        for (Device hostDevice :
                deviceSet) {
            if (hostDevice.getType().equals("Host")) {
                if ("00:00:".concat(srcMACAddress.toString()).equals(hostDevice.getSrcMACAddress().toString())) {
                    sourceSwitch = hostDevice.getDestMACAddress();
                    sourceHostPort = hostDevice.getSrcPort();
                }
                if ("00:00:".concat(destMACAddress.toString()).equals(hostDevice.getSrcMACAddress().toString())) {
                    destinationSwitch = hostDevice.getDestMACAddress();
                    destinationHostPort = hostDevice.getSrcPort();

                }
            }
        }// end - foreach

        for (Device device :
                deviceSet) {
            if (!device.getType().equals("Host")) {
                String switchId = device.getSrcMACAddress().toString();
                Node node = new Node(switchId);
                nodeTable.put(switchId, node);
            }
        }// end - foreach

        // Graph Olustur
        for (Device device :
                deviceSet) {
            if (!device.getType().equals("Host")) {
                String switchId = device.getSrcMACAddress().toString();
                String destSwitchId = device.getDestMACAddress().toString();
                nodeTable.get(switchId).addDestination(nodeTable.get(destSwitchId), 1);

            }
        }// end - foreach

        Graph graph = new Graph();
        Dijkstra dijkstra = new Dijkstra();

        Collection<Node> tmpNodes = nodeTable.values();
        for (Node node :
                tmpNodes) {

            graph.addNode(node);
        }

        for (Node node :
                tmpNodes) {
            if (node.getName().equals(sourceSwitch.toString()))
                graph = dijkstra.calculateShortestPathFromSource(graph, node);

        }
        // graph icinde secilen source dan digerlerine olan en kisa yollar mevcut
        // sadece port adresleri eksik.
        // port adresleride eklenip, geriye sadece source destination arasi yol dondurulebilir.
        // butun hepsine olan uzaklik degilde
        OFPort inPort = sourceHostPort;
        List<Switch> swOnPathList = new ArrayList<>();
        Path path = new Path();
        Node prevNode = null;
        for (Node node :
                graph.getNodes()) {
            if (node.getName().equals(destinationSwitch.toString())) {
                prevNode = node.getShortestPath().get(0);

                for (int i = 1; i < node.getShortestPath().size(); i++) {
                    Node sNode = node.getShortestPath().get(i);
                    for (Device switchDevice :
                            deviceSet) {

                        if (switchDevice.getType().equals("Switch")) {
                            if (prevNode.getName().equals(switchDevice.getSrcMACAddress().toString()) &&
                                    sNode.getName().equals(switchDevice.getDestMACAddress().toString())) {
                                path.getSwitchList().add(switchDevice.getSrcMACAddress());
                                path.getPortList().add(switchDevice.getSrcPort());
                                Switch swOnPath = new Switch(switchDevice.getSrcMACAddress(), switchDevice.getSrcPort(), inPort);
                                swOnPathList.add(swOnPath);
                                inPort = switchDevice.getDestPort();
                                prevNode = sNode;
                            }
                        }
                    }

                }
            }

        }
        for (Device switchDevice :
                deviceSet) {

            if (switchDevice.getType().equals("Switch")) {
                if (prevNode.getName().equals(switchDevice.getSrcMACAddress().toString()) &&
                        destinationSwitch.toString().equals(switchDevice.getDestMACAddress().toString())) {
                    path.getSwitchList().add(switchDevice.getSrcMACAddress());
                    path.getPortList().add(switchDevice.getSrcPort());
                    Switch swOnPath = new Switch(switchDevice.getSrcMACAddress(), switchDevice.getSrcPort(), inPort);
                    swOnPathList.add(swOnPath);
                    inPort = switchDevice.getDestPort();
                }
            }
        }
        path.getSwitchList().add(destinationSwitch);
        path.getPortList().add(destinationHostPort);
        Switch swOnPath = new Switch(destinationSwitch, destinationHostPort, inPort);
        swOnPathList.add(swOnPath);
        pathTable.put(pathName, swOnPathList);
        System.out.println(path);
        return path;

    }

    public Hashtable<String, List<Switch>> getPathTable() {
        return pathTable;
    }

    public void setPathTable(Hashtable<String, List<Switch>> pathTable) {
        this.pathTable = pathTable;
    }
}
