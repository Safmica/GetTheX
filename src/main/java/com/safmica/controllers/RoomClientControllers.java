package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.List;

import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.utils.ui.PlayerListCell;

public class RoomClientControllers implements RoomListener {

    @FXML
    private ListView<Player> playersList;

    private TcpClientHandler client;
    private int port;
    private String host;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    public void setServerSocket (String host, int port) {
        this.port = port;
        this.host = host;
    }

    @FXML
    private void initialize() {
        playersList.setItems(players);
        playersList.setCellFactory(listView -> new PlayerListCell());
    }

    public void startClient(String username) {
        if (client != null) return;
        client = new TcpClientHandler(host, port);
        client.addRoomListener(this);
        client.startClient(username);
    }

    private void stopClient() {
        if (client == null) return;
        client.stopClient();
        client = null;
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
            // TODO: Add some notify ui
        });
    }
    
    @Override
    public void onPlayerDisconnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " left the room");
            // TODO: Add some notify ui
        });
    }
}
