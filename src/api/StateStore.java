package api;

import model.Node;
import model.Packet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class to hold the full network state for the API server and UI.
 * Acts as the central data bus using the Observer pattern.
 */
public class StateStore {
    private static StateStore instance;

    // --- Core State ---
    private List<Node> nodes = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private List<Packet> currentTraffic = new ArrayList<>();
    private int totalPackets = 0;
    private boolean attackDetected = false;
    private String victimIP = "None";

    // --- New: Flow Table State ---
    private List<FlowEntry> flowTable = new ArrayList<>();

    // --- New: Bandwidth State (IP -> percentage 0-100) ---
    private Map<String, Integer> nodeBandwidth = new HashMap<>();

    // --- Observer pattern ---
    private List<StateChangeListener> listeners = new ArrayList<>();

    public interface StateChangeListener {
        void onStateChange();
    }

    private StateStore() {
        // Install the default allow-all rule at startup
        flowTable.add(FlowEntry.defaultForward());
    }

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
        for (StateChangeListener l : listeners) l.onStateChange();
    }

    // ---- Node State ----
    public synchronized void updateNodes(List<Node> newNodes) {
        this.nodes = new ArrayList<>(newNodes);
        notifyListeners();
    }

    // ---- Log State ----
    public synchronized void addLog(String log) {
        this.logs.add(log);
        notifyListeners();
    }

    public synchronized void clearLogs() {
        this.logs.clear();
        notifyListeners();
    }

    // ---- Traffic State ----
    public synchronized void setCurrentTraffic(List<Packet> packets) {
        this.currentTraffic = new ArrayList<>(packets);
        notifyListeners();
    }

    public synchronized void setTotalPackets(int total) {
        this.totalPackets = total;
        notifyListeners();
    }

    // ---- Attack State ----
    public synchronized void setAttackDetected(boolean detected) {
        this.attackDetected = detected;
        notifyListeners();
    }

    public synchronized void setVictimIP(String victimIP) {
        this.victimIP = victimIP;
        notifyListeners();
    }

    // ---- Flow Table State ----
    public synchronized void addFlowEntry(FlowEntry entry) {
        flowTable.add(entry);
        notifyListeners();
    }

    public synchronized void clearFlowEntries() {
        flowTable.clear();
        flowTable.add(FlowEntry.defaultForward()); // restore default
        notifyListeners();
    }

    // ---- Bandwidth State ----
    public synchronized void updateBandwidth(String ip, int percent) {
        nodeBandwidth.put(ip, Math.min(100, Math.max(0, percent)));
        notifyListeners();
    }

    public synchronized void resetBandwidth() {
        nodeBandwidth.clear();
        notifyListeners();
    }

    // ---- Full Network Reset ----
    public synchronized void resetAll() {
        // Unblock all nodes and restore roles
        for (Node n : nodes) {
            if (n.isBlocked()) {
                n.setBlocked(false);
                n.setRole("BOT"); // restore pre-assigned bot role
            }
        }
        logs.clear();
        currentTraffic.clear();
        totalPackets = 0;
        attackDetected = false;
        victimIP = "None";
        flowTable.clear();
        flowTable.add(FlowEntry.defaultForward());
        nodeBandwidth.clear();
        notifyListeners();
    }

    // ---- Getters ----
    public List<Node> getNodes()              { return nodes; }
    public List<String> getLogs()             { return logs; }
    public List<Packet> getCurrentTraffic()   { return currentTraffic; }
    public int getTotalPackets()              { return totalPackets; }
    public boolean isAttackDetected()         { return attackDetected; }
    public String getVictimIP()               { return victimIP; }
    public List<FlowEntry> getFlowTable()     { return flowTable; }
    public Map<String, Integer> getNodeBandwidth() { return nodeBandwidth; }

    // ---- JSON serialization for REST API ----
    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalPackets\":").append(totalPackets).append(",");
        sb.append("\"attackDetected\":").append(attackDetected).append(",");
        sb.append("\"victimIP\":\"").append(victimIP).append("\",");

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
