// filepath: c:\Users\Asus\Documents\vscode\gotthex\src\main\java\com\safmica\network\client\TcpClientHandler.java
package com.safmica.network.client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import com.safmica.network.ClientConnectionListener;
import com.safmica.utils.LoggerHandler;

public class TcpClientHandler extends Thread {
    private String host;
    private int port;
    private Socket client;
    private boolean isRunning = true;
    private BufferedReader in;
    private PrintWriter out;
    private List<ClientConnectionListener> listeners;

    public TcpClientHandler(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.out = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            LoggerHandler.logError("Error initializing streams for client: " + client.getInetAddress().getHostAddress(),
                    e);
        }
    }

    public void addClientConnectionListener(ClientConnectionListener l) {
        listeners.add(l);
    }

    public void removeClientConnectionListener(ClientConnectionListener l) {
        listeners.remove(l);
    }

    public void startClient() throws IOException {
        client = new Socket(host, port);
        LoggerHandler.logInfoMessage("Connect to " + host);
        this.start();
    }

    @Override
    public void run() {
        try {
            while (isRunning && client != null && !client.isClosed()) {
                String line = in.readLine();
                if (line == null)
                    break;
                if (listeners != null) {
                    for (ClientConnectionListener l : listeners) {
                        l.onClientConnected(line); // Atur sesuai kebutuhan
                    }
                }
            }
        } catch (IOException e) {
            LoggerHandler.logError("Error in client handler run loop.", e);
        }
    }

    public void stopClient() {
        this.isRunning = false;
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            LoggerHandler.logError("Error closing Client socket.", e);
        }
    }
}