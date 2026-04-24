package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.List;

public class HybridDetector extends AttackDetector {
    
    @Override
    public List<String> detectAttack(List<Packet> packets) {
        System.out.println("\n=== Running Hybrid Detection ===");
        
        AttackDetector threshold = new ThresholdDetector();
        List<String> thresholdIPs = threshold.detectAttack(packets);

        AttackDetector rate = new RateDetector();
        List<String> rateIPs = rate.detectAttack(packets);

        System.out.println("\n--- Hybrid Detection Summary ---");
        List<String> combinedIPs = new ArrayList<>();

        for (String ip : thresholdIPs) {
            if (rateIPs.contains(ip)) {
                System.out.println("CRITICAL ALERT: IP " + ip + " failed both Volume and Burst checks!");
                if (!combinedIPs.contains(ip)) combinedIPs.add(ip);
            } else {
                System.out.println("WARNING: IP " + ip + " failed Volume check only.");
                if (!combinedIPs.contains(ip)) combinedIPs.add(ip);
            }
        }

        for (String ip : rateIPs) {
            if (!thresholdIPs.contains(ip)) {
                System.out.println("WARNING: IP " + ip + " failed Burst check only.");
                if (!combinedIPs.contains(ip)) combinedIPs.add(ip);
            }
        }
        
        if (combinedIPs.isEmpty()) {
            System.out.println("Traffic is perfectly safe.");
        }

        return combinedIPs;
    }
}
