package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.LoggerHandler;
import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

public class ServerControllers {

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

            URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            RoomController roomController = loader.getController();
            roomController.initAsHost(port, username);
            App.setRoot(root);
        } catch (NumberFormatException e) {
            LoggerHandler.logErrorMessage("Invalid port number format.");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logError("Failed to create server or change scene.", e);
        }
    }
}