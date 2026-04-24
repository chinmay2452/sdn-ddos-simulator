package model;

public class BotNode extends Node {
    private boolean isBot;

    public BotNode(String nodeId, String ipAddress) {
        super(nodeId, ipAddress);
        this.isBot = true;
        setRole("BOT");
    }

    public boolean isBot() {
        return isBot;
    }
}
