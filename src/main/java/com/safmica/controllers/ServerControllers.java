package com.safmica.controllers;

import com.safmica.*;
import com.safmica.utils.LoggerHandler;
import java.io.IOException;
import javafx.fxml.FXML;

public class ServerControllers {

  @FXML
  private void backToMenu() {
    try {
      App.setRoot("menu");
    } catch (IOException | IllegalStateException e) {
      LoggerHandler.LogFXMLFailed("Menu", e);
    }
  }
}
