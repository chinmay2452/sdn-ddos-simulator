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

        // Step 2: Generate Packets
        List<Packet> packets = new ArrayList<>();
        Random random = new Random();

        for(int i = 0; i < 20; i++){
            Node source = nodes.get(random.nextInt(nodes.size()));
            Node destination = nodes.get(random.nextInt(nodes.size()));

            Packet packet = new Packet(
                    source.getIpAddress(),
                    destination.getIpAddress(),
                    random.nextInt(1000)
            );

            packets.add(packet);
        }

        // Step 3: Print Packets
        System.out.println("\nGenerated Packets:");
        for(Packet p : packets){
            System.out.println(
                    "Packet: " + p.getSourceIP() +
                            " -> " + p.getDestinationIP() +
                            " Size: " + p.getSize()
            );
        }
    }
}