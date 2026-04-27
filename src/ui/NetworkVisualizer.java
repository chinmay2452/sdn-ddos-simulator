package ui;

import api.FlowEntry;
import api.StateStore;
import model.Node;
import model.Packet;
import simulator.TrafficGenerator;
import detection.AttackDetector;
import detection.ThresholdDetector;
import detection.DistributedDetector;
import detection.HybridDetector;
import detection.RateDetector;
import controller.SDNController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NetworkVisualizer extends JFrame implements StateStore.StateChangeListener {

    private StateStore state;
    private TopologyPanel topologyPanel;
    private MetricsPanel metricsPanel;
    private LogPanel logPanel;
    private ControlPanel controlPanel;
    private BandwidthPanel bandwidthPanel;
    private FlowTablePanel flowTablePanel;

    // Coalescing flag: prevents flooding the EDT with redundant repaints
    private volatile boolean updatePending = false;

    public NetworkVisualizer() {
        this.state = StateStore.getInstance();
        this.state.addListener(this);

        setTitle("SDN DDoS Cyber Defense Dashboard");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));
        getContentPane().setBackground(new Color(15, 15, 25));

        // --- CENTER: Topology canvas ---
        topologyPanel = new TopologyPanel();
        add(topologyPanel, BorderLayout.CENTER);

        // --- EAST: Right sidebar ---
        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        rightPanel.setBackground(new Color(20, 20, 32));
        rightPanel.setPreferredSize(new Dimension(360, 900));

        metricsPanel   = new MetricsPanel();
        controlPanel   = new ControlPanel();
        bandwidthPanel = new BandwidthPanel();
        logPanel       = new LogPanel();

        JPanel topRight = new JPanel(new BorderLayout(0, 4));
        topRight.setBackground(new Color(20, 20, 32));
        topRight.add(metricsPanel,   BorderLayout.NORTH);
        topRight.add(controlPanel,   BorderLayout.CENTER);
        topRight.add(bandwidthPanel, BorderLayout.SOUTH);

        rightPanel.add(topRight, BorderLayout.NORTH);
        rightPanel.add(logPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // --- SOUTH: Flow Table panel (full width) ---
        flowTablePanel = new FlowTablePanel();
        flowTablePanel.setPreferredSize(new Dimension(1400, 220));
        add(flowTablePanel, BorderLayout.SOUTH);

        // Seed nodes if not already done
        if (state.getNodes().isEmpty()) {
            List<Node> nodes = new ArrayList<>();
            for (int i = 1; i <= 10; i++) nodes.add(new Node("N" + i, "192.168.1." + i));
            state.updateNodes(nodes);
        }

        setVisible(true);
    }

    @Override
    public void onStateChange() {
        // Only schedule ONE repaint even if many state changes fire back-to-back.
        // This is the key fix for lag during rapid mitigation logging.
        if (!updatePending) {
            updatePending = true;
            SwingUtilities.invokeLater(() -> {
                updatePending = false;
                topologyPanel.updateState();
                metricsPanel.updateState();
                logPanel.updateState();
                bandwidthPanel.updateState();
                flowTablePanel.updateState();
                repaint();
            });
        }
    }

    // =========================================================
    // TOPOLOGY PANEL
    // =========================================================
    class TopologyPanel extends JPanel {
        private Map<String, Point> nodePositions = new HashMap<>();
        private List<VisualPacket> visualPackets  = new ArrayList<>();
        private Timer animationTimer;

        TopologyPanel() {
            setBackground(new Color(10, 10, 20));
            animationTimer = new Timer(30, e -> tick());
            animationTimer.start();
        }

        private void tick() {
            synchronized (visualPackets) {
                visualPackets.removeIf(p -> p.progress >= 1.0);
                for (VisualPacket p : visualPackets) p.progress += 0.015;
            }
            repaint();
        }

        void updateState() {
            int w = getWidth(), h = getHeight();
            if (w == 0) return;
            int pad = 60, botIdx = 0, normIdx = 0;
            for (Node n : state.getNodes()) {
                if (nodePositions.containsKey(n.getIpAddress())) continue;
                switch (n.getRole()) {
                    case "BOTMASTER":
                        nodePositions.put(n.getIpAddress(), new Point(w / 2, pad)); break;
                    case "BOT":
                        nodePositions.put(n.getIpAddress(), new Point(pad + 40, pad + 120 + botIdx++ * 65)); break;
                    case "SERVER":
                        nodePositions.put(n.getIpAddress(), new Point(w - pad - 40, h / 2)); break;
                    default:
                        nodePositions.put(n.getIpAddress(), new Point(w / 2 + 80, pad + 80 + normIdx++ * 65));
                }
            }
            List<Packet> packets = state.getCurrentTraffic();
            synchronized (visualPackets) {
                for (Packet p : packets) {
                    Point src = nodePositions.get(p.getSourceIP());
                    Point dst = nodePositions.get(p.getDestinationIP());
                    if (src != null && dst != null) {
                        boolean isAttack = p.getDestinationIP().equals(state.getVictimIP());
                        visualPackets.add(new VisualPacket(src, dst, isAttack));
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw packet trails
            synchronized (visualPackets) {
                for (VisualPacket p : visualPackets) {
                    Color lineColor = p.isAttack ? new Color(255, 50, 50, 80) : new Color(50, 220, 50, 60);
                    g2.setColor(lineColor);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawLine(p.start.x, p.start.y, p.end.x, p.end.y);

                    int cx = (int)(p.start.x + (p.end.x - p.start.x) * p.progress);
                    int cy = (int)(p.start.y + (p.end.y - p.start.y) * p.progress);
                    g2.setColor(p.isAttack ? new Color(255, 80, 80) : new Color(80, 255, 80));
                    g2.fillOval(cx - 4, cy - 4, 8, 8);
                }
            }

            // Draw nodes
            for (Node n : state.getNodes()) {
                Point p = nodePositions.get(n.getIpAddress());
                if (p == null) continue;

                // Glow ring for special roles
                if (n.isBlocked()) {
                    g2.setColor(new Color(255, 0, 0, 60));
                    g2.fillOval(p.x - 22, p.y - 22, 44, 44);
                } else if (n.getRole().equals("SERVER")) {
                    g2.setColor(new Color(0, 200, 255, 60));
                    g2.fillOval(p.x - 22, p.y - 22, 44, 44);
                }

                // Node fill
                Color fill;
                if      (n.isBlocked())               fill = new Color(180, 30, 30);
                else if (n.getRole().equals("BOTMASTER")) fill = new Color(200, 100, 0);
                else if (n.getRole().equals("BOT"))   fill = new Color(220, 80, 80);
                else if (n.getRole().equals("SERVER")) fill = new Color(0, 180, 220);
                else                                   fill = new Color(70, 130, 180);
                g2.setColor(fill);
                g2.fillOval(p.x - 16, p.y - 16, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(p.x - 16, p.y - 16, 32, 32);

                // Labels
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString(n.getNodeId() + " (" + n.getRole() + ")", p.x - 22, p.y + 30);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2.setColor(new Color(180, 180, 180));
                g2.drawString(n.getIpAddress(), p.x - 26, p.y + 42);
            }
        }
    }

    static class VisualPacket {
        Point start, end;
        double progress = 0;
        boolean isAttack;
        VisualPacket(Point s, Point e, boolean a) { start = s; end = e; isAttack = a; }
    }

    // =========================================================
    // METRICS PANEL
    // =========================================================
    class MetricsPanel extends JPanel {
        private JLabel packetsLabel, victimLabel, statusLabel;

        MetricsPanel() {
            setLayout(new GridLayout(3, 1, 2, 2));
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 100)), "Network Metrics",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), new Color(180, 200, 255)));
            setBackground(new Color(22, 22, 38));

            packetsLabel = styledLabel("Total Packets: 0");
            victimLabel  = styledLabel("Victim Node: None");
            statusLabel  = styledLabel("Status: \u2713 NORMAL");
            statusLabel.setForeground(new Color(80, 220, 80));

            add(packetsLabel);
            add(victimLabel);
            add(statusLabel);
        }

        private JLabel styledLabel(String text) {
            JLabel l = new JLabel(text);
            l.setForeground(new Color(200, 210, 255));
            l.setFont(new Font("SansSerif", Font.PLAIN, 12));
            return l;
        }

        void updateState() {
            packetsLabel.setText("Total Packets: " + state.getTotalPackets());
            victimLabel.setText("Victim Node: " + state.getVictimIP());
            if (state.isAttackDetected()) {
                statusLabel.setText("Status: \u26A0 ATTACK DETECTED!");
                statusLabel.setForeground(new Color(255, 80, 80));
            } else {
                statusLabel.setText("Status: \u2713 NORMAL");
                statusLabel.setForeground(new Color(80, 220, 80));
            }
        }
    }

    // =========================================================
    // CONTROL PANEL
    // =========================================================
    class ControlPanel extends JPanel {
        private JComboBox<String> detectorBox;

        ControlPanel() {
            setLayout(new GridLayout(6, 1, 4, 4));
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 100)), "Control Center",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), new Color(180, 200, 255)));
            setBackground(new Color(22, 22, 38));

            String[] strategies = {"Threshold (Volume)", "Distributed (Botnet)", "Rate (Flow-based)", "Hybrid (All)"};
            detectorBox = new JComboBox<>(strategies);
            detectorBox.setSelectedIndex(3);
            detectorBox.setBackground(new Color(30, 30, 50));
            detectorBox.setForeground(Color.WHITE);

            JButton btnNormal   = makeButton("Generate Normal Traffic", new Color(30, 100, 50));
            JButton btnAttack   = makeButton("Trigger Botnet Attack",   new Color(140, 30, 30));
            JButton btnMitigate = makeButton("Apply Mitigation",        new Color(30, 80, 130));
            JButton btnReset    = makeButton("Reset Network",           new Color(80, 50, 10));

            add(detectorBox);
            add(btnNormal);
            add(btnAttack);
            add(btnMitigate);
            add(btnReset);

            btnNormal.addActionListener(e   -> generateNormal());
            btnAttack.addActionListener(e   -> triggerAttack());
            btnMitigate.addActionListener(e -> applyMitigation());
            btnReset.addActionListener(e    -> resetNetwork());
        }

        private JButton makeButton(String text, Color bg) {
            JButton b = new JButton(text);
            b.setBackground(bg);
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            b.setFont(new Font("SansSerif", Font.BOLD, 11));
            b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return b;
        }

        private void generateNormal() {
            TrafficGenerator gen = new TrafficGenerator();
            List<Packet> packets = gen.generateNormalTraffic(state.getNodes(), 20);
            state.setCurrentTraffic(packets);
            state.setTotalPackets(state.getTotalPackets() + packets.size());
            state.addLog("[TRAFFIC] Generated 20 normal packets");
            // Set low bandwidth for all nodes
            Random r = new Random();
            for (Node n : state.getNodes()) {
                state.updateBandwidth(n.getIpAddress(), 5 + r.nextInt(20));
            }
        }

        private void triggerAttack() {
            TrafficGenerator gen = new TrafficGenerator();
            List<Packet> packets = gen.generateSuspiciousTraffic(state.getNodes(), 0);
            state.setCurrentTraffic(packets);
            state.setTotalPackets(state.getTotalPackets() + packets.size());

            String victimIP = "None";
            if (!packets.isEmpty()) {
                victimIP = packets.get(0).getDestinationIP();
                state.setVictimIP(victimIP);
            }

            // Push RATE-LIMIT flow rule for victim
            if (!victimIP.equals("None")) {
                state.addFlowEntry(FlowEntry.rateLimitVictim(victimIP));
            }

            // Update bandwidth: bots high, victim very high, normals low
            Random r = new Random();
            for (Node n : state.getNodes()) {
                if (n.getRole().equals("BOT"))         state.updateBandwidth(n.getIpAddress(), 50 + r.nextInt(30));
                else if (n.getIpAddress().equals(victimIP)) state.updateBandwidth(n.getIpAddress(), 85 + r.nextInt(15));
                else                                   state.updateBandwidth(n.getIpAddress(), 5 + r.nextInt(15));
            }

            runDetection();
            state.addLog("[ATTACK] Botnet attack launched. Victim: " + victimIP);
        }

        private void runDetection() {
            AttackDetector detector = buildDetector();
            List<String> suspicious = detector.detectAttack(state.getCurrentTraffic());
            if (!suspicious.isEmpty()) {
                state.setAttackDetected(true);
                state.addLog("[DETECT] Attack detected from " + suspicious.size() + " sources!");
            } else {
                state.setAttackDetected(false);
                state.addLog("[DETECT] No attack patterns found.");
            }
        }

        private void applyMitigation() {
            AttackDetector detector = buildDetector();
            List<String> suspicious = detector.detectAttack(state.getCurrentTraffic());
            SDNController controller = new SDNController();
            controller.applyMitigation(state.getNodes(), suspicious, state.getCurrentTraffic());
            state.updateNodes(state.getNodes());
            state.setAttackDetected(false);
            state.addLog("[MITIGATE] Blocked bot IPs. Traffic normalized.");
        }

        private void resetNetwork() {
            state.resetAll();
            // Clear topology positions so layout is fresh
            topologyPanel.nodePositions.clear();
            topologyPanel.visualPackets.clear();
            state.addLog("[RESET] Network restored to initial state.");
        }

        private AttackDetector buildDetector() {
            int choice = detectorBox.getSelectedIndex();
            if (choice == 0) return new ThresholdDetector();
            if (choice == 1) return new DistributedDetector();
            if (choice == 2) return new RateDetector();
            return new HybridDetector();
        }
    }

    // =========================================================
    // BANDWIDTH PANEL
    // =========================================================
    class BandwidthPanel extends JPanel {
        private Map<String, JProgressBar> bars = new HashMap<>();
        private Map<String, JLabel> labels      = new HashMap<>();

        BandwidthPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 100)), "Bandwidth / Load",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), new Color(180, 200, 255)));
            setBackground(new Color(22, 22, 38));
        }

        void updateState() {
            // Add missing node rows
            for (Node n : state.getNodes()) {
                if (!bars.containsKey(n.getIpAddress())) {
                    JPanel row = new JPanel(new BorderLayout(4, 0));
                    row.setBackground(new Color(22, 22, 38));
                    row.setMaximumSize(new Dimension(360, 20));

                    JLabel lbl = new JLabel(String.format("%-12s", n.getNodeId() + "(" + n.getRole().charAt(0) + ")"));
                    lbl.setForeground(new Color(180, 200, 255));
                    lbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    lbl.setPreferredSize(new Dimension(90, 16));

                    JProgressBar bar = new JProgressBar(0, 100);
                    bar.setValue(0);
                    bar.setStringPainted(true);
                    bar.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    bar.setBorderPainted(false);

                    row.add(lbl, BorderLayout.WEST);
                    row.add(bar, BorderLayout.CENTER);
                    add(row);

                    bars.put(n.getIpAddress(), bar);
                    labels.put(n.getIpAddress(), lbl);
                }
            }

            // Update bar values and colors — do NOT call revalidate() here,
            // bars are already laid out; revalidate causes expensive full relayout every tick
            Map<String, Integer> bw = state.getNodeBandwidth();
            for (Node n : state.getNodes()) {
                JProgressBar bar = bars.get(n.getIpAddress());
                if (bar == null) continue;
                int val = bw.getOrDefault(n.getIpAddress(), 0);
                bar.setValue(val);
                bar.setString(val + "%");
                if (val <= 30)      bar.setForeground(new Color(50, 200, 80));
                else if (val <= 60) bar.setForeground(new Color(230, 150, 30));
                else                bar.setForeground(new Color(220, 50, 50));
            }
            repaint();
        }
    }

    // =========================================================
    // FLOW TABLE PANEL
    // =========================================================
    class FlowTablePanel extends JPanel {
        private DefaultTableModel tableModel;
        private JTable table;
        // Track how many rows we have already rendered so we only append new ones
        private int renderedRowCount = 0;

        FlowTablePanel() {
            setLayout(new BorderLayout());
            setBackground(new Color(12, 12, 22));
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 120), 1),
                "SDN Flow Table  (OpenFlow-style Rules)",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), new Color(150, 190, 255)));

            String[] cols = {"Priority", "Match Rule", "Action", "Status", "Time"};
            tableModel = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            table = new JTable(tableModel);
            table.setBackground(new Color(15, 15, 28));
            table.setForeground(new Color(200, 210, 255));
            table.setFont(new Font("Monospaced", Font.PLAIN, 12));
            table.setRowHeight(22);
            table.setGridColor(new Color(40, 40, 70));
            table.getTableHeader().setBackground(new Color(25, 25, 50));
            table.getTableHeader().setForeground(new Color(150, 180, 255));
            table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
            table.setSelectionBackground(new Color(50, 50, 100));

            // Custom renderer for row coloring
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object val,
                        boolean sel, boolean focus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                    String action = (String) tableModel.getValueAt(row, 2);
                    if      (action != null && action.startsWith("DROP"))      c.setBackground(new Color(80, 20, 20));
                    else if (action != null && action.startsWith("RATE"))      c.setBackground(new Color(80, 70, 10));
                    else if (action != null && action.startsWith("REROUTE"))   c.setBackground(new Color(10, 60, 80));
                    else                                                        c.setBackground(new Color(15, 50, 25));
                    c.setForeground(Color.WHITE);
                    if (sel) c.setBackground(new Color(60, 60, 120));
                    return c;
                }
            });

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBackground(new Color(12, 12, 22));
            scroll.getViewport().setBackground(new Color(12, 12, 22));
            add(scroll, BorderLayout.CENTER);
        }

        void updateState() {
            List<FlowEntry> entries = state.getFlowTable();
            // Only append new rows — never rebuild the entire table
            if (entries.size() > renderedRowCount) {
                for (int i = renderedRowCount; i < entries.size(); i++) {
                    FlowEntry e = entries.get(i);
                    tableModel.addRow(new Object[]{
                        e.getPriorityStr(),
                        e.getMatchRule(),
                        e.getActionStr(),
                        e.getStatus(),
                        e.getTimestamp()
                    });
                }
                renderedRowCount = entries.size();
            } else if (entries.size() < renderedRowCount) {
                // Full reset happened — rebuild cleanly
                tableModel.setRowCount(0);
                renderedRowCount = 0;
            }
        }
    }

    // =========================================================
    // LOG PANEL
    // =========================================================
    class LogPanel extends JPanel {
        private JTextArea textArea;

        LogPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 100)), "Event Log",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), new Color(180, 200, 255)));
            setBackground(new Color(10, 10, 18));

            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setBackground(new Color(8, 8, 15));
            textArea.setForeground(new Color(100, 230, 100));
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            textArea.setLineWrap(true);

            JScrollPane scroll = new JScrollPane(textArea);
            scroll.setBorder(null);
            add(scroll, BorderLayout.CENTER);
        }

        void updateState() {
            List<String> logs = state.getLogs();
            StringBuilder sb  = new StringBuilder();
            int start = Math.max(0, logs.size() - 100);
            for (int i = start; i < logs.size(); i++) {
                sb.append("> ").append(logs.get(i)).append("\n");
            }
            textArea.setText(sb.toString());
            // Auto-scroll to bottom
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NetworkVisualizer::new);
    }
}
