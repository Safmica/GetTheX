package com.safmica.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

public class RoomSettingsController {

    @FXML
    private Spinner<Integer> totalCardSpinner;
    
    private int selectedTotalCard;
    private boolean saved = false;

    @FXML
    private void initialize() {
        selectedTotalCard = 4;
    }

    public void setCurrentTotalCard(int totalCard) {
        totalCardSpinner.getValueFactory().setValue(totalCard);
    }

    @FXML
    private void handleSave() {
        selectedTotalCard = totalCardSpinner.getValue();
        saved = true;
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) totalCardSpinner.getScene().getWindow();
        stage.close();
    }

    public int getSelectedTotalCard() {
        return selectedTotalCard;
    }

    public boolean isSaved() {
        return saved;
    }
}
