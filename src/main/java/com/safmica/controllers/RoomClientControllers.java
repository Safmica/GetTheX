package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;

import java.io.IOException;

import com.safmica.listener.ClientConnectionListener;
import com.safmica.network.client.TcpClientHandler;

public class RoomClientControllers implements ClientConnectionListener {

    @FXML
    private ListView<String> playersList;

    private TcpClientHandler client;
    private int port;
    private String host;
    private final ObservableList<String> players = FXCollections.observableArrayList();

    public void setServerSocket (String host, int port) {
        this.port = port;
        this.host = host;
    }

    @FXML
    private void initialize() {
        playersList.setItems(players);
    }

    public void startClient(String username) {
        if (client != null) return;
        client = new TcpClientHandler(host, port);
        client.addClientConnectionListener(this);
        client.startClient(username);
    }

    private void stopClient() {
        if (client == null) return;
        client.stopClient();
        client = null;
    }

    @Override
    public void onClientConnected(String clientId) {
        Platform.runLater(() -> {
            if (!players.contains(clientId)) {
                players.add(clientId);
                
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Client Connected");
                alert.setHeaderText(null);
                alert.setContentText("New client joined: " + clientId);
                alert.showAndWait();
            }
        });
    }

    @Override
    public void onClientDisconnected(String clientId) {
        Platform.runLater(() -> players.remove(clientId));
    }
}
