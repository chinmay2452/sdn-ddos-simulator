package model;

public class Flow {
    private String sourceIP;
    private int packetCount;

    public Flow(String sourceIP) {
        this.sourceIP = sourceIP;
        this.packetCount = 0;
    }

    public void incrementPackets() {
        packetCount++;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public String getSourceIP() {
        return sourceIP;
    }
}
