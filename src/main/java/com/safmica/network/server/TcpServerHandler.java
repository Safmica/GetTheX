package com.safmica.network.server;

import com.google.gson.Gson;
import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.model.Player;
import com.safmica.model.PlayerEvent;
import com.safmica.model.PlayerLeaderboard;
import com.safmica.model.PlayerSurrender;
import com.safmica.model.Room;
import com.safmica.utils.LoggerHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServerHandler extends Thread {

  private ServerSocket serverSocket;
  private int port;
  private Gson gson = new com.google.gson.Gson();
  private boolean isRunning = true;
  private Room room;
  private Game game = new Game();
  private SubmissionProcessor submissionProcessor;
  private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
  private final List<Player> players = new CopyOnWriteArrayList<>();
  private final String TOTAL_CARD = "TOTAL_CARD";
  private final String TOTAL_ROUND = "TOTAL_ROUND";
  private List<String> currentSurrenderOffer = new CopyOnWriteArrayList<>();

  private volatile boolean finalRoundActive = false;
  private final List<String> finalRoundPlayers = new CopyOnWriteArrayList<>();

  public TcpServerHandler(int port) {
    this.port = port;
  }

  public void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    room = new Room(4, 3);

    submissionProcessor = new SubmissionProcessor(this, game);
    // LoggerHandler.logInfoMessage("Server is running on port " + port);
    this.start();
  }

  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
        System.out.println("DEBUG : SERVER CLOSE");
        // TODO: REMOVE THIS DEBUG
      }
    } catch (IOException e) {
      LoggerHandler.logError("Error closing Client socket.", e);
    }

    try {
      if (submissionProcessor != null)
        submissionProcessor.shutdown();
    } catch (Exception ignored) {
    }
  }

  public void disconnectAllClients() {
    for (ClientHandler handler : clients) {
      try {
        handler.disconnect();
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  public void run() {
    while (isRunning) {
      try {
        Socket clientSocket = serverSocket.accept();
        handleNewClient(clientSocket);
      } catch (IOException e) {
        if (isRunning) {
          LoggerHandler.logError("Error accepting client connection.", e);
        } else {
          // LoggerHandler.logInfoMessage("Server stopped.");
        }
      }
    }
  }

  public void updateRoomSettings(int option, String type) {
    switch (type) {
      case TOTAL_CARD:
        room.setTotalCard(option);
        break;
      case TOTAL_ROUND:
        room.setTotalRound(option);
        break;
      default:
        break;
    }
    broadcastSettinsEvent(null);
  }

  public void startGame() {
    submissionProcessor.setRoom(room);
    Message<String> gameStart = new Message<>("GAME_START", null);
    broadcast(gameStart, null);
  }

  public synchronized void offerSurrender(String username) {
    currentSurrenderOffer.add(username);
    broadcastPlayerSurrender(username);
    if (currentSurrenderOffer.size() == players.size()) {
      nextRoundWithSurrender();
    }
  }

  public synchronized void startFinalRound(List<String> finalists) {
    finalRoundPlayers.clear();
    for (String u : finalists) {
      finalRoundPlayers.add(u);
    }
    finalRoundActive = true;
    randomizeCards();
    Message<List<String>> msg = new Message<>("FINAL_ROUND", new ArrayList<>(finalists));
    broadcast(msg, null);
  }

  public synchronized void endFinalRound() {
    finalRoundPlayers.clear();
    finalRoundActive = false;
  }

  public boolean isFinalRoundActive() {
    return finalRoundActive;
  }

  public boolean isFinalRoundPlayer(String username) {
    if (username == null) return false;
    return finalRoundPlayers.contains(username);
  }

  public void setLeaderboard() {
    if (game.getLeaderboard() != null && !game.getLeaderboard().isEmpty()) {
      return;
    }

    List<PlayerLeaderboard> leaderboard = new ArrayList<>();
    for (Player p : players) {
      PlayerLeaderboard entry = new PlayerLeaderboard(p.getId(), p.getName());
      leaderboard.add(entry);
    }
    game.setLeaderboard(leaderboard);
  }

  public void addPointToPlayer(String username) {
    if (game.getLeaderboard() == null || game.getLeaderboard().isEmpty()) {
      setLeaderboard();
    }
    List<PlayerLeaderboard> leaderboard = game.getLeaderboard();
    if (leaderboard == null)
      return;

    for (PlayerLeaderboard entry : leaderboard) {
      if (entry.getName().equals(username)) {
        entry.addScore();
      }
    }

    Message<List<PlayerLeaderboard>> leaderboards = new Message<>("LEADERBOARD_UPDATE", game.getLeaderboard());
    broadcast(leaderboards, null);
  }

  private synchronized void handleNewClient(Socket clientSocket) {
    String clientUsername;
    try {
      BufferedReader socketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      clientUsername = socketIn.readLine();
      if (clientUsername == null)
        return;
    } catch (IOException e) {
      LoggerHandler.logError("Error reading client username.", e);
      return;
    }

    boolean isHost = players.isEmpty();
    Player newPlayer = new Player(clientUsername, isHost);
    ClientHandler handler = new ClientHandler(clientSocket, this, clientUsername);

    players.add(newPlayer);
    clients.add(handler);

    sendPlayerListToClient(handler);
    sendRoomInfoToClient(handler);
    broadcastPlayerEvent("CONNECTED", clientUsername, handler);
    broadcastPlayerList(handler);

    handler.start();
    System.out.println("CLIENT JOIN = " + clientUsername + (isHost ? " (HOST)" : ""));
    // TODO: remove this debug
  }

  public void enqueueAnswer(GameAnswer answer) {
    if (submissionProcessor != null)
      submissionProcessor.enqueue(answer);
  }

  public Game getGame() {
    return game;
  }

  public void broadcastMessage(Message<?> message) {
    broadcast(message, null);
  }

  public void sendToClient(String username, Message<?> message) {
    String json = gson.toJson(message);
    for (ClientHandler handler : clients) {
      if (handler != null && handler.isConnected() && handler.usernameEquals(username)) {
        handler.sendMessage(json);
        return;
      }
    }
  }

  public synchronized boolean unregisterClient(String username, ClientHandler handler) {
    boolean removedHandler = clients.remove(handler);

    Player disconnectedPlayer = players.stream()
        .filter(p -> p.getName().equals(username))
        .findFirst()
        .orElse(null);

    boolean wasHost = disconnectedPlayer != null && disconnectedPlayer.isHost();
    Boolean removedUser = players.removeIf(p -> p.getName().equals(username));

    if (removedUser) {
      broadcastPlayerEvent("DISCONNECTED", username, null);
      broadcastPlayerList(null);

      if (wasHost) {
        System.out.println("HOST DISCONNECTED - Stopping server...");
        stopServer();
      }
    }
    return removedHandler || removedUser;
  }

  public void nextRound() {
    randomizeCards();
    Message<String> gameStart = new Message<>("NEXT_ROUND", null);
    broadcast(gameStart, null);
  }

  public void roundOver(String winner) {
    Message<String> gameStart = new Message<>("ROUND_OVER", "THE WINNER IS " + winner);
    broadcast(gameStart, null);
  }

  public void randomizeCards() {
    List<Integer> cards = new ArrayList<>();
    for (int x = 0; x < room.getTotalCard(); x++) {
      cards.add((int) (Math.random() * 10));
    }
    game.setCards(cards);
    game.setX((int) (Math.random() * 26) + 15);

    Message<Game> dataCards = new Message<>("CARDS_BROADCAST", game);
    broadcast(dataCards, null);
  }

  private void broadcast(Message<?> message, ClientHandler exclude) {
    String json = gson.toJson(message);
    for (ClientHandler handler : clients) {
      if (handler != exclude) {
        handler.sendMessage(json);
      }
    }
  }

  private void sendPlayerListToClient(ClientHandler client) {
    List<Player> snapshot = new ArrayList<>(players);
    Message<List<Player>> msg = new Message<>("PLAYER_LIST", snapshot);
    String json = gson.toJson(msg);
    client.sendMessage(json);
  }

  private void sendRoomInfoToClient(ClientHandler client) {
    Message<Room> msg = new Message<>("SETTING_UPDATE", room);
    String json = gson.toJson(msg);
    client.sendMessage(json);
  }

  private void broadcastPlayerList(ClientHandler exclude) {
    List<Player> snapshot = new ArrayList<>(players);
    Message<List<Player>> msg = new Message<>("PLAYER_LIST", snapshot);
    broadcast(msg, exclude);
  }

  private void broadcastPlayerEvent(String eventType, String username, ClientHandler exclude) {
    PlayerEvent event = new PlayerEvent(eventType, username);
    Message<PlayerEvent> msg = new Message<>("PLAYER_EVENT", event);
    broadcast(msg, exclude);
  }

  private synchronized void broadcastPlayerSurrender(String username) {
    PlayerSurrender playerSurrender = new PlayerSurrender(username, "Surrend ("+currentSurrenderOffer.size()+"/"+players.size()+")");
    Message<PlayerSurrender> msg = new Message<>("PLAYER_SURRENDER", playerSurrender);
    broadcast(msg, null);
  }

  private void nextRoundWithSurrender() {
    currentSurrenderOffer.clear();
    randomizeCards();
    Message<String> gameStart = new Message<>("NEXT_ROUND_WITH_SURRENDER", "PLAYER CHOOSE TO SURRENDER");
    broadcast(gameStart, null);
  }

  private void broadcastSettinsEvent(ClientHandler exclude) {
    Message<Room> msg = new Message<>("SETTING_UPDATE", room);
    broadcast(msg, exclude);
  }
}
