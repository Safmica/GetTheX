package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.*;
import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class MenuControllers {

  @FXML
  private void switchToServer() {
    try {
      App.setRoot("server");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.LogFXMLFailed("Server", e);
    }
  }

  @FXML
  private void switchToClient() {
    try {
      App.setRoot("client");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.LogFXMLFailed("Client", e);
    }
  }

  @FXML
  private void exitTheGame() {
    Platform.exit();
  }
}
