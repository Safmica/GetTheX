package com.safmica.controllers;

import com.safmica.*;
import com.safmica.network.*;
import com.safmica.utils.LoggerHandler;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ServerControllers {

    @FXML
    private TextField portField;
    private RoomServerControllers room;

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
        String portText = portField.getText();

        try {
            int port = Integer.parseInt(portText);

            room = new RoomServerControllers(port);

            room.startServer();

            App.setRoot("room_server");

        } catch (NumberFormatException e) {
            LoggerHandler.logErrorMessage("Invalid port number format.");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logError("Failed to create server or change scene.", e);
        }
    }
}