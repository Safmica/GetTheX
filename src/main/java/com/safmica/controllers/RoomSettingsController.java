package com.safmica.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

public class RoomSettingsController {

    @FXML
    private Spinner<Integer> totalCardSpinner;
    @FXML
    private Spinner<Integer> totalRoundSpinner;

    private int selectedTotalCard;
    private int selectedTotalRound;
    private boolean saved = false;

    @FXML
    private void initialize() {
        selectedTotalCard = 4;
        selectedTotalRound = 3;
    }

    public void setCurrentTotalCard(int totalCard) {
        totalCardSpinner.getValueFactory().setValue(totalCard);
    }

    public void setCurrentTotalRound(int totalRound) {
        totalRoundSpinner.getValueFactory().setValue(totalRound);
    }

    @FXML
    private void handleSave() {
        selectedTotalCard = totalCardSpinner.getValue();
        selectedTotalRound = totalRoundSpinner.getValue();
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

    public int getSelectedTotalRound() {
        return selectedTotalRound;
    }

    public boolean isSaved() {
        return saved;
    }
}
