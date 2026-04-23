import model.Node;
import model.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {

        // Step 1: Create Nodes
        List<Node> nodes = new ArrayList<>();

        for(int i = 1; i <= 10; i++){
            nodes.add(new Node("N" + i, "192.168.1." + i));
        }

        System.out.println("Total Nodes Created: " + nodes.size());

        // Step 2: Generate Traffic using TrafficGenerator
        simulator.TrafficGenerator generator = new simulator.TrafficGenerator();
        ArrayList<Packet> allTraffic = generator.generateAllTraffic(nodes);

        // Step 3: Print total generated packets
        System.out.println("\nNodes Created: " + nodes.size());
        System.out.println("Packets Generated: " + allTraffic.size());

        // Step 4: Detect Attacks using AttackDetector
        detection.AttackDetector detector = new detection.AttackDetector();
        detector.analyzeTraffic(allTraffic);
    }
}