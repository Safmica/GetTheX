package com.safmica.model;

import java.util.UUID;

public class Player {
    private String id;
    private String name;
    private boolean isHost;

    public Player(String name, boolean isHost) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.isHost = isHost;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isHost() { return isHost; }

    @Override
    public String toString() { return name; } 
}
