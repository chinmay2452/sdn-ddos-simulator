import model.Node;
import model.Packet;
import simulator.TrafficGenerator;
import analyzer.TrafficAnalyzer;
import detection.AttackDetector;
import controller.SDNController;
import api.DashboardServer;
import api.StateStore;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Start API Server for Dashboard
        DashboardServer.start(8081);
        StateStore state = StateStore.getInstance();
        state.addLog("SDN Simulation Started");

        // Step 1: Create Network Nodes
        List<Node> nodes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            nodes.add(new Node("N" + i, "192.168.1." + i));
        }
        state.updateNodes(nodes);
        state.addLog("Created 10 Network Nodes");

        // Step 2: Generate Traffic
        TrafficGenerator trafficGenerator = new TrafficGenerator();
        ArrayList<Packet> normalTraffic = trafficGenerator.generateNormalTraffic(nodes, 50);
        ArrayList<Packet> suspiciousTraffic = trafficGenerator.generateSuspiciousTraffic(nodes, 200);

        ArrayList<Packet> allPackets = new ArrayList<>();
        allPackets.addAll(normalTraffic);
        allPackets.addAll(suspiciousTraffic);
        
        state.setTotalPackets(allPackets.size());
        state.addLog("Traffic Generated: " + allPackets.size() + " packets");

        // Initial Output
        System.out.println("Nodes Created: " + nodes.size());
        System.out.println("Packets Generated: " + allPackets.size());
        System.out.println();

        // Step 3: Analyze Traffic
        TrafficAnalyzer trafficAnalyzer = new TrafficAnalyzer();
        trafficAnalyzer.analyzeTraffic(allPackets);
        trafficAnalyzer.printReport();
        
        state.addLog("Traffic analysis complete");

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
                state.addLog("Using Rate-based Detector");
                break;
            case 3:
                attackDetector = new detection.HybridDetector();
                state.addLog("Using Hybrid Detector");
                break;
            case 1:
            default:
                System.out.println("Using Default Threshold Detector.");
                attackDetector = new detection.ThresholdDetector();
                state.addLog("Using Threshold Detector");
                break;
        }
        
        List<String> suspiciousIPs = attackDetector.detectAttack(allPackets);
        if (!suspiciousIPs.isEmpty()) {
            state.setAttackDetected(true);
            state.addLog("ATTACK DETECTED from: " + suspiciousIPs);
        }
        
        // Step 5: Apply Mitigation (SDN Controller)
        SDNController sdnController = new SDNController();
        sdnController.applyMitigation(nodes, suspiciousIPs, allPackets);
        
        state.updateNodes(nodes);
        state.addLog("Mitigation applied. Blocked malicious nodes.");
        
        System.out.println("\n--- Dashboard is active at http://localhost:5173 ---");
        System.out.println("--- API is active at http://localhost:8081/api/network ---");
        System.out.println("Keep this process running to view real-time data.");
    }
}