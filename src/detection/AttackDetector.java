package detection;

import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AttackDetector {
    private static final int THRESHOLD = 50; // Threshold for suspicious traffic

    // Analyzes generated packets and identifies potential DDoS attacks
    public void analyzeTraffic(ArrayList<Packet> packets) {
        HashMap<String, Integer> sourcePacketCounts = new HashMap<>();

        // Count number of packets from each source IP
        for (Packet packet : packets) {
            String sourceIP = packet.getSourceIP();
            sourcePacketCounts.put(sourceIP, sourcePacketCounts.getOrDefault(sourceIP, 0) + 1);
        }

        System.out.println("\n--- Detection Report ---");
        boolean attackDetected = false;

        // Print traffic report and check for threshold violations
        for (Map.Entry<String, Integer> entry : sourcePacketCounts.entrySet()) {
            String ip = entry.getKey();
            int count = entry.getValue();

            System.out.println(ip + " -> " + count + " packets");

            // If packet count crosses the threshold, mark as suspicious
            if (count > THRESHOLD) {
                System.out.println("\nALERT: Possible DDoS Attack from IP: " + ip);
                attackDetected = true;
            }
        }

        // If no suspicious traffic exists, print safe traffic message
        if (!attackDetected) {
            System.out.println("\nTraffic is safe. No suspicious patterns detected.");
        }
    }
}
