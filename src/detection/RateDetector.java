package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RateDetector extends AttackDetector {
    private static final int WINDOW_SIZE = 50;
    private static final int BURST_THRESHOLD = 35; // If > 35 packets in a window of 50 are from same IP

    @Override
    public List<String> detectAttack(List<Packet> packets) {
        List<String> suspiciousIPs = new ArrayList<>();
        System.out.println("\n--- Rate (Burst) Detection Report ---");

        boolean attackDetected = false;

        for (int i = 0; i <= packets.size() - WINDOW_SIZE; i += WINDOW_SIZE) {
            HashMap<String, Integer> windowCounts = new HashMap<>();
            
            // Count packets in current window
            int end = Math.min(i + WINDOW_SIZE, packets.size());
            for (int j = i; j < end; j++) {
                String ip = packets.get(j).getSourceIP();
                windowCounts.put(ip, windowCounts.getOrDefault(ip, 0) + 1);
            }

            for (String ip : windowCounts.keySet()) {
                int count = windowCounts.get(ip);
                if (count > BURST_THRESHOLD) {
                    if (!suspiciousIPs.contains(ip)) {
                        System.out.println("ALERT: Burst Attack Detected from IP: " + ip 
                            + " (" + count + " packets in a window of " + (end - i) + ")");
                        suspiciousIPs.add(ip);
                        attackDetected = true;
                    }
                }
            }
        }

        if (!attackDetected) {
            System.out.println("Traffic is safe. No burst patterns detected.");
        }

        return suspiciousIPs;
    }
}
