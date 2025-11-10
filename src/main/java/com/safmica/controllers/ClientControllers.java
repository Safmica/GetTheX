package com.safmica.controllers;

import com.safmica.*;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.ClientHandler;
import com.safmica.utils.*;
import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

public class ClientControllers {
  @FXML
  private TextField usernameField;
  @FXML
  private TextField portField;
  @FXML
  private TextField ipAddressField;

  private RoomClientControllers room;

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
      URL fxmlUrl = getClass().getResource("/com/safmica/views/room_client.fxml");
      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      Parent root = loader.load();
      RoomClientControllers roomController = loader.getController();
      roomController.setServerSocket(ipText, port);
      roomController.startClient(usernameText);
      App.setRoot(root);
      
    } catch (NumberFormatException e) {
      LoggerHandler.logErrorMessage("Invalid port number format.");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logError("Failed to create server or change scene.", e);
    }
  }
}
