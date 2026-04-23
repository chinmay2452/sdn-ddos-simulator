package simulator;

import model.Node;
import model.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrafficGenerator {
    private Random random = new Random();

    // Generates normal network traffic
    public ArrayList<Packet> generateNormalTraffic(List<Node> nodes, int count) {
        ArrayList<Packet> packets = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Randomly select source and destination nodes
            Node source = nodes.get(random.nextInt(nodes.size()));
            Node destination = nodes.get(random.nextInt(nodes.size()));
            
            // Ensure source and destination are not the same
            while (source.getIpAddress().equals(destination.getIpAddress())) {
                destination = nodes.get(random.nextInt(nodes.size()));
            }
            
            // Generate realistic packet sizes (100 to 500 bytes)
            int packetSize = 100 + random.nextInt(401); // 100 to 500
            
            packets.add(new Packet(source.getIpAddress(), destination.getIpAddress(), packetSize));
        }
        
        return packets;
    }

    // Generates suspicious traffic (DDoS Simulation)
    public ArrayList<Packet> generateSuspiciousTraffic(List<Node> nodes, int count) {
        ArrayList<Packet> packets = new ArrayList<>();
        
        // Select ONE attacker node
        Node attacker = nodes.get(random.nextInt(nodes.size()));
        
        for (int i = 0; i < count; i++) {
            // Send packets rapidly to multiple destinations
            Node destination = nodes.get(random.nextInt(nodes.size()));
            
            // Ensure attacker is not sending to itself
            while (attacker.getIpAddress().equals(destination.getIpAddress())) {
                destination = nodes.get(random.nextInt(nodes.size()));
            }
            
            // Packet sizes can be 500 to 1500 bytes
            int packetSize = 500 + random.nextInt(1001); // 500 to 1500
            
            packets.add(new Packet(attacker.getIpAddress(), destination.getIpAddress(), packetSize));
        }
        
        return packets;
    }

    // Generates a mix of normal and suspicious traffic
    public ArrayList<Packet> generateAllTraffic(List<Node> nodes) {
        int normalCount = 50; // Example count for normal packets
        int suspiciousCount = 200; // Example count for suspicious packets
        
        ArrayList<Packet> normalPackets = generateNormalTraffic(nodes, normalCount);
        ArrayList<Packet> suspiciousPackets = generateSuspiciousTraffic(nodes, suspiciousCount);
        
        ArrayList<Packet> allPackets = new ArrayList<>();
        allPackets.addAll(normalPackets);
        allPackets.addAll(suspiciousPackets);
        
        // Identify the attacker IP
        String attackerIp = "N/A";
        if (!suspiciousPackets.isEmpty()) {
            attackerIp = suspiciousPackets.get(0).getSourceIP();
        }
        
        // Print generated traffic summary
        System.out.println("--- Traffic Generation Summary ---");
        System.out.println("Normal Packets Generated: " + normalCount);
        System.out.println("Suspicious Packets Generated: " + suspiciousCount);
        System.out.println("Total Packets Generated: " + allPackets.size());
        System.out.println("Attacker IP: " + attackerIp);
        System.out.println("----------------------------------");
        
        return allPackets;
    }
}
