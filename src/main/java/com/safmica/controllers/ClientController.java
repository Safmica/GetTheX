package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
import com.safmica.utils.ui.NotificationUtil;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.model.Room;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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
      TcpClientHandler client = new TcpClientHandler(ipText, port, usernameText);
      // Cache early events that may arrive before the Room scene/controller is loaded.
  final java.util.concurrent.atomic.AtomicReference<java.util.List<com.safmica.model.Player>> cachedPlayers = new java.util.concurrent.atomic.AtomicReference<>();
  final java.util.concurrent.atomic.AtomicReference<com.safmica.model.Room> cachedRoom = new java.util.concurrent.atomic.AtomicReference<>();
      client.addRoomListener(new RoomListener() {
        @Override
        public void onPlayerListChanged(List<Player> players) {
          // Cache the player list; RoomController will be attached after scene change.
          cachedPlayers.set(new java.util.ArrayList<>(players));
        }

        @Override
        public void onPlayerConnected(String username) {
        }

        @Override
        public void onPlayerDisconnected(String username) {
        }

        @Override
        public void onSettingChange(Room room) {
          try {
            cachedRoom.set(room);
            URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            RoomController roomController = loader.getController();
            roomController.initAsClientWithHandler(client, usernameText);
            List<Player> playersNow = cachedPlayers.get();
            if (playersNow != null && !playersNow.isEmpty()) {
              roomController.onPlayerListChanged(playersNow);
            }
            Room cached = cachedRoom.get();
            if (cached != null) {
              roomController.onSettingChange(cached);
            }
            App.setRoot(root);
          } catch (IOException | IllegalStateException e) {
            LoggerHandler.logError("Failed to change to room scene.", e);
          }
        }

        @Override
        public void onGameStart() {
        }

        @Override
        public void onDuplicateUsernameAssigned(String assignedName) {
        }

        @Override
        public void onUsernameAccepted(String newName) {
        }

        @Override
        public void onConnectionError(String message) {
          try {
            Scene scene = usernameField.getScene();
            if (scene != null && scene.getRoot() instanceof Pane) {
              NotificationUtil.showError((Pane) scene.getRoot(), message);
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
