package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.util.List;

import com.safmica.App;
import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.TcpServerHandler;
import com.safmica.utils.LoggerHandler;
import com.safmica.utils.ui.PlayerListCell;

public class RoomController implements RoomListener {

    @FXML
    private ListView<Player> playersList;
    
    private TcpClientHandler clientHandler;
    private TcpServerHandler server;
    private boolean isHost;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        playersList.setItems(players);
        playersList.setCellFactory(listView -> new PlayerListCell());
    }

    public void initAsHost(int port, String username) {
        this.isHost = true;
        server = new TcpServerHandler(port);

        try {
            server.startServer();
            System.out.println("DEBUG : SERVER START ON PORT " + port);
            
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                    connectAsClient("localhost", port, username);
                    System.out.println("DEBUG : HOST CONNECTED AS CLIENT");
                } catch (Exception e) {
                    LoggerHandler.logError("Error connecting host as client", e);
                }
            }).start();
            
        } catch (IOException ex) {
            LoggerHandler.logError("Error starting server", ex);
        }
    }

    public void initAsClient(String host, int port, String username) {
        this.isHost = false;
        connectAsClient(host, port, username);
    }

    private void connectAsClient(String host, int port, String username) {
        clientHandler = new TcpClientHandler(host, port, username);
        clientHandler.addRoomListener(this);
        clientHandler.startClient();
    }

    private void cleanup() {
        if (clientHandler != null) {
            clientHandler.stopClient();
            clientHandler = null;
        }
        
        if (server != null) {
            server.stopServer();
            server = null;
        }
    }

    @Override
    public void onPlayerListChanged(List<Player> serverPlayers) {
        Platform.runLater(() -> {
            players.setAll(serverPlayers);
        });
    }

    @Override
    public void onPlayerConnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " joined the room");
            // TODO: Add some notify ui lol
        });
    }

    @Override
    public void onPlayerDisconnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " left the room");

            Player disconnectedPlayer = players.stream()
                .filter(p -> p.getName().equals(username) && p.isHost())
                .findFirst()
                .orElse(null);
            
            if (disconnectedPlayer != null && !isHost) {
                System.out.println("Host disconnected - leaving room...");
                cleanup();
                
                try {
                    App.setRoot("menu");
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Menu", e);
                }
            }
            
            // TODO: Add some notify ui lol
        });
    }
    
    @FXML
    private void handleExit() {
        cleanup();
        
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            
            Platform.runLater(() -> {
                try {
                    App.setRoot("menu");
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Menu", e);
                }
            });
        }).start();
    }
}
