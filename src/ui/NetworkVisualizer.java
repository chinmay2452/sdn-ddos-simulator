package ui;

import api.StateStore;
import model.Node;
import model.Packet;
import simulator.TrafficGenerator;
import detection.AttackDetector;
import detection.ThresholdDetector;
import detection.DistributedDetector;
import detection.HybridDetector;
import controller.SDNController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public NetworkVisualizer() {
        this.state = StateStore.getInstance();
        this.state.addListener(this);

        setTitle("SDN DDoS Simulation Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        topologyPanel = new TopologyPanel();
        add(topologyPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(350, 800));

        metricsPanel = new MetricsPanel();
        controlPanel = new ControlPanel();
        
        JPanel topSidebar = new JPanel(new BorderLayout());
        topSidebar.add(metricsPanel, BorderLayout.NORTH);
        topSidebar.add(controlPanel, BorderLayout.CENTER);
        
        rightPanel.add(topSidebar, BorderLayout.NORTH);
        
        logPanel = new LogPanel();
        rightPanel.add(logPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        // Initialize nodes in state if empty
        if (state.getNodes().isEmpty()) {
            List<Node> nodes = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                nodes.add(new Node("N" + i, "192.168.1." + i));
            }
            state.updateNodes(nodes);
        }

        setVisible(true);
    }

    @Override
    public void onStateChange() {
        SwingUtilities.invokeLater(() -> {
            topologyPanel.updateState();
            metricsPanel.updateState();
            logPanel.updateState();
            repaint();
        });
    }

    // --- Topology Panel ---
    class TopologyPanel extends JPanel {
        private Map<String, Point> nodePositions = new HashMap<>();
        private List<VisualPacket> visualPackets = new ArrayList<>();
        private Timer animationTimer;

        public TopologyPanel() {
            setBackground(new Color(20, 20, 30));
            animationTimer = new Timer(30, e -> updateAnimation());
            animationTimer.start();
        }

        private void updateAnimation() {
            synchronized (visualPackets) {
                visualPackets.removeIf(p -> p.progress >= 1.0);
                for (VisualPacket p : visualPackets) {
                    p.progress += 0.05;
                }
            }
            repaint();
        }

        public void updateState() {
            List<Node> nodes = state.getNodes();
            // Assign positions if not already assigned
            int w = getWidth();
            int h = getHeight();
            if (w == 0) return;

            // Simple layout logic
            // Botmaster at top
            // Bots on left
            // Victim on right
            // Others in middle
            
            int padding = 50;
            int botIndex = 0;
            int normalIndex = 0;

            for (Node n : nodes) {
                if (!nodePositions.containsKey(n.getIpAddress())) {
                    if (n.getRole().equals("BOTMASTER")) {
                        nodePositions.put(n.getIpAddress(), new Point(w / 2, padding));
                    } else if (n.getRole().equals("BOT")) {
                        nodePositions.put(n.getIpAddress(), new Point(padding + 50, padding + 100 + (botIndex++ * 60)));
                    } else if (n.getRole().equals("SERVER")) {
                        nodePositions.put(n.getIpAddress(), new Point(w - padding - 50, h / 2));
                    } else {
                        nodePositions.put(n.getIpAddress(), new Point(w / 2, padding + 100 + (normalIndex++ * 60)));
                    }
                }
            }

            // Create visual packets for new traffic
            List<Packet> packets = state.getCurrentTraffic();
            synchronized (visualPackets) {
                for (Packet p : packets) {
                    Point src = nodePositions.get(p.getSourceIP());
                    Point dst = nodePositions.get(p.getDestinationIP());
                    if (src != null && dst != null) {
                        visualPackets.add(new VisualPacket(src, dst, p.getDestinationIP().equals(state.getVictimIP())));
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw connections/traffic lines
            synchronized (visualPackets) {
                for (VisualPacket p : visualPackets) {
                    g2d.setColor(p.isAttack ? new Color(255, 0, 0, 100) : new Color(0, 255, 0, 50));
                    g2d.drawLine(p.start.x, p.start.y, p.end.x, p.end.y);
                    
                    // Draw moving dot
                    int curX = (int) (p.start.x + (p.end.x - p.start.x) * p.progress);
                    int curY = (int) (p.start.y + (p.end.y - p.start.y) * p.progress);
                    g2d.setColor(p.isAttack ? Color.RED : Color.GREEN);
                    g2d.fillOval(curX - 3, curY - 3, 6, 6);
                }
            }

            // Draw nodes
            for (Node n : state.getNodes()) {
                Point p = nodePositions.get(n.getIpAddress());
                if (p == null) continue;

                // Node color based on state
                if (n.isBlocked()) {
                    g2d.setColor(Color.RED);
                } else if (n.getRole().equals("BOT")) {
                    g2d.setColor(new Color(255, 100, 0));
                } else if (n.getRole().equals("SERVER")) {
                    g2d.setColor(Color.CYAN);
                } else {
                    g2d.setColor(Color.WHITE);
                }

                g2d.fillOval(p.x - 15, p.y - 15, 30, 30);
                g2d.setColor(Color.WHITE);
                g2d.drawOval(p.x - 15, p.y - 15, 30, 30);
                
                // Label
                g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2d.drawString(n.getNodeId() + " (" + n.getRole() + ")", p.x - 20, p.y + 30);
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2d.drawString(n.getIpAddress(), p.x - 25, p.y + 42);
            }
        }
    }

    static class VisualPacket {
        Point start, end;
        double progress = 0;
        boolean isAttack;

        public VisualPacket(Point start, Point end, boolean isAttack) {
            this.start = start;
            this.end = end;
            this.isAttack = isAttack;
        }
    }

    // --- Metrics Panel ---
    class MetricsPanel extends JPanel {
        private JLabel totalPacketsLabel;
        private JLabel victimLabel;
        private JLabel statusLabel;

        public MetricsPanel() {
            setLayout(new GridLayout(4, 1));
            setBorder(BorderFactory.createTitledBorder("Network Metrics"));
            setBackground(new Color(240, 240, 240));

            totalPacketsLabel = new JLabel("Total Packets: 0");
            victimLabel = new JLabel("Target Victim: None");
            statusLabel = new JLabel("System Status: Normal");
            statusLabel.setForeground(new Color(0, 150, 0));

            add(totalPacketsLabel);
            add(victimLabel);
            add(statusLabel);
        }

        public void updateState() {
            totalPacketsLabel.setText("Total Packets: " + state.getTotalPackets());
            victimLabel.setText("Target Victim: " + state.getVictimIP());
            if (state.isAttackDetected()) {
                statusLabel.setText("System Status: ATTACK DETECTED!");
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setText("System Status: Normal");
                statusLabel.setForeground(new Color(0, 150, 0));
            }
        }
    }

    // --- Control Panel ---
    class ControlPanel extends JPanel {
        private JComboBox<String> detectorBox;

        public ControlPanel() {
            setLayout(new GridLayout(5, 1, 5, 5));
            setBorder(BorderFactory.createTitledBorder("Control Center"));

            JButton btnNormal = new JButton("Generate Normal Traffic");
            JButton btnAttack = new JButton("Trigger Botnet Attack");
            JButton btnMitigate = new JButton("Apply Mitigation");
            
            String[] strategies = {"Threshold (Single)", "Distributed (Botnet)", "Hybrid (All)"};
            detectorBox = new JComboBox<>(strategies);
            detectorBox.setSelectedIndex(2);

            add(new JLabel("Detection Strategy:"));
            add(detectorBox);
            add(btnNormal);
            add(btnAttack);
            add(btnMitigate);

            btnNormal.addActionListener(e -> generateNormal());
            btnAttack.addActionListener(e -> triggerAttack());
            btnMitigate.addActionListener(e -> applyMitigation());
        }

        private void generateNormal() {
            TrafficGenerator gen = new TrafficGenerator();
            List<Packet> packets = gen.generateNormalTraffic(state.getNodes(), 20);
            state.setCurrentTraffic(packets);
            state.setTotalPackets(state.getTotalPackets() + packets.size());
            state.addLog("Generated 20 normal packets");
        }

        private void triggerAttack() {
            TrafficGenerator gen = new TrafficGenerator();
            List<Packet> packets = gen.generateSuspiciousTraffic(state.getNodes(), 0);
            state.setCurrentTraffic(packets);
            state.setTotalPackets(state.getTotalPackets() + packets.size());
            
            if (!packets.isEmpty()) {
                state.setVictimIP(packets.get(0).getDestinationIP());
            }

            // Run detection immediately
            runDetection();
        }

        private void runDetection() {
            AttackDetector detector;
            int choice = detectorBox.getSelectedIndex();
            if (choice == 0) detector = new ThresholdDetector();
            else if (choice == 1) detector = new DistributedDetector();
            else detector = new HybridDetector();

            List<String> suspicious = detector.detectAttack(state.getCurrentTraffic());
            if (!suspicious.isEmpty()) {
                state.setAttackDetected(true);
                state.addLog("DETECTION: Attack detected from " + suspicious.size() + " sources!");
            } else {
                state.setAttackDetected(false);
            }
        }

        private void applyMitigation() {
            SDNController controller = new SDNController();
            // We need suspicious IPs again or store them in state
            AttackDetector detector;
            int choice = detectorBox.getSelectedIndex();
            if (choice == 0) detector = new ThresholdDetector();
            else if (choice == 1) detector = new DistributedDetector();
            else detector = new HybridDetector();

            List<String> suspicious = detector.detectAttack(state.getCurrentTraffic());
            controller.applyMitigation(state.getNodes(), suspicious, state.getCurrentTraffic());
            state.updateNodes(state.getNodes());
            state.addLog("MITIGATION: Applied blocking and rerouting.");
        }
    }

    // --- Log Panel ---
    class LogPanel extends JPanel {
        private JTextArea textArea;

        public LogPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("System Logs"));
            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setBackground(Color.BLACK);
            textArea.setForeground(Color.GREEN);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            add(new JScrollPane(textArea), BorderLayout.CENTER);
        }

        public void updateState() {
            List<String> logs = state.getLogs();
            StringBuilder sb = new StringBuilder();
            // Show last 20 logs
            int start = Math.max(0, logs.size() - 20);
            for (int i = start; i < logs.size(); i++) {
                sb.append("> ").append(logs.get(i)).append("\n");
            }
            textArea.setText(sb.toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NetworkVisualizer());
    }
}
