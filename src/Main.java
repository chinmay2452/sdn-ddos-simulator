import model.Node;
import model.Packet;
import simulator.TrafficGenerator;
import analyzer.TrafficAnalyzer;
import detection.AttackDetector;
import controller.SDNController;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Step 1: Create Network Nodes
        List<Node> nodes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            nodes.add(new Node("N" + i, "192.168.1." + i));
        }

        // Step 2: Generate Traffic
        TrafficGenerator trafficGenerator = new TrafficGenerator();
        ArrayList<Packet> normalTraffic = trafficGenerator.generateNormalTraffic(nodes, 50);
        ArrayList<Packet> suspiciousTraffic = trafficGenerator.generateSuspiciousTraffic(nodes, 200);

        ArrayList<Packet> allPackets = new ArrayList<>();
        allPackets.addAll(normalTraffic);
        allPackets.addAll(suspiciousTraffic);

        // Initial Output
        System.out.println("Nodes Created: " + nodes.size());
        System.out.println("Packets Generated: " + allPackets.size());
        System.out.println();

        // Step 3: Analyze Traffic
        TrafficAnalyzer trafficAnalyzer = new TrafficAnalyzer();
        trafficAnalyzer.analyzeTraffic(allPackets);
        trafficAnalyzer.printReport();
        trafficAnalyzer.getMostActiveIP();

        // Step 4: Detect Attack
        AttackDetector attackDetector = new AttackDetector();
        List<String> suspiciousIPs = attackDetector.analyzeTraffic(allPackets);
        
        // Step 5: Apply Mitigation (SDN Controller)
        SDNController sdnController = new SDNController();
        sdnController.applyMitigation(nodes, suspiciousIPs, allPackets);
    }
}