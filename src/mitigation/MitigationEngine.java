package mitigation;

import api.FlowEntry;
import api.StateStore;
import model.Node;
import model.Packet;
import java.util.*;

public class MitigationEngine {

    public void applyMitigation(List<Node> nodes, List<String> suspiciousIPs, List<Packet> packets) {
        api.StateStore.getInstance().addLog("\n--- Mitigation Engine Actions ---");

        if (suspiciousIPs == null || suspiciousIPs.isEmpty()) {
            api.StateStore.getInstance().addLog("No suspicious nodes detected. No mitigation required.");
            return;
        }

        api.StateStore.getInstance().addLog("ALERT: Distributed DDoS detected from multiple sources");
        api.StateStore.getInstance().addLog("Blocking bot network...");

        // 1. Block Suspicious Nodes and identify the Victim
        String victimIP = identifyVictim(packets, suspiciousIPs);
        for (String ip : suspiciousIPs) {
            StateStore.getInstance().addLog("Blocking bot IP: " + ip);
            // Push DROP flow rule into the SDN flow table
            StateStore.getInstance().addFlowEntry(FlowEntry.dropBot(ip));
            // Set bandwidth to 0 (blocked)
            StateStore.getInstance().updateBandwidth(ip, 0);
            Node attackerNode = getNodeByIp(nodes, ip);
            if (attackerNode != null) {
                attackerNode.setBlocked(true);
                attackerNode.setRole("BOT");
            }
        }
        // Push rate-limit rule for the victim
        if (!victimIP.equals("None")) {
            StateStore.getInstance().addFlowEntry(FlowEntry.rateLimitVictim(victimIP));
        }

        // 2. Create Safe Node Pool (Exclude bots and victim)
        List<String> safeNodeIPs = new ArrayList<>();
        Map<String, Integer> nodeLoad = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isBlocked() && !node.getIpAddress().equals(victimIP)) {
                safeNodeIPs.add(node.getIpAddress());
                nodeLoad.put(node.getIpAddress(), 0);
            }
        }

        if (safeNodeIPs.isEmpty()) {
            api.StateStore.getInstance().addLog("No safe nodes available for rerouting. All malicious packets will be dropped.");
        }

        api.StateStore.getInstance().addLog("Applying intelligent traffic management...");

        int droppedCount = 0;
        int reroutedCount = 0;
        Random random = new Random();

        for (Packet packet : packets) {
            String srcIp = packet.getSourceIP();

            // Check if packet is from a blocked node (attacker)
            Node srcNode = getNodeByIp(nodes, srcIp);
            if (srcNode != null && srcNode.isBlocked()) {
                
                // Partial Rerouting Policy: 70% drop, 30% reroute (to honey pots or safe nodes for analysis)
                if (random.nextDouble() < 0.3 && !safeNodeIPs.isEmpty()) {
                    // Smart Routing: Select least-loaded node
                    String leastLoadedNode = getLeastLoadedNode(nodeLoad);
                    
                    // Reroute the packet
                    packet.setDestinationIP(leastLoadedNode);
                    StateStore.getInstance().addLog("Traffic rerouted from " + srcIp + " -> " + leastLoadedNode + " (load-balanced)");
                    
                    // Push reroute flow rule (only once per source)
                    if (nodeLoad.get(leastLoadedNode) == 0) {
                        StateStore.getInstance().addFlowEntry(FlowEntry.rerouteTraffic(victimIP, leastLoadedNode));
                    }
                    // Update load
                    nodeLoad.put(leastLoadedNode, nodeLoad.get(leastLoadedNode) + 1);
                    reroutedCount++;
                } else {
                    // Drop packet
                    droppedCount++;
                }
            }
        }

        StateStore.getInstance().addLog("\n--- Mitigation Summary ---");
        StateStore.getInstance().addLog("Total packets dropped: " + droppedCount);
        StateStore.getInstance().addLog("Total packets rerouted: " + reroutedCount);
        if (reroutedCount > 0) {
            StateStore.getInstance().addLog("Rerouting Distribution:");
            for (Map.Entry<String, Integer> entry : nodeLoad.entrySet()) {
                if (entry.getValue() > 0) {
                    StateStore.getInstance().addLog(" - " + entry.getKey() + ": " + entry.getValue() + " packets");
                }
            }
        }
        // Update victim bandwidth back to normal after mitigation
        if (!victimIP.equals("None")) {
            StateStore.getInstance().updateBandwidth(victimIP, 5 + new Random().nextInt(10));
        }
    }

    private String identifyVictim(List<Packet> packets, List<String> suspiciousIPs) {
        Map<String, Integer> destCounts = new HashMap<>();
        for (Packet p : packets) {
            if (suspiciousIPs.contains(p.getSourceIP())) {
                destCounts.put(p.getDestinationIP(), destCounts.getOrDefault(p.getDestinationIP(), 0) + 1);
            }
        }
        
        String victim = "None";
        int max = 0;
        for (Map.Entry<String, Integer> entry : destCounts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                victim = entry.getKey();
            }
        }
        return victim;
    }

    private String getLeastLoadedNode(Map<String, Integer> nodeLoad) {
        return Collections.min(nodeLoad.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Node getNodeByIp(List<Node> nodes, String ip) {
        for (Node node : nodes) {
            if (node.getIpAddress().equals(ip)) {
                return node;
            }
        }
        return null;
    }
}
