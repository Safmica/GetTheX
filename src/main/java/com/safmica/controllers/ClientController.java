package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
import com.safmica.utils.ui.NotificationUtil;
import javafx.scene.layout.Pane;
import java.net.InetSocketAddress;
import java.net.Socket;
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
      try (Socket test = new Socket()) {
        test.connect(new InetSocketAddress(ipText, port), 2000);
      } catch (Exception connEx) {
        String msg = "Failed to connect to server: " + connEx.getMessage();
        Pane parent = null;
        try {
          if (usernameField.getScene() != null && usernameField.getScene().getRoot() instanceof Pane) {
            parent = (Pane) usernameField.getScene().getRoot();
          }
        } catch (Exception ignored) {}
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
      roomController.initAsClient(ipText, port, usernameText);
      App.setRoot(root);
      
    } catch (NumberFormatException e) {
      LoggerHandler.logErrorMessage("Invalid port number format.");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logError("Failed to create server or change scene.", e);
    }
  }
}
