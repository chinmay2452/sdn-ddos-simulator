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
        // Start API Server for Dashboard (Optional but kept for compatibility)
        try {
            DashboardServer.start(8081);
        } catch (Exception e) {
            System.out.println("Could not start API server: " + e.getMessage());
        }
        
        StateStore state = StateStore.getInstance();
        state.addLog("SDN Simulation Started");

        // Step 1: Create Network Nodes
        List<Node> nodes = new ArrayList<>();
        // Designate some roles for better visualization
        for (int i = 1; i <= 10; i++) {
            Node node = new Node("N" + i, "192.168.1." + i);
            if (i == 1) {
                node.setRole("SERVER"); // Victim candidate
            } else if (i == 2) {
                node.setRole("BOTMASTER");
            } else if (i >= 3 && i <= 6) {
                node.setRole("BOT");
            }
            nodes.add(node);
        }
        state.updateNodes(nodes);
        state.addLog("Network Initialized: 10 Nodes, 1 Server, 1 Botmaster, 4 Bots");

        // Step 2: Launch the Visualizer UI
        System.out.println("Launching SDN Security Dashboard...");
        javax.swing.SwingUtilities.invokeLater(() -> {
            new ui.NetworkVisualizer();
        });

        System.out.println("\n--- Dashboard is active (Java Swing) ---");
        System.out.println("--- API is active at http://localhost:8081/api/network ---");
    }
}