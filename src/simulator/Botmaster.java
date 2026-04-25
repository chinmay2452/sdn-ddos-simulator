package simulator;

import model.Node;
import model.Packet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Botmaster {
    private Random random = new Random();

    public List<Packet> launchAttack(List<Node> bots, Node victim) {
        List<Packet> attackPackets = new ArrayList<>();
        api.StateStore.getInstance().addLog("\n[Botmaster] Launching coordinated DDoS attack on " + victim.getIpAddress());
        
        for (Node bot : bots) {
            // Each bot sends a medium number of packets (e.g., 30-60)
            int botPacketCount = 30 + random.nextInt(31);
            for (int i = 0; i < botPacketCount; i++) {
                int packetSize = 500 + random.nextInt(1001);
                bot.incrementPacketsSent();
                attackPackets.add(new Packet(bot.getIpAddress(), victim.getIpAddress(), packetSize));
            }
            api.StateStore.getInstance().addLog("Bot " + bot.getIpAddress() + " is flooding " + victim.getIpAddress() + " with " + botPacketCount + " packets.");
        }
        
        return attackPackets;
    }
}
