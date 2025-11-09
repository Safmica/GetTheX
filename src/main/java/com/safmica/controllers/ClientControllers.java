package com.safmica.controllers;

import com.safmica.*;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.ClientHandler;
import com.safmica.utils.*;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class ClientControllers {
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
    String portText = portField.getText();
    String ipText = ipAddressField.getText();

    try {
      int port = Integer.parseInt(portText);

      room = new RoomClientControllers(ipText, port);

      room.startClient();

      App.setRoot("room_client");

    } catch (NumberFormatException e) {
      LoggerHandler.logErrorMessage("Invalid port number format.");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.logError("Failed to create server or change scene.", e);
    }
  }
}
