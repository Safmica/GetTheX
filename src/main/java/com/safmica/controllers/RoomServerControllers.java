package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.IOException;

import com.safmica.listener.ClientConnectionListener;
import com.safmica.listener.PlayerListener;
import com.safmica.model.Player;
import com.safmica.network.server.TcpServerHandler;

public class RoomServerControllers implements ClientConnectionListener, PlayerListener {

    @FXML
    private ListView<Player> playersList;
    private Player host;
    private TcpServerHandler server;
    private int port;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    public void setPort (int port) {
        this.port = port;
    }

    @FXML
    private void initialize() {
        playersList.setItems(players);
    }

    public void setHost(String name) {
        host = new Player(name, true);
        players.add(host);
    }

    public void onPlayerAdded (String name) {
        Player ply = new Player(name, false);
        Platform.runLater(() -> {
            players.add(ply);
        });
    }

    public void onPlayerRemoved (String id) {
        Platform.runLater(() -> {
            players.removeIf(p -> p.getId().equals(id));
        });
    }

    public void startServer() {
        if (server != null) return;
        server = new TcpServerHandler(port);
        server.addClientConnectionListener(this);

        try {
            server.startServer();
        } catch (IOException ex) {
        }
    }

    // private void stopServer() {
    //     if (server == null) return;
    //     server.stopServer();
    //     server = null;
    // }
    
    @Override
    public void onClientConnected(String name) {
        Platform.runLater(() -> {
            onPlayerAdded(name);
        });
    }

    @Override
    public void onClientDisconnected(String clientId) {
        Platform.runLater(() -> players.remove(clientId));
    }
}
