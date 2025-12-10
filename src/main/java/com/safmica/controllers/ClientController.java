package com.safmica.controllers;

import com.safmica.*;
import com.safmica.model.ClientSaveState;
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
import javafx.scene.control.Button;

public class ClientController {
  @FXML
  private TextField usernameField;
  @FXML
  private TextField portField;
  @FXML
  private TextField ipAddressField;
  @FXML
  private Button reconnectButton;

  private ClientSaveState cachedSave;

  @FXML
  private void backToMenu() {
    try {
      App.setRoot("menu");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logFXMLFailed("Menu", e);
    }
  }

  @FXML
  private void initialize() {
    try {
      java.util.Optional<ClientSaveState> maybe = ClientAutosaveUtil.read();
      if (maybe.isPresent()) {
        cachedSave = maybe.get();
        if (cachedSave.getUsername() != null && !cachedSave.getUsername().isEmpty()) {
          usernameField.setText(cachedSave.getUsername());
        }
        if (cachedSave.getServerIp() != null && !cachedSave.getServerIp().isEmpty()) {
          ipAddressField.setText(cachedSave.getServerIp());
        }
        if (cachedSave.getServerPort() > 0) {
          portField.setText(String.valueOf(cachedSave.getServerPort()));
        }
        if (reconnectButton != null) {
          reconnectButton.setVisible(true);
          reconnectButton.setManaged(true);
        }
      } else {
        if (reconnectButton != null) {
          reconnectButton.setVisible(false);
          reconnectButton.setManaged(false);
        }
      }
    } catch (Exception ignored) {
      if (reconnectButton != null) {
        reconnectButton.setVisible(false);
        reconnectButton.setManaged(false);
      }
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
      client.addRoomListener(new RoomListener() {
        @Override
        public void onPlayerListChanged(List<Player> players) {}
        @Override
        public void onPlayerConnected(String username) {}
        @Override
        public void onPlayerDisconnected(String username) {}
        @Override
        public void onSettingChange(Room room) {
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

  @FXML
  private void reconnectRoom() {
    if (cachedSave == null) {
      LoggerHandler.logErrorMessage("No save data available for reconnect.");
      return;
    }

    String usernameText = cachedSave.getUsername();
    String ipText = cachedSave.getServerIp();
    int port = cachedSave.getServerPort();
    String playerId = cachedSave.getPlayerId();

    try {
      try (Socket test = new Socket()) {
        test.connect(new InetSocketAddress(ipText, port), 2000);
      } catch (Exception connEx) {
        Pane parent = null;
        try {
          if (usernameField.getScene() != null && usernameField.getScene().getRoot() instanceof Pane) {
            parent = (Pane) usernameField.getScene().getRoot();
          }
        } catch (Exception ignored) {}

        String msg = "Failed to connect to server: " + connEx.getMessage();
        if (parent != null) {
          NotificationUtil.showError(parent, msg);
        } else {
          System.out.println(msg);
        }
        return;
      }

      URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      Parent root = loader.load();
      RoomController roomController = loader.getController();

      if (playerId != null && !playerId.trim().isEmpty()) {
        roomController.requestReconnect(ipText, port, playerId, usernameText);
        App.setRoot(root);
        Pane parent = null;
        try {
          if (root instanceof Pane) parent = (Pane) root;
        } catch (Exception ignored) {}
        if (parent != null) NotificationUtil.showInfo(parent, "Reconnecting to saved session...");
      } else {
        roomController.initAsClient(ipText, port, usernameText);
        App.setRoot(root);
      }
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logError("Failed to reconnect.", e);
    }
  }
}
