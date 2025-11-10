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

import com.safmica.network.ClientConnectionListener;
import com.safmica.network.server.TcpServerHandler;

public class RoomServerControllers implements ClientConnectionListener {

    @FXML
    private ListView<String> usersList;

    private TcpServerHandler server;
    private int port;
    private final ObservableList<String> users = FXCollections.observableArrayList();

    public void setPort (int port) {
        this.port = port;
    }

    @FXML
    private void initialize() {
        usersList.setItems(users);
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

    private void stopServer() {
        if (server == null) return;
        server.stopServer();
        server = null;
    }
    
    @Override
    public void onClientConnected(String clientId) {
        Platform.runLater(() -> {
            if (!users.contains(clientId)) {
                users.add(clientId);
                
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
        Platform.runLater(() -> users.remove(clientId));
    }
}
