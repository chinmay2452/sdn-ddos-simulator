package api;

import model.Node;
import model.Packet;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to hold the network state for the API server and UI.
 */
public class StateStore {
    private static StateStore instance;
    private List<Node> nodes = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private List<Packet> currentTraffic = new ArrayList<>();
    private int totalPackets = 0;
    private boolean attackDetected = false;
    private String victimIP = "None";
    private List<StateChangeListener> listeners = new ArrayList<>();

    public interface StateChangeListener {
        void onStateChange();
    }

    private StateStore() {}

    public static synchronized StateStore getInstance() {
        if (instance == null) {
            instance = new StateStore();
        }
        return instance;
    }

    public synchronized void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (StateChangeListener listener : listeners) {
            listener.onStateChange();
        }
    }

    public synchronized void updateNodes(List<Node> newNodes) {
        this.nodes = new ArrayList<>(newNodes);
        notifyListeners();
    }

    public synchronized void addLog(String log) {
        this.logs.add(log);
        notifyListeners();
    }

    public synchronized void setTotalPackets(int total) {
        this.totalPackets = total;
        notifyListeners();
    }

    public synchronized void setAttackDetected(boolean detected) {
        this.attackDetected = detected;
        notifyListeners();
    }

    public synchronized void setVictimIP(String victimIP) {
        this.victimIP = victimIP;
        notifyListeners();
    }

    public synchronized void setCurrentTraffic(List<Packet> packets) {
        this.currentTraffic = new ArrayList<>(packets);
        notifyListeners();
    }

    public List<Node> getNodes() { return nodes; }
    public List<String> getLogs() { return logs; }
    public List<Packet> getCurrentTraffic() { return currentTraffic; }
    public int getTotalPackets() { return totalPackets; }
    public boolean isAttackDetected() { return attackDetected; }
    public String getVictimIP() { return victimIP; }

    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalPackets\":").append(totalPackets).append(",");
        sb.append("\"attackDetected\":").append(attackDetected).append(",");
        sb.append("\"victimIP\":\"").append(victimIP).append("\",");
        
        // Nodes
        sb.append("\"nodes\":[");
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            sb.append("{")
              .append("\"id\":\"").append(n.getNodeId()).append("\",")
              .append("\"ip\":\"").append(n.getIpAddress()).append("\",")
              .append("\"blocked\":").append(n.isBlocked()).append(",")
              .append("\"role\":\"").append(n.getRole()).append("\",")
              .append("\"packets\":").append(n.getPacketsSent())
              .append("}");
            if (i < nodes.size() - 1) sb.append(",");
        }
        sb.append("],");

        // Logs
        sb.append("\"logs\":[");
        for (int i = 0; i < logs.size(); i++) {
            sb.append("\"").append(logs.get(i).replace("\"", "\\\"")).append("\"");
            if (i < logs.size() - 1) sb.append(",");
        }
        sb.append("]");
        
        sb.append("}");
        return sb.toString();
    }
}
