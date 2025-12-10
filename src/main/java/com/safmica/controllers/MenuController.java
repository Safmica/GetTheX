package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
import com.safmica.network.server.TcpServerHandler;
import java.io.IOException;
import java.net.URL;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

public class MenuController {

  @FXML
  private void switchToServer() {
    try {
      App.setRoot("server");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logFXMLFailed("Server", e);
    }
  }

  @FXML
  private void switchToClient() {
    try {
      App.setRoot("client");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logFXMLFailed("Client", e);
    }
  }

  @FXML
  private Button reconnectButton;

  @FXML
  private void initialize() {
    try {
        java.util.Optional<com.safmica.model.SaveState> maybe = com.safmica.utils.AutosaveUtil.read();
        if (maybe.isPresent()) {
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
  private void reconnectFromMenu() {
    try {
      java.util.Optional<com.safmica.model.SaveState> maybeSave = AutosaveUtil.read();
      if (!maybeSave.isPresent()) {
        LoggerHandler.logErrorMessage("No server save found to reconnect.");
        return;
      }

      com.safmica.model.SaveState saveState = maybeSave.get();

      String hostName = "Host";
      if (saveState.getPlayers() != null && !saveState.getPlayers().isEmpty()) {
        for (com.safmica.model.Player p : saveState.getPlayers()) {
          if (p.isHost()) {
            hostName = p.getName();
            break;
          }
        }
      }

      int port = saveState.getServerPort() > 0 ? saveState.getServerPort() : 8080;

      TcpServerHandler server = new TcpServerHandler(port);
      server.startServer();

      URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      Parent root = loader.load();
      RoomController roomController = loader.getController();
      roomController.initAsHostFromSave(server, port, hostName, saveState);
      
      App.setRoot(root);
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logError("Failed to reconnect server from save.", e);
    }
  }

  @FXML
  private void exitTheGame() {
    Platform.exit();
  }
}
