package controller;

import model.Node;
import model.Packet;
import mitigation.MitigationEngine;
import java.util.List;

public class SDNController {

    private MitigationEngine mitigationEngine;

    public SDNController() {
        this.mitigationEngine = new MitigationEngine();
    }

    // Accepts List of Nodes, suspicious IP list, and packets to simulate mitigation
    public void applyMitigation(List<Node> nodes, List<String> suspiciousIPs, List<Packet> packets) {
        System.out.println("\n--- SDN Controller Actions ---");
        System.out.println("Delegating mitigation to Mitigation Engine...");
        
        // Let MitigationEngine handle the actual logic
        mitigationEngine.applyMitigation(nodes, suspiciousIPs, packets);
    }
}
