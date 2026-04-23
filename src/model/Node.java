// Node.java
package model;

public class Node {
    private String nodeId;
    private String ipAddress;
    private boolean blocked;
    private String role;   // NORMAL / ATTACKER / SERVER
    private int packetsSent;

    public Node(String nodeId, String ipAddress) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.blocked = false;
        this.role = "NORMAL";
        this.packetsSent = 0;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public void incrementPacketsSent() {
        packetsSent++;
    }

    @Override
    public String toString() {
        return nodeId +
                " | IP: " + ipAddress +
                " | Role: " + role +
                " | Blocked: " + blocked;
    }
}