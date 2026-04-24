package detection;

import model.Packet;
import java.util.List;

public abstract class AttackDetector {
    /**
     * Analyzes generated packets and identifies potential DDoS attacks.
     * @param packets List of packets to analyze.
     * @return List of suspicious IP addresses.
     */
    public abstract List<String> detectAttack(List<Packet> packets);
}
