package com.safmica.model;

public class ClientSaveState {
    private String username;
    private String serverIp;
    private int serverPort;
    private String playerId;
    private long timestamp;

    public ClientSaveState() { }

    public ClientSaveState(String username, String serverIp, int serverPort, String playerId) {
        this.username = username;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.playerId = playerId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
