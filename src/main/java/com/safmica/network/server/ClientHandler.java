package com.safmica.network.server;

import com.safmica.listener.RoomListener;
import com.safmica.utils.LoggerHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final TcpServerHandler server;
    private Socket client;
    private List<RoomListener> listeners;
    private String username;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket client, TcpServerHandler server, List<RoomListener> listeners, String username) {
        this.client = client;
        this.server = server;
        this.listeners = listeners;
        this.username = username;
        try {
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.out = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            LoggerHandler.logError("Error initializing streams for client: " + client.getInetAddress().getHostAddress(),e);
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                if ("EXIT".equalsIgnoreCase(inputLine)) {
                    break;
                }
            }

        } catch (java.net.SocketException e) {
            System.out.println("INFO: Client " + username + " disconnected abruptly.");
        } catch (IOException e) {
            LoggerHandler.logError("Client " + username + " connection error.", e);
        } finally {
            cleanupConnection();
        }
    }
    
    private void cleanupConnection() {
        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            LoggerHandler.logError("Error closing client socket for " + username, e);
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }
        
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
        }

        try {
            server.unregisterClient(username, this);
        } catch (Exception e) {
            LoggerHandler.logError("Error unregistering client " + username, e);
        }
    }

    public void sendMessage(String message) {
        try {
            if (out != null && !client.isClosed()) {
                out.println(message);
                out.flush();
            }
        } catch (Exception e) {
            //todo: give some handle
        }
    }
    
    public boolean isConnected() {
        return client != null && !client.isClosed() && client.isConnected();
    }
}