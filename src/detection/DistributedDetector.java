package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributedDetector extends AttackDetector {
    private static final int AGGREGATE_THRESHOLD = 150; // Total packets to one destination

    @Override
    public List<String> detectAttack(List<Packet> packets) {
        Map<String, Integer> destCounts = new HashMap<>();
        Map<String, List<String>> destSources = new HashMap<>();
        List<String> suspiciousIPs = new ArrayList<>();

        for (Packet packet : packets) {
            String dest = packet.getDestinationIP();
            String src = packet.getSourceIP();
            
            destCounts.put(dest, destCounts.getOrDefault(dest, 0) + 1);
            
            destSources.putIfAbsent(dest, new ArrayList<>());
            if (!destSources.get(dest).contains(src)) {
                destSources.get(dest).add(src);
            }
        }

        api.StateStore.getInstance().addLog("\n--- Distributed DDoS Detection Report ---");
        boolean distributedAttack = false;

        for (Map.Entry<String, Integer> entry : destCounts.entrySet()) {
            String victimIP = entry.getKey();
            int totalPackets = entry.getValue();
            List<String> sources = destSources.get(victimIP);

            if (totalPackets > AGGREGATE_THRESHOLD && sources.size() >= 3) {
                api.StateStore.getInstance().addLog("ALERT: Distributed DDoS Attack Detected!");
                api.StateStore.getInstance().addLog("Victim IP: " + victimIP);
                api.StateStore.getInstance().addLog("Total Incoming Packets: " + totalPackets);
                api.StateStore.getInstance().addLog("Attack Sources (" + sources.size() + " bots):");
                for (String src : sources) {
                    api.StateStore.getInstance().addLog(" - " + src);
                    if (!suspiciousIPs.contains(src)) {
                        suspiciousIPs.add(src);
                    }
                }
                distributedAttack = true;
            }
        }

        if (!distributedAttack) {
            api.StateStore.getInstance().addLog("No distributed attack patterns detected.");
        }

        return suspiciousIPs;
    }
}
