package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThresholdDetector extends AttackDetector {
    private static final int THRESHOLD = 50; // Threshold for suspicious traffic

    @Override
    public List<String> detectAttack(List<Packet> packets) {
        HashMap<String, Integer> sourcePacketCounts = new HashMap<>();
        List<String> suspiciousIPs = new ArrayList<>();

        // Count number of packets from each source IP
        for (Packet packet : packets) {
            String sourceIP = packet.getSourceIP();
            sourcePacketCounts.put(sourceIP, sourcePacketCounts.getOrDefault(sourceIP, 0) + 1);
        }

        api.StateStore.getInstance().addLog("\n--- Threshold Detection Report ---");
        boolean attackDetected = false;

        for (Map.Entry<String, Integer> entry : sourcePacketCounts.entrySet()) {
            String ip = entry.getKey();
            int count = entry.getValue();

            api.StateStore.getInstance().addLog(ip + " -> " + count + " packets (Total)");

            if (count > THRESHOLD) {
                api.StateStore.getInstance().addLog("ALERT: Possible DDoS Attack from IP: " + ip + " (Exceeded threshold of " + THRESHOLD + ")");
                suspiciousIPs.add(ip);
                attackDetected = true;
            }
        }

        if (!attackDetected) {
            api.StateStore.getInstance().addLog("Traffic is safe. No suspicious volume detected.");
        }
        
        return suspiciousIPs;
    }
}
