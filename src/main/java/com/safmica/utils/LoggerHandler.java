package com.safmica.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoggerHandler {

  private static final Logger logger = Logger.getLogger(Logger.class.getName());

  public static void LogFXMLFailed(String page, Exception e) {
    logger.log(Level.SEVERE, page + " FXML File Not Found", e);
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Error");
      alert.setHeaderText("Failed to Load Page");
      alert.setContentText("Can't Load Page "+page);

      alert.showAndWait();
    });
  }
}
