package com.safmica.utils.ui;

import com.safmica.App;
import com.safmica.controllers.NotificationController;
import com.safmica.utils.LoggerHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class NotificationUtil {
    public static void showNotification(Pane parent, String message, String cssColor, double seconds) {
        if (parent == null) return;
        try {
            FXMLLoader loader = App.getFXMLLoader("components/notification");
            loader.setClassLoader(NotificationUtil.class.getClassLoader());
            loader.load();
            NotificationController ctrl = loader.getController();
            if (ctrl == null) return;
            ctrl.setMessage(message);
            if (cssColor != null) ctrl.setBackgroundColor(cssColor);
            ctrl.showAndAutoRemove(parent, seconds);
        } catch (IOException e) {
            LoggerHandler.logError("Failed to load notification component", e);
        }
    }

    public static void showInfo(Pane parent, String message) {
        showNotification(parent, message, "#3498db", 3.0);
    }

    public static void showSuccess(Pane parent, String message) {
        showNotification(parent, message, "#2ecc71", 3.0);
    }

    public static void showError(Pane parent, String message) {
        showNotification(parent, message, "#e74c3c", 3.0);
    }
}
