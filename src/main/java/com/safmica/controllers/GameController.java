package com.safmica.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;

import com.safmica.model.Player;

public class GameController {

    @FXML
    private Label statusLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private TextField answerField;
    @FXML
    private HBox cardsContainer;
    @FXML
    private Label totalScoreLabel;
    @FXML
    private Label targetScoreLabel;
    @FXML
    private VBox playersContainer;
    @FXML
    private Label currentPlayerNameLabel;

    private List<Integer> cards = new ArrayList<>();
    private List<Button> cardButtons = new ArrayList<>();
    private List<Boolean> cardUsed = new ArrayList<>();

    @FXML
    private void initialize() {
        initCards(6);
        initPlayers(10);
    }

    private void initCards(int totalCards) {
        cardsContainer.getChildren().clear();
        cardButtons.clear();
        cards.clear();
        cardUsed.clear();

        int[] sampleCards = {1, 7, 8, 0, 3, 3};
        
        for (int i = 0; i < totalCards && i < sampleCards.length; i++) {
            int cardValue = sampleCards[i];
            cards.add(cardValue);
            cardUsed.add(false); 
            
            Button cardButton = new Button(String.valueOf(cardValue));
            cardButton.getStyleClass().add("card-button");
            cardButton.setPrefWidth(80);
            cardButton.setPrefHeight(100);
            
            final int cardIndex = i;
            cardButton.setOnAction(e -> handleCardClick(cardValue, cardIndex, cardButton));
            
            cardButtons.add(cardButton);
            cardsContainer.getChildren().add(cardButton);
        }
    }

    private void initPlayers(int playerCount) {
        playersContainer.getChildren().clear();

        for (int i = 0; i < playerCount; i++) {
            VBox playerBox = new VBox(5);
            playerBox.setAlignment(Pos.CENTER);
            playerBox.getStyleClass().add("player-card");
            
            Label avatarLabel = new Label("👤");
            avatarLabel.getStyleClass().add("player-avatar");
            
            Label nameLabel = new Label("Name (Pts)");
            nameLabel.getStyleClass().add("player-name");
            
            playerBox.getChildren().addAll(avatarLabel, nameLabel);
            playersContainer.getChildren().add(playerBox);
        }
    }

    public void setCards(List<Integer> newCards) {
        initCards(newCards.size());
        for (int i = 0; i < newCards.size(); i++) {
            cards.set(i, newCards.get(i));
            cardButtons.get(i).setText(String.valueOf(newCards.get(i)));
        }
    }

    public void setPlayers(List<Player> players) {
        playersContainer.getChildren().clear();
        
        for (Player player : players) {
            VBox playerBox = new VBox(5);
            playerBox.setAlignment(Pos.CENTER);
            playerBox.getStyleClass().add("player-card");
            
            if (player.isHost()) {
                playerBox.getStyleClass().add("host-player");
            }
            
            Label avatarLabel = new Label("👤");
            avatarLabel.getStyleClass().add("player-avatar");
            
            Label nameLabel = new Label(player.getName() + " (0)");
            nameLabel.getStyleClass().add("player-name");
            
            playerBox.getChildren().addAll(avatarLabel, nameLabel);
            playersContainer.getChildren().add(playerBox);
        }
    }

    @FXML
    private void handleCardClick(int cardValue, int cardIndex, Button cardButton) {
        if (cardUsed.get(cardIndex)) {
            return;
        }
        
        String currentAnswer = answerField.getText();
        if (currentAnswer.isEmpty()) {
            answerField.setText(String.valueOf(cardValue));
        } else {
            answerField.setText(currentAnswer + "+" + cardValue);
        }
        
        cardUsed.set(cardIndex, true);
        cardButton.setDisable(true);
    }

    @FXML
    private void handleOperator() {
        Button button = (Button) answerField.getScene().getFocusOwner();
        if (button != null) {
            String operator = button.getText();
            String currentAnswer = answerField.getText();
            if (!currentAnswer.isEmpty()) {
                answerField.setText(currentAnswer + operator);
            }
        }
    }

    @FXML
    private void handleClear() {
        answerField.clear();
        
        for (int i = 0; i < cardButtons.size(); i++) {
            cardUsed.set(i, false);
            cardButtons.get(i).setDisable(false);
        }
    }

    @FXML
    private void handleSurrender() {
        System.out.println("Player surrendered");
        // TODO: Implement surrender logic
    }

    @FXML
    private void handleSubmit() {
        String answer = answerField.getText();
        System.out.println("Submitted answer: " + answer);
        // TODO: Implement answer validation and scoring
    }

    @FXML
    private void handleSettings() {
        System.out.println("Open settings");
        // TODO: Implement settings modal
    }
}

