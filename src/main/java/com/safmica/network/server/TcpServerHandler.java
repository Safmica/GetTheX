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
import com.safmica.model.SaveState;
import com.safmica.utils.AutosaveUtil;
import com.safmica.utils.LoggerHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServerHandler extends Thread {

  private ServerSocket serverSocket;
  private int port;
  private Gson gson = new com.google.gson.Gson();
  private boolean isRunning = true;
  private Room room;
  private Game game;
  private SubmissionProcessor submissionProcessor;
  private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
  private final List<Player> players = new CopyOnWriteArrayList<>();
  private volatile boolean reconnectMode = false;
  private final List<String> allowedReconnectIds = new CopyOnWriteArrayList<>();
  private final String TOTAL_CARD = "TOTAL_CARD";
  private final String TOTAL_ROUND = "TOTAL_ROUND";
  private final String PLAYER_LIMIT = "PLAYER_LIMIT";
  private List<String> currentSurrenderOffer = new CopyOnWriteArrayList<>();

  private volatile boolean finalRoundActive = false;
  private final List<String> finalRoundPlayers = new CopyOnWriteArrayList<>();

  public TcpServerHandler(int port) {
    this.port = port;
  }

  public void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    room = new Room(4, 3);

    submissionProcessor = new SubmissionProcessor(this);
    try {
      Optional<SaveState> maybe = AutosaveUtil.read();
      if (maybe.isPresent()) {
        SaveState s = maybe.get();
        if (s.isInProgress() && s.getPlayers() != null && !s.getPlayers().isEmpty()) {
          reconnectMode = true;
          players.clear();
          players.addAll(s.getPlayers());
          for (Player p : s.getPlayers()) {
            if (p != null && p.getId() != null)
              allowedReconnectIds.add(p.getId());
          }
          if (s.getRoom() != null)
            room = s.getRoom();
          if (s.getGame() != null)
            game = s.getGame();
          LoggerHandler.logInfoMessage(
              "Server started in RECONNECT mode. Allowed reconnect players: " + allowedReconnectIds.size());
        }
      }
    } catch (Exception e) {
      LoggerHandler.logError("Error loading autosave during server start.", e);
    }

    this.start();
  }

  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
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
      case PLAYER_LIMIT:
        room.setPlayerLimit(option);
        break;
      default:
        break;
    }
    broadcastSettinsEvent(null);
  }

  public void startGame() {
    game = new Game();
    submissionProcessor.setRoom(room);
    submissionProcessor.setGame(game);
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

    try {
      SaveState save = new SaveState(room, game, new ArrayList<>(players), game != null ? game.getLeaderboard() : null,
          true, port);
      boolean ok = AutosaveUtil.writeAtomic(save);
      if (!ok)
        LoggerHandler.logError("Autosave write failed at startFinalRound.", null);
    } catch (Exception e) {
      LoggerHandler.logError("Failed creating/writing autosave in startFinalRound.", e);
    }
  }

  public synchronized void endFinalRound() {
    finalRoundPlayers.clear();
    finalRoundActive = false;
  }

  public boolean isFinalRoundActive() {
    return finalRoundActive;
  }

  public boolean isFinalRoundPlayer(String username) {
    if (username == null)
      return false;
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
    String requestedUsername;
    try {
      DataInputStream socketIn = new DataInputStream(clientSocket.getInputStream());
      int length;
      try {
        length = socketIn.readInt();
      } catch (java.io.EOFException eof) {
        try {
          clientSocket.close();
        } catch (IOException ignored) {
        }
        return;
      } catch (IOException e) {
        LoggerHandler.logError("Error reading username length from client.", e);
        try {
          clientSocket.close();
        } catch (IOException ignored) {
        }
        return;
      }

      if (length <= 0) {
        return;
      }

      byte[] buf = new byte[length];
      socketIn.readFully(buf);
      requestedUsername = new String(buf, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LoggerHandler.logError("Error reading client username.", e);
      try {
        clientSocket.close();
      } catch (IOException ignored) {
      }
      return;
    }

    boolean gameInProgress = false;
    java.util.Optional<SaveState> maybeSave = AutosaveUtil.read();
    if (maybeSave.isPresent() && maybeSave.get().isInProgress()) {
      gameInProgress = true;

      SaveState save = maybeSave.get();
      boolean usernameExists = false;
      if (save.getPlayers() != null) {
        for (Player p : save.getPlayers()) {
          if (p != null && p.getName() != null && p.getName().equalsIgnoreCase(requestedUsername)) {
            usernameExists = true;
            break;
          }
        }
      }

      if (!usernameExists) {
        try {
          DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
          Message<String> rejectMsg = new Message<>("JOIN_REJECTED",
              "Game in progress. Only previous players can rejoin.");
          String json = gson.toJson(rejectMsg);
          byte[] data = json.getBytes(StandardCharsets.UTF_8);
          out.writeInt(data.length);
          out.write(data);
          out.flush();
          clientSocket.close();
          LoggerHandler
              .logInfoMessage("Rejected join from '" + requestedUsername + "' - not a previous player in active game.");
          return;
        } catch (IOException ex) {
          LoggerHandler.logError("Error sending rejection message.", ex);
        }
        return;
      }
    }

    if (reconnectMode || gameInProgress) {
      Player existing = players.stream()
          .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(requestedUsername))
          .findFirst()
          .orElse(null);
      if (existing != null) {
        ClientHandler handler = new ClientHandler(clientSocket, this, existing.getName());
        clients.add(handler);

        sendPlayerListToClient(handler);
        sendRoomInfoToClient(handler);

        broadcastPlayerEvent("RECONNECTED", existing.getName(), handler);
        broadcastPlayerList(handler);
        handler.start();
        System.out.println("CLIENT RECONNECT = " + existing.getName());
        return;
      }
    }

    boolean duplicate = players.stream().anyMatch(p -> p.getName().equalsIgnoreCase(requestedUsername));
    String assignedName = requestedUsername;
    if (duplicate) {
      assignedName = "Player" + ((int) (Math.random() * 9000) + 1000);
    }

    if (game != null) {
      try {
        Message<String> fullMsg = new Message<>("ROOM_FULL", "Game in progress");
        String json = gson.toJson(fullMsg);
        java.io.DataOutputStream dos = new java.io.DataOutputStream(clientSocket.getOutputStream());
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
      } catch (Exception ignored) {}
      try { clientSocket.close(); } catch (IOException ignored) {}
      return;
    }

    if (players.size() >= room.getPlayerLimit()) {
      try {
        Message<String> fullMsg = new Message<>("ROOM_FULL", "Room is full");
        String json = gson.toJson(fullMsg);
        java.io.DataOutputStream dos = new java.io.DataOutputStream(clientSocket.getOutputStream());
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
      } catch (Exception ignored) {}
      try { clientSocket.close(); } catch (IOException ignored) {}
      return;
    }

    boolean isHost = players.isEmpty();
    Player newPlayer = new Player(assignedName, isHost);
    ClientHandler handler = new ClientHandler(clientSocket, this, assignedName);

    players.add(newPlayer);
    clients.add(handler);

    sendPlayerListToClient(handler);
    sendRoomInfoToClient(handler);

    if (duplicate) {
      Message<String> dupMsg = new Message<>("DUPLICATE_USERNAME", assignedName);
      String dupJson = gson.toJson(dupMsg);
      handler.sendMessage(dupJson);
    }

    broadcastPlayerEvent("CONNECTED", assignedName, handler);
    broadcastPlayerList(handler);

    handler.start();
    System.out.println("CLIENT JOIN = " + assignedName + (isHost ? " (HOST)" : ""));
  }

  public synchronized boolean acceptReconnect(ClientHandler handler, String playerId) {
    if (!reconnectMode)
      return false;
    if (playerId == null || playerId.trim().isEmpty())
      return false;
    String pid = playerId.trim();
    if (!allowedReconnectIds.contains(pid))
      return false;

    Player saved = players.stream().filter(p -> p.getId() != null && p.getId().equals(pid)).findFirst().orElse(null);
    if (saved == null)
      return false;

    handler.setUsername(saved.getName());
    clients.add(handler);

    sendPlayerListToClient(handler);
    sendRoomInfoToClient(handler);

    broadcastPlayerEvent("RECONNECTED", saved.getName(), handler);
    broadcastPlayerList(handler);
    
    allowedReconnectIds.remove(saved.getId());
    return true;
  }

  public void enqueueAnswer(GameAnswer answer) {
    if (submissionProcessor != null)
      submissionProcessor.enqueue(answer);
  }

  public Game getGame() {
    return game;
  }

  public boolean isReconnectMode() {
    return reconnectMode;
  }

  public List<String> getAllowedReconnectIds() {
    return new ArrayList<>(allowedReconnectIds);
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
      currentSurrenderOffer.removeIf(u -> u != null && u.equals(username));
      if (!currentSurrenderOffer.isEmpty()) {
        for (String u : new ArrayList<>(currentSurrenderOffer)) {
          broadcastPlayerSurrender(u);
        }
        if (currentSurrenderOffer.size() == players.size()) {
          nextRoundWithSurrender();
        }
      }
      if (finalRoundActive) {
        finalRoundPlayers.removeIf(u -> u != null && u.equals(username));
        if (finalRoundPlayers.isEmpty()) {
          endFinalRound();
        }
      }

      if (game != null && game.getLeaderboard() != null) {
        List<PlayerLeaderboard> leaderboard = game.getLeaderboard();
        boolean changed = leaderboard.removeIf(entry -> entry != null && username != null && username.equals(entry.getName()));
        if (changed) {
          Message<List<PlayerLeaderboard>> leaderboards = new Message<>("LEADERBOARD_UPDATE", leaderboard);
          broadcast(leaderboards, null);
        }
      }

      broadcastPlayerEvent("DISCONNECTED", username, null);
      broadcastPlayerList(null);

      if (wasHost) {
        System.out.println("HOST DISCONNECTED - Stopping server...");
        stopServer();
      }
    }
    return removedHandler || removedUser;
  }

  public synchronized boolean changeUsername(ClientHandler handler, String newName) {
    if (newName == null || newName.trim().isEmpty())
      return false;
    String candidate = newName.trim();

    if (handler.getUsername() != null && handler.getUsername().equalsIgnoreCase(candidate)) {
      return true;
    }

    boolean exists = players.stream()
        .anyMatch(p -> p.getName() != null && p.getName().equalsIgnoreCase(candidate));
    if (exists) {
      return false;
    }

    Player player = players.stream()
        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(handler.getUsername()))
        .findFirst()
        .orElse(null);

    if (player != null) {
      player.setName(candidate);
    }

    handler.setUsername(candidate);

    broadcastPlayerList(null);
    return true;
  }

  public void nextRound() {
    randomizeCards();
    Message<String> gameStart = new Message<>("NEXT_ROUND", null);
    broadcast(gameStart, null);

    try {
      SaveState save = new SaveState(room, game, new ArrayList<>(players), game != null ? game.getLeaderboard() : null,
          true, port);
      boolean ok = AutosaveUtil.writeAtomic(save);
      if (!ok)
        LoggerHandler.logError("Autosave write failed at nextRound.", null);
    } catch (Exception e) {
      LoggerHandler.logError("Failed creating/writing autosave in nextRound.", e);
    }
  }

  public void roundOver(String winner) {
    Message<String> gameStart = new Message<>("ROUND_OVER", "THE WINNER IS " + winner);
    broadcast(gameStart, null);

    try {
      boolean ok = AutosaveUtil.delete();
      if (!ok)
        LoggerHandler.logError("Failed to delete autosave on roundOver.", null);
    } catch (Exception e) {
      LoggerHandler.logError("Failed deleting autosave in roundOver.", e);
    }
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

    try {
      SaveState save = new SaveState(room, game, new ArrayList<>(players), game != null ? game.getLeaderboard() : null,
          true, port);
      boolean ok = AutosaveUtil.writeAtomic(save);
      if (!ok) {
        LoggerHandler.logError("Autosave write failed at startGame.", null);
      }
    } catch (Exception e) {
      LoggerHandler.logError("Failed creating/writing autosave in startGame.", e);
    }
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
    PlayerSurrender playerSurrender = new PlayerSurrender(username,
        "Surrend (" + currentSurrenderOffer.size() + "/" + players.size() + ")");
    Message<PlayerSurrender> msg = new Message<>("PLAYER_SURRENDER", playerSurrender);
    broadcast(msg, null);
  }

  private void nextRoundWithSurrender() {
    currentSurrenderOffer.clear();
    randomizeCards();
    Message<String> gameStart = new Message<>("NEXT_ROUND_WITH_SURRENDER", "PLAYER CHOOSE TO SURRENDER");
    broadcast(gameStart, null);

    try {
      SaveState save = new SaveState(room, game, new ArrayList<>(players), game != null ? game.getLeaderboard() : null,
          true, port);
      boolean ok = AutosaveUtil.writeAtomic(save);
      if (!ok)
        LoggerHandler.logError("Autosave write failed at nextRoundWithSurrender.", null);
    } catch (Exception e) {
      LoggerHandler.logError("Failed creating/writing autosave in nextRoundWithSurrender.", e);
    }
  }

  private void broadcastSettinsEvent(ClientHandler exclude) {
    Message<Room> msg = new Message<>("SETTING_UPDATE", room);
    broadcast(msg, exclude);
  }
}
