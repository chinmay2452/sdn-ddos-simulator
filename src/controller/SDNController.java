package controller;

import model.Node;
import model.Packet;
import java.util.List;

public class SDNController {

    // Accepts List of Nodes, suspicious IP list, and packets to simulate mitigation
    public void applyMitigation(List<Node> nodes, List<String> suspiciousIPs, List<Packet> packets) {
        System.out.println("\n--- SDN Controller Actions ---");

        if (suspiciousIPs == null || suspiciousIPs.isEmpty()) {
            System.out.println("No suspicious nodes detected. No mitigation required.");
            return;
        }

        // 2. Block Suspicious Nodes
        for (String ip : suspiciousIPs) {
            System.out.println("Blocking IP: " + ip);
            Node attackerNode = getNodeByIp(nodes, ip);
            if (attackerNode != null) {
                attackerNode.setBlocked(true);
                attackerNode.setRole("ATTACKER");
            }
        }
        
        System.out.println("Dropping malicious packets...");

        int droppedCount = 0;
        int reroutedCount = 0;

        for (Packet packet : packets) {
            String srcIp = packet.getSourceIP();
            String destIp = packet.getDestinationIP();

            Node srcNode = getNodeByIp(nodes, srcIp);
            Node destNode = getNodeByIp(nodes, destIp);

            // 3. Drop Packets from Blocked Nodes
            if (srcNode != null && srcNode.isBlocked()) {
                System.out.println("Packet dropped from blocked IP");
                droppedCount++;
                continue; // Ignore packet
            }

            // 4. Simulate Rerouting
            if (destNode != null && destNode.isBlocked()) {
                Node safeNode = getSafeNode(nodes);
                if (safeNode != null) {
                    System.out.println("Traffic rerouted from " + destIp + " to " + safeNode.getIpAddress());
                    reroutedCount++;
                }
            }
        }
        
        if (reroutedCount > 0) {
            System.out.println("Traffic rerouted to safe nodes");
        }
    }

    private Node getNodeByIp(List<Node> nodes, String ip) {
        for (Node node : nodes) {
            if (node.getIpAddress().equals(ip)) {
                return node;
            }
        }
        return null;
    }

    private Node getSafeNode(List<Node> nodes) {
        for (Node node : nodes) {
            if (!node.isBlocked()) {
                return node;
            }
        }
        return null;
    }
}
