package com.safmica.model;

import java.util.List;

public class SaveState {
    private Room room;
    private Game game;
    private List<Player> players;
    private List<PlayerLeaderboard> leaderboards;
    private long timestamp;
    private boolean inProgress;
    private int serverPort;

    public SaveState(Room room, Game game, List<Player> players, List<PlayerLeaderboard> leaderboards, boolean inProgress, int serverPort) {
        this.room = room;
        this.game = game;
        this.players = players;
        this.leaderboards = leaderboards;
        this.inProgress = inProgress;
        this.serverPort = serverPort;
        this.timestamp = System.currentTimeMillis();
    }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public List<PlayerLeaderboard> getLeaderboards() { return leaderboards; }
    public void setLeaderboards(List<PlayerLeaderboard> leaderboards) { this.leaderboards = leaderboards; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isInProgress() { return inProgress; }
    public void setInProgress(boolean inProgress) { this.inProgress = inProgress; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
}
