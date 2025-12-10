package com.safmica;

import com.safmica.model.Player;
import com.safmica.model.Room;
import com.safmica.network.server.TcpServerHandler;
import com.safmica.network.client.TcpClientHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameSession {
    private static final GameSession INSTANCE = new GameSession();

    private final List<Player> players = new CopyOnWriteArrayList<>();
    private String username;
    private Room room;
    private TcpServerHandler server;
    private TcpClientHandler client;

    private GameSession() {}

    public static GameSession getInstance() { return INSTANCE; }

    public List<Player> getPlayers() { return new ArrayList<>(players); }
    public void setPlayers(List<Player> list) {
        players.clear();
        if (list != null) players.addAll(list);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public TcpServerHandler getServer() { return server; }
    public void setServer(TcpServerHandler server) { this.server = server; }

    public TcpClientHandler getClient() { return client; }
    public void setClient(TcpClientHandler client) { this.client = client; }
}
