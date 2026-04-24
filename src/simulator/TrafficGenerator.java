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
            
            // Increment packet count for source node
            source.incrementPacketsSent();
            
            packets.add(new Packet(source.getIpAddress(), destination.getIpAddress(), packetSize));
        }
        
        return packets;
    }

    // Generates suspicious traffic (Distributed DDoS Simulation)
    public ArrayList<Packet> generateSuspiciousTraffic(List<Node> nodes, int count) {
        ArrayList<Packet> packets = new ArrayList<>();
        
        if (nodes.size() < 6) {
            System.out.println("Not enough nodes to simulate a botnet. Need at least 6 nodes.");
            return packets;
        }

        // Select 1 Victim Node
        Node victim = nodes.get(random.nextInt(nodes.size()));
        
        // Select 3-5 Bots (excluding the victim)
        int botCount = 3 + random.nextInt(3); // 3, 4, or 5
        List<Node> bots = new ArrayList<>();
        List<Node> availableNodes = new ArrayList<>(nodes);
        availableNodes.remove(victim);

        for (int i = 0; i < botCount; i++) {
            Node bot = availableNodes.remove(random.nextInt(availableNodes.size()));
            bots.add(bot);
        }

        // Use Botmaster to launch the attack
        Botmaster botmaster = new Botmaster();
        packets.addAll(botmaster.launchAttack(bots, victim));
        
        return packets;
    }

    // Generates a mix of normal and suspicious traffic
    public ArrayList<Packet> generateAllTraffic(List<Node> nodes) {
        int normalCount = 50; 
        
        ArrayList<Packet> normalPackets = generateNormalTraffic(nodes, normalCount);
        ArrayList<Packet> suspiciousPackets = generateSuspiciousTraffic(nodes, normalCount); // count parameter not strictly used in the new botnet logic
        
        ArrayList<Packet> allPackets = new ArrayList<>();
        allPackets.addAll(normalPackets);
        allPackets.addAll(suspiciousPackets);
        
        // Identify the victim IP
        String victimIp = "N/A";
        if (!suspiciousPackets.isEmpty()) {
            victimIp = suspiciousPackets.get(0).getDestinationIP();
        }
        
        // Print generated traffic summary
        System.out.println("\n--- Traffic Generation Summary ---");
        System.out.println("Normal Packets Generated: " + normalCount);
        System.out.println("DDoS Packets Generated: " + suspiciousPackets.size());
        System.out.println("Total Packets Generated: " + allPackets.size());
        System.out.println("Victim IP: " + victimIp);
        System.out.println("----------------------------------");
        
        return allPackets;
    }
}
