package model;

public class Packet {
    private String sourceIP;
    private String destinationIP;
    private int size;

    public Packet(String sourceIP, String destinationIP, int size) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.size = size;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public String getDestinationIP() {
        return destinationIP;
    }

    public int getSize() {
        return size;
    }
}
