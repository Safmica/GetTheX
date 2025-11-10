package com.safmica.model;

import java.util.UUID;

public class Player {
    private final String id;
    private final String name;
    private final boolean isHost;

    public Player(String name, boolean isHost) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.isHost = isHost;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean getIsHost() { return isHost; }

    @Override
    public String toString() { return name; } 
}
