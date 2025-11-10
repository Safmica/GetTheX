package com.safmica.network.server;

import com.google.gson.Gson;
import com.safmica.model.ClientConnectedMessage;
import com.safmica.model.Message;
import com.safmica.network.ClientConnectionListener;
import com.safmica.utils.LoggerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServerHandler extends Thread {

  private ServerSocket serverSocket;
  private int port;
  private Gson gson = new com.google.gson.Gson();
  private boolean isRunning = true;
  private final List<ClientConnectionListener> listeners = new CopyOnWriteArrayList<>();
  private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

  public TcpServerHandler(int port) {
    this.port = port;
  }

  public void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    LoggerHandler.logInfoMessage("Server is running on port " + port);
    this.start();
  }

  public void addClientConnectionListener(ClientConnectionListener l) {
    listeners.add(l);
  }

  public void removeClientConnectionListener(ClientConnectionListener l) {
    listeners.remove(l);
  }

  private void handleNewClient(Socket clientSocket) {
    String clientId = clientSocket.getInetAddress().getHostAddress();
    for (ClientConnectionListener l : listeners) {
      try {
        l.onClientConnected(clientId);
      } catch (Exception ignore) {
      }
    }

    ClientHandler handler = new ClientHandler(clientSocket, listeners);
    clients.add(handler);

    handler.start();
    broadcastClientConnected(clientId);
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
          LoggerHandler.logInfoMessage("Server stopped.");
        }
      }
    }
  }

  private void broadcastClientConnected(String clientId) {
    ClientConnectedMessage broadcastClientConnected = new ClientConnectedMessage();
    broadcastClientConnected.clientId = clientId;
    Message<ClientConnectedMessage> msg = new Message<>("CONNECTED", broadcastClientConnected);
    String json = gson.toJson(msg);
    for (ClientHandler handler : clients) {
      handler.sendMessage(json);
    }
  }

  public void stopServer() {
    this.isRunning = false;
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (IOException e) {
      LoggerHandler.logError("Error closing server socket.", e);
    }
  }
}
