package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.LoggerHandler;
import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

import com.safmica.network.server.TcpServerHandler;
import com.safmica.utils.ui.NotificationUtil;
import javafx.scene.layout.Pane;
public class ServerController {

    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;

    @FXML
    private void backToMenu() {
        try {
            App.setRoot("menu");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logFXMLFailed("Menu", e);
        }
    }

    @FXML
    private void createTCPServer() {
        String username = usernameField.getText();
        String portText = portField.getText();

        try {
            int port = Integer.parseInt(portText);
            TcpServerHandler server = new TcpServerHandler(port);
            try {
                server.startServer();
            } catch (IOException ex) {
                String msg = "Failed to start server: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage());
                try {
                    Platform.runLater(() -> {
                        Pane parent = null;
                        try {
                            if (portField.getScene() != null && portField.getScene().getRoot() instanceof Pane) {
                                parent = (Pane) portField.getScene().getRoot();
                            }
                        } catch (Exception ignored) {}
                        if (parent != null) {
                            NotificationUtil.showError(parent, msg);
                        } else {
                            System.out.println(msg);
                        }
                    });
                } catch (Exception ignored) {}
                return;
            }

            URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            RoomController roomController = loader.getController();
            roomController.initAsHost(server, port, username);
            App.setRoot(root);
        } catch (NumberFormatException e) {
            LoggerHandler.logErrorMessage("Invalid port number format.");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logError("Failed to create server or change scene.", e);
        }
    }
}