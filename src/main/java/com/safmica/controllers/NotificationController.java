package com.safmica.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class NotificationController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label messageLabel;

    public void setMessage(String message) {
        Platform.runLater(() -> {
            if (messageLabel != null) messageLabel.setText(message);
        });
    }

    public void setBackgroundColor(String cssColor) {
        Platform.runLater(() -> {
            if (rootPane != null) {
                rootPane.setStyle("-fx-background-color: " + cssColor + "; -fx-background-radius: 6; -fx-padding: 8;");
            }
        });
    }

    public Node getRoot() {
        return rootPane;
    }

    public void showAndAutoRemove(Pane parent, double seconds) {
        if (parent == null || rootPane == null) return;
        Platform.runLater(() -> {
            if (!parent.getChildren().contains(rootPane)) {
                parent.getChildren().add(rootPane);
            }
            PauseTransition pt = new PauseTransition(Duration.seconds(seconds));
            pt.setOnFinished(e -> parent.getChildren().remove(rootPane));
            pt.play();
        });
    }

    public void show(Pane parent) {
        if (parent == null || rootPane == null) return;
        Platform.runLater(() -> {
            if (!parent.getChildren().contains(rootPane)) parent.getChildren().add(rootPane);
        });
    }

    public void hide(Pane parent) {
        if (parent == null || rootPane == null) return;
        Platform.runLater(() -> parent.getChildren().remove(rootPane));
    }
}
