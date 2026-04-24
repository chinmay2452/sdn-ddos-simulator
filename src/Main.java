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

        // Step 4: Choose Detector Strategy and Detect Attack
        System.out.println("Select Detection Strategy:");
        System.out.println("1. Threshold Detector (Volume)");
        System.out.println("2. Rate Detector (Burst/Window)");
        System.out.println("3. Hybrid Detector (Volume + Burst)");
        System.out.print("Enter choice (1-3): ");
        
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int choice = 1;
        if (scanner.hasNextLine()) {
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, defaulting to 1.");
            }
        }
        
        AttackDetector attackDetector;
        switch (choice) {
            case 2:
                attackDetector = new detection.RateDetector();
                break;
            case 3:
                attackDetector = new detection.HybridDetector();
                break;
            case 1:
            default:
                System.out.println("Using Default Threshold Detector.");
                attackDetector = new detection.ThresholdDetector();
                break;
        }
        
        List<String> suspiciousIPs = attackDetector.detectAttack(allPackets);
        
        // Step 5: Apply Mitigation (SDN Controller)
        SDNController sdnController = new SDNController();
        sdnController.applyMitigation(nodes, suspiciousIPs, allPackets);
    }
}