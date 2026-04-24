package model;

public class VictimNode extends Node {
    public VictimNode(String nodeId, String ipAddress) {
        super(nodeId, ipAddress);
        setRole("SERVER");
    }
}
