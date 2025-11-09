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
import com.safmica.network.client.TcpClientHandler;

public class RoomClientControllers implements ClientConnectionListener {

    @FXML
    private ListView<String> usersList;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Label statusLabel;

    private TcpClientHandler client;
    private final int port;
    private final String host;
    private final ObservableList<String> users = FXCollections.observableArrayList();

    public RoomClientControllers (String host, int port) {
        this.port = port;
        this.host = host;
    }

    @FXML
    private void initialize() {
        usersList.setItems(users);

        startBtn.setOnAction(e -> startClient());
        stopBtn.setOnAction(e -> stopClient());

        stopBtn.setDisable(true);
    }

    public void startClient() {
        if (client != null) return;
        client = new TcpClientHandler(host, port);
        client.addClientConnectionListener(this);

        try {
            client.startClient();
            statusLabel.setText("Client running on port " + port);
            startBtn.setDisable(true);
            stopBtn.setDisable(false);
        } catch (IOException ex) {
            statusLabel.setText("Failed to start Client: " + ex.getMessage());
        }
    }

    private void stopClient() {
        if (client == null) return;
        client.stopClient();
        client = null;
        statusLabel.setText("Client stopped");
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
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
                alert.showAndWait();  // Tampilkan popup dan tunggu user close
            }
        });
    }

    @Override
    public void onClientDisconnected(String clientId) {
        Platform.runLater(() -> users.remove(clientId));
    }
}
