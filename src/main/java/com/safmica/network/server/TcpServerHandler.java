package com.safmica.network.server;

import com.google.gson.Gson;
import com.safmica.model.Game;
import com.safmica.model.Message;
import com.safmica.model.Player;
import com.safmica.model.PlayerEvent;
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
  private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
  private final List<Player> players = new CopyOnWriteArrayList<>();
  private final String TOTAL_CARD = "TOTAL_CARD";
  private final String TOTAL_ROUND = "TOTAL_ROUND";

  public TcpServerHandler(int port) {
    this.port = port;
  }

  public void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    room = new Room(4, 3);
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
    Message<String> gameStart = new Message<>("GAME_START", null);
    broadcast(gameStart, null);
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

  public void randomizeCards() {
    List<Integer> cards = new ArrayList<>();
    for (int x = 0; x < room.getTotalCard(); x++) {
      cards.add((int) (Math.random() * 10));
    }
    game.setCards(cards);
    game.setX((int)(Math.random() * 26) + 15);
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

  private void broadcastSettinsEvent(ClientHandler exclude) {
    Message<Room> msg = new Message<>("SETTING_UPDATE", room);
    broadcast(msg, exclude);
  }
}
