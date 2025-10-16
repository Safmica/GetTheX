package com.safmica.network;

import com.safmica.utils.LoggerHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServerManager extends Thread {

  private ServerSocket serverSocket;
  private int port;
  private boolean isRunning = true;

  public TcpServerManager(int port) {
    this.port = port;
  }

  public void startServer() throws IOException {
    serverSocket = new ServerSocket(port);
    LoggerHandler.logInfoMessage("Server is running on port " + port);
    this.start();
  }

  @Override
  public void run() {
    while (isRunning) {
      try {
        Socket clientSocket = serverSocket.accept();

        LoggerHandler.logInfoMessage(
          "Client connected: " + clientSocket.getInetAddress().getHostAddress()
        );

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

  private void handleNewClient(Socket clientSocket) {
    ClientHandler handler = new ClientHandler(clientSocket);

    handler.start();
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
