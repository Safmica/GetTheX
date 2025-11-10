package com.safmica.network.server;

import com.safmica.listener.ClientConnectionListener;
import com.safmica.utils.LoggerHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private Socket server;
    private List<ClientConnectionListener> listeners;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket server, List<ClientConnectionListener> listeners) {
        this.server = server;
        this.listeners = listeners;
        try {
            this.in = new BufferedReader(new InputStreamReader(server.getInputStream()));
            this.out = new PrintWriter(server.getOutputStream(), true);
        } catch (IOException e) {
            LoggerHandler.logError("Error initializing streams for client: " + server.getInetAddress().getHostAddress(),e);
        }
    }

    @Override
    public void run() {
        String clientAddress = server.getInetAddress().getHostAddress();
        LoggerHandler.logInfoMessage("Starting communication thread for client: " + clientAddress);

        try {
            String inputLine;
            out.println("Welcome to the Game Room!");

            while ((inputLine = in.readLine()) != null) {
                LoggerHandler.logInfoMessage("Received from " + clientAddress + ": " + inputLine);

                String response = "Server received: " + inputLine.toUpperCase();
                out.println(response);

                if ("EXIT".equalsIgnoreCase(inputLine)) {
                    break;
                }
            }

        } catch (IOException e) {
            LoggerHandler.logError("Client " + clientAddress + " connection abruptly closed.", e);
        } finally {
            ;
            try {
                server.close();
                LoggerHandler.logInfoMessage("Client " + clientAddress + " disconnected.");
            } catch (IOException e) {
                LoggerHandler.logError("Error closing client socket for " + clientAddress, e);
            }
            for (ClientConnectionListener l : listeners) {
                try {
                    l.onClientDisconnected(clientAddress);
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.println(message);
            out.flush();
        } catch (Exception e) {
        }
    }
}