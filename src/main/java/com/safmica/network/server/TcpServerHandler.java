package com.safmica.network.server;

import com.google.gson.Gson;
import com.safmica.listener.ClientConnectionListener;
import com.safmica.model.ClientConnectedMessage;
import com.safmica.model.Message;
import com.safmica.utils.LoggerHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServerHandler extends Thread {

  private ServerSocket serverSocket;
  private int port;
  private Gson gson = new com.google.gson.Gson();
  private boolean isRunning = true;
  private BufferedReader in;
  private PrintWriter out;
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
    String clientUsername = null;
    try {
      clientUsername = in.readLine();
    } catch (IOException e) {
      LoggerHandler.logError("Error reading client username.", e);
      return;
    }
    for (ClientConnectionListener l : listeners) {
      try {
        l.onClientConnected(clientUsername);
      } catch (Exception ignore) {
      }
    }

    ClientHandler handler = new ClientHandler(clientSocket, listeners);
    clients.add(handler);

    handler.start();
    broadcastClientConnected(clientUsername);
  }

  @Override
  public void run() {
    while (isRunning) {
      try {
        Socket clientSocket = serverSocket.accept();
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
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

  public void stopClient(Socket clientSocket) {
    this.isRunning = false;
    try {
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
      if (clientSocket != null && !clientSocket.isClosed()) {
        clientSocket.close();
      }
    } catch (IOException e) {
      LoggerHandler.logError("Error closing Client socket.", e);
    }
  }
}
