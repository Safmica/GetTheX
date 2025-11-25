package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.IOException;

import com.safmica.App;
import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.network.server.TcpServerHandler;
import com.safmica.utils.LoggerHandler;
import com.safmica.utils.ui.PlayerListCell;

public class RoomServerControllers implements RoomListener {

    @FXML
    private ListView<Player> playersList;
    private TcpServerHandler server;
    private int port;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    public void setPort(int port) {
        this.port = port;
    }

    @FXML
    private void initialize() {
        playersList.setItems(players);
        playersList.setCellFactory(listView -> new PlayerListCell());
    }

    public void startServer(String username) {
        if (server != null)
            return;
        server = new TcpServerHandler(port, username, this);

        try {
            server.startServer();
            // TODO: remove this debug
            System.out.println("DEBUG : SERVER START ON PORT " + port);
        } catch (IOException ex) {
        }
    }

    private void stopServer() {
        if (server == null)
            return;
        server.stopServer();
        server = null;
    }

    @Override
    public void onPlayerListChanged(java.util.List<Player> serverPlayers) {
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
            // TODO: Add some notify ui lol
        });
    }
    
    @FXML
    private void handleExit() {
        stopServer();
        try {
            App.setRoot("menu");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logFXMLFailed("Menu", e);
        }
    }
}
