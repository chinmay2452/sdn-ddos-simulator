package api;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single OpenFlow-style flow rule in the SDN switch's flow table.
 * Each entry defines how the switch should handle a specific type of traffic.
 */
public class FlowEntry {
    public enum Action { DROP, FORWARD, RATE_LIMIT, REROUTE }
    public enum Priority { HIGH, MEDIUM, LOW }

    private Priority priority;
    private String matchRule;   // e.g., "src=192.168.1.3" or "src=*"
    private Action action;
    private String actionDetail; // e.g., reroute destination IP
    private String status;       // BLOCKED, ACTIVE, DEFAULT
    private String timestamp;

    public FlowEntry(Priority priority, String matchRule, Action action, String actionDetail, String status) {
        this.priority = priority;
        this.matchRule = matchRule;
        this.action = action;
        this.actionDetail = actionDetail;
        this.status = status;
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** Factory: default allow-all rule installed at startup */
    public static FlowEntry defaultForward() {
        return new FlowEntry(Priority.LOW, "src=*", Action.FORWARD, "", "DEFAULT");
    }

    /** Factory: rate-limit rule when attack is first detected */
    public static FlowEntry rateLimitVictim(String victimIP) {
        return new FlowEntry(Priority.MEDIUM, "dst=" + victimIP, Action.RATE_LIMIT, "", "ACTIVE");
    }

    /** Factory: drop rule for a confirmed bot */
    public static FlowEntry dropBot(String botIP) {
        return new FlowEntry(Priority.HIGH, "src=" + botIP, Action.DROP, "", "BLOCKED");
    }

    /** Factory: reroute rule for redirected traffic */
    public static FlowEntry rerouteTraffic(String victimIP, String safeNode) {
        return new FlowEntry(Priority.MEDIUM, "dst=" + victimIP, Action.REROUTE, "-> " + safeNode, "ACTIVE");
    }

    // --- Getters ---
    public String getPriorityStr() { return priority.name(); }
    public String getMatchRule()   { return matchRule; }
    public String getActionStr() {
        if (action == Action.REROUTE) return "REROUTE " + actionDetail;
        if (action == Action.RATE_LIMIT) return "RATE-LIMIT";
        return action.name();
    }
    public Action getAction()      { return action; }
    public String getStatus()      { return status; }
    public String getTimestamp()   { return timestamp; }
}
