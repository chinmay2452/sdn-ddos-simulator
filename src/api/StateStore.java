package api;

import model.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to hold the network state for the API server.
 */
public class StateStore {
    private static StateStore instance;
    private List<Node> nodes = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private int totalPackets = 0;
    private boolean attackDetected = false;

    private StateStore() {}

    public static synchronized StateStore getInstance() {
        if (instance == null) {
            instance = new StateStore();
        }
        return instance;
    }

    public synchronized void updateNodes(List<Node> newNodes) {
        this.nodes = new ArrayList<>(newNodes);
    }

    public synchronized void addLog(String log) {
        this.logs.add(log);
    }

    public synchronized void setTotalPackets(int total) {
        this.totalPackets = total;
    }

    public synchronized void setAttackDetected(boolean detected) {
        this.attackDetected = detected;
    }

    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalPackets\":").append(totalPackets).append(",");
        sb.append("\"attackDetected\":").append(attackDetected).append(",");
        
        // Nodes
        sb.append("\"nodes\":[");
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            sb.append("{")
              .append("\"id\":\"").append(n.getNodeId()).append("\",")
              .append("\"ip\":\"").append(n.getIpAddress()).append("\",")
              .append("\"blocked\":").append(n.isBlocked()).append(",")
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
