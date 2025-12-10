package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
import com.safmica.utils.ui.NotificationUtil;
import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

public class ClientController {
  @FXML
  private TextField usernameField;
  @FXML
  private TextField portField;
  @FXML
  private TextField ipAddressField;

  @FXML
  private void backToMenu() {
    try {
      App.setRoot("menu");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logFXMLFailed("Menu", e);
    }
  }

  @FXML
  private void joinRoom() {
    String usernameText = usernameField.getText();
    String portText = portField.getText();
    String ipText = ipAddressField.getText();
    try {
      int port = Integer.parseInt(portText);
      // Start client and stay on client.fxml; only navigate to room on successful acceptance
      com.safmica.network.client.TcpClientHandler client = new com.safmica.network.client.TcpClientHandler(ipText, port, usernameText);
      client.addRoomListener(new com.safmica.listener.RoomListener() {
        @Override
        public void onPlayerListChanged(java.util.List<com.safmica.model.Player> players) {}
        @Override
        public void onPlayerConnected(String username) {}
        @Override
        public void onPlayerDisconnected(String username) {}
        @Override
        public void onSettingChange(com.safmica.model.Room room) {
          // First successful room snapshot -> navigate to room scene
          try {
            URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            RoomController roomController = loader.getController();
            roomController.initAsClientWithHandler(client, usernameText);
            App.setRoot(root);
          } catch (IOException | IllegalStateException e) {
            LoggerHandler.logError("Failed to change to room scene.", e);
          }
        }
        @Override
        public void onGameStart() {}
        @Override
        public void onDuplicateUsernameAssigned(String assignedName) {}
        @Override
        public void onUsernameAccepted(String newName) {}
        @Override
        public void onConnectionError(String message) {
          // Show notification on the current client screen (client.fxml)
          try {
            javafx.scene.Scene scene = usernameField.getScene();
            if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
              NotificationUtil.showError((javafx.scene.layout.Pane) scene.getRoot(), message);
            } else {
              LoggerHandler.logErrorMessage(message);
            }
          } catch (Exception ex) {
            LoggerHandler.logErrorMessage(message);
          }
        }
      });
      client.startClient();
    } catch (NumberFormatException e) {
      LoggerHandler.logErrorMessage("Invalid port number format.");
    } catch (IllegalStateException e) {
      LoggerHandler.logError("Failed to create client or change scene.", e);
    }
  }
}
