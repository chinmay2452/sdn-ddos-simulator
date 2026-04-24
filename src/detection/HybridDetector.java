package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.List;

public class HybridDetector extends AttackDetector {
    
    @Override
    public List<String> detectAttack(List<Packet> packets) {
        System.out.println("\n=== Running Hybrid Detection (Volume + Distributed) ===");
        
        AttackDetector threshold = new ThresholdDetector();
        List<String> thresholdIPs = threshold.detectAttack(packets);

        AttackDetector distributed = new DistributedDetector();
        List<String> distributedIPs = distributed.detectAttack(packets);

        System.out.println("\n--- Hybrid Detection Summary ---");
        List<String> combinedIPs = new ArrayList<>();

        // Add all unique suspicious IPs from both detectors
        for (String ip : thresholdIPs) {
            if (!combinedIPs.contains(ip)) combinedIPs.add(ip);
        }
        for (String ip : distributedIPs) {
            if (!combinedIPs.contains(ip)) combinedIPs.add(ip);
        }

        if (combinedIPs.isEmpty()) {
            System.out.println("Traffic appears safe based on hybrid analysis.");
        } else {
            System.out.println("Hybrid analysis identified " + combinedIPs.size() + " unique suspicious sources.");
            for (String ip : combinedIPs) {
                String level = (thresholdIPs.contains(ip) && distributedIPs.contains(ip)) ? "CRITICAL" : "WARNING";
                System.out.println("[" + level + "] Suspicious IP: " + ip);
            }
        }

        return combinedIPs;
    }
}
