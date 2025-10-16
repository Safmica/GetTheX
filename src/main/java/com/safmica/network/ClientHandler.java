package com.safmica.network;

import com.safmica.utils.LoggerHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        LoggerHandler.logInfoMessage("Starting communication thread for client: " + clientAddress);
        
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
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
        } finally {;
            try {
                clientSocket.close();
                LoggerHandler.logInfoMessage("Client " + clientAddress + " disconnected.");
            } catch (IOException e) {
                LoggerHandler.logError("Error closing client socket for " + clientAddress, e);
            }
        }
    }
}