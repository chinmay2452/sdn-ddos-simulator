// Packet.java
package model;

public class Packet {
    private static int counter = 1;

    private int packetId;
    private String sourceIP;
    private String destinationIP;
    private int size;
    private long timestamp;
    private String protocol;

    public Packet(String sourceIP, String destinationIP, int size) {
        this.packetId = counter++;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
        this.protocol = "TCP";
    }

    public int getPacketId() {
        return packetId;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public String getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(String destinationIP) {
        this.destinationIP = destinationIP;
    }

    public int getSize() {
        return size;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return "Packet #" + packetId +
                " | " + sourceIP +
                " -> " + destinationIP +
                " | Size: " + size +
                " bytes | " + protocol;
    }
}