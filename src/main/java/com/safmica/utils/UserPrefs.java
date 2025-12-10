package com.safmica.utils;

public class UserPrefs {
    private String playerId;
    private String name;

    public UserPrefs() {}

    public UserPrefs(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
