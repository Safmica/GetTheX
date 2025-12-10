package com.safmica.model;

public class PlayerLeaderboard {
    private String id;
    private String name;
    private int score;

    public PlayerLeaderboard(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public void addScore() {
        score++;
    }
}
