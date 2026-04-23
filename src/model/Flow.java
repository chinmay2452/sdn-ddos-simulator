// Flow.java
package model;

public class Flow {
    private String sourceIP;
    private int packetCount;
    private int totalBytes;
    private long startTime;
    private long lastUpdated;

    public Flow(String sourceIP) {
        this.sourceIP = sourceIP;
        this.packetCount = 0;
        this.totalBytes = 0;
        this.startTime = System.currentTimeMillis();
        this.lastUpdated = startTime;
    }

    public void addPacket(int size) {
        packetCount++;
        totalBytes += size;
        lastUpdated = System.currentTimeMillis();
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public double getPacketsPerSecond() {
        long duration = lastUpdated - startTime;

        if (duration == 0) {
            return packetCount;
        }

        return (packetCount * 1000.0) / duration;
    }

    @Override
    public String toString() {
        return "Flow | Source: " + sourceIP +
                " | Packets: " + packetCount +
                " | Bytes: " + totalBytes +
                " | Rate: " + String.format("%.2f", getPacketsPerSecond()) +
                " pkt/sec";
    }
}