package model;

public class Node {
    private String nodeId;
    private String ipAddress;

    public Node(String nodeId, String ipAddress) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
