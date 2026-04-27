package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RateDetector extends AttackDetector {
    // Minimum packets from a single IP across the entire traffic sample to be suspicious
    private static final int TOTAL_RATE_THRESHOLD = 20;
    // Within any window, if an IP exceeds this fraction it is also flagged
    private static final int WINDOW_SIZE = 50;
    private static final int BURST_THRESHOLD = 10; // >10 of 50 packets from same IP in one window

    @Override
    public List<String> detectAttack(List<Packet> packets) {
        List<String> suspiciousIPs = new ArrayList<>();
        api.StateStore.getInstance().addLog("\n--- Rate (Burst) Detection Report ---");

        // Phase 1: Accumulate total counts per IP across ALL packets
        HashMap<String, Integer> totalCounts = new HashMap<>();
        for (Packet p : packets) {
            String ip = p.getSourceIP();
            totalCounts.put(ip, totalCounts.getOrDefault(ip, 0) + 1);
        }

        // Flag IPs whose total volume already exceeds the rate threshold
        for (String ip : totalCounts.keySet()) {
            int total = totalCounts.get(ip);
            if (total >= TOTAL_RATE_THRESHOLD && !suspiciousIPs.contains(ip)) {
                api.StateStore.getInstance().addLog("ALERT: High-rate attack detected from IP: " + ip
                    + " (" + total + " total packets, threshold=" + TOTAL_RATE_THRESHOLD + ")");
                suspiciousIPs.add(ip);
            }
        }

        // Phase 2: Sliding-window burst check (catches short, intense bursts)
        for (int i = 0; i <= packets.size() - WINDOW_SIZE; i += WINDOW_SIZE) {
            HashMap<String, Integer> windowCounts = new HashMap<>();
            int end = Math.min(i + WINDOW_SIZE, packets.size());
            for (int j = i; j < end; j++) {
                String ip = packets.get(j).getSourceIP();
                windowCounts.put(ip, windowCounts.getOrDefault(ip, 0) + 1);
            }
            for (String ip : windowCounts.keySet()) {
                int count = windowCounts.get(ip);
                if (count > BURST_THRESHOLD && !suspiciousIPs.contains(ip)) {
                    api.StateStore.getInstance().addLog("ALERT: Burst Attack Detected from IP: " + ip
                        + " (" + count + " packets in window of " + (end - i) + ")");
                    suspiciousIPs.add(ip);
                }
            }
        }

        if (suspiciousIPs.isEmpty()) {
            api.StateStore.getInstance().addLog("Traffic is safe. No burst patterns detected.");
        }

        return suspiciousIPs;
    }
}
