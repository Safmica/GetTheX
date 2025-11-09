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

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Label statusLabel;

    private TcpServerHandler server;
    private final int port;
    private final ObservableList<String> users = FXCollections.observableArrayList();

    public RoomServerControllers (int port) {
        this.port = port;
    }

    @FXML
    private void initialize() {
        usersList.setItems(users);

        startBtn.setOnAction(e -> startServer());
        stopBtn.setOnAction(e -> stopServer());

        stopBtn.setDisable(true);
    }

    public void startServer() {
        if (server != null) return;
        server = new TcpServerHandler(port);
        server.addClientConnectionListener(this);

        try {
            server.startServer();
            statusLabel.setText("Server running on port " + port);
            startBtn.setDisable(true);
            stopBtn.setDisable(false);
        } catch (IOException ex) {
            statusLabel.setText("Failed to start server: " + ex.getMessage());
        }
    }

    private void stopServer() {
        if (server == null) return;
        server.stopServer();
        server = null;
        statusLabel.setText("Server stopped");
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
