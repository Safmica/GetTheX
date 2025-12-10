package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
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
    Parent root = null;

    try {
      int port = Integer.parseInt(portText);
      URL fxmlUrl = getClass().getResource("/com/safmica/views/room.fxml");
      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      root = loader.load();
      RoomController roomController = loader.getController();
      roomController.initAsClient(ipText, port, usernameText);
    } catch (Exception e) {
      LoggerHandler.logErrorMessage("Failed to connect please ensure ip and port is correct");
      return;
    } 
    try {
      App.setRoot(root);
    }
    catch (IllegalStateException e) {
      LoggerHandler.logError("Failed to create server or change scene.", e);
      return;
    }
  }
}
