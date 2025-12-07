package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.listener.GameListener;
import com.safmica.model.Player;
import com.safmica.model.Room;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.TcpServerHandler;

public class GameController implements GameListener {

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
    private Label x;
    @FXML
    private VBox playersContainer;
    @FXML
    private Label currentPlayerNameLabel;
    @FXML
    private Label currentAnswer;
    @FXML
    private Label currentPlayerAnswer;

    private TcpClientHandler client;
    private TcpServerHandler server;
    private boolean isHost = true;
    private String username;

    private Room room;
    private Game game;
    private GameAnswer gameAnswer;
    private ObservableList<Player> players = FXCollections.observableArrayList();

    private List<Integer> cards = new ArrayList<>();
    private List<Button> cardButtons = new ArrayList<>();
    private List<Boolean> cardUsed = new ArrayList<>();

    public void initializeGame(String username, Room room, ObservableList<Player> players, TcpServerHandler server,
            TcpClientHandler client) {
        this.username = username;
        this.room = room;
        this.players = players;
        this.server = server;
        this.client = client;
        if (server != null) {
            isHost = true;
            server.randomizeCards();
        }
        client.addGameListener(this);
        startRound();
    }

    @FXML
    private void initialize() {
        if (room != null && !players.isEmpty()) {
            startRound();
        }
    }

    private void startRound() {
        setCurrentPlayer();
        setPlayers();
    }

    private void setCards() {
        cardsContainer.getChildren().clear();
        cardButtons.clear();
        cards.clear();
        cardUsed.clear();
        cards = game.getCards();

        int totalCards = room.getTotalCard();
        double cardWidth = totalCards <= 4 ? 80 : (totalCards == 5 ? 70 : 60);
        double cardHeight = totalCards <= 4 ? 100 : (totalCards == 5 ? 90 : 80);

        for (int i = 0; i < totalCards; i++) {
            int cardValue = cards.get(i);
            cardUsed.add(false);

            Button cardButton = new Button(String.valueOf(cardValue));
            cardButton.getStyleClass().add("card-button");
            cardButton.setPrefWidth(cardWidth);
            cardButton.setPrefHeight(cardHeight);

            final int cardIndex = i;
            cardButton.setOnAction(e -> handleCardClick(cardValue, cardIndex, cardButton));

            cardButtons.add(cardButton);
            cardsContainer.getChildren().add(cardButton);
        }

        System.out.println(game.getX());

        Platform.runLater(() -> {
            x.setText(Integer.toString(game.getX()));
        });
    }

    private void setPlayers() {
        playersContainer.getChildren().clear();

        for (Player player : players) {
            if (player.getName().equals(username)) {
                continue;
            }

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

    private void setCurrentPlayer() {
        Player currentPlayer = players.stream()
                .filter(p -> p.getName().equals(username))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null) {
            currentPlayerNameLabel.setText(currentPlayer.getName() + " (0)");
        }
    }

    @Override
    public void onCardsBroadcast(Game game) {
        this.game = game;
        setCards();
    }

    @FXML
    private void handleCardClick(int cardValue, int cardIndex, Button cardButton) {
        if (cardUsed.get(cardIndex)) {
            return;
        }

        String currentAnswer = answerField.getText();

        if (!currentAnswer.isEmpty()) {
            char lastChar = currentAnswer.charAt(currentAnswer.length() - 1);
            if (Character.isDigit(lastChar)) {
                return;
            }
            if (lastChar == '²') {
                return;
            }
        }

        if (currentAnswer.isEmpty()) {
            answerField.setText(String.valueOf(cardValue));
        } else {
            answerField.setText(currentAnswer + cardValue);
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

            if (operator.equals("√x")) {
                if (!currentAnswer.isEmpty()) {
                    char lastChar = currentAnswer.charAt(currentAnswer.length() - 1);
                    if (Character.isDigit(lastChar) || lastChar == '√' || lastChar == '²') {
                        return;
                    }
                }
            } else if (!operator.equals("(") && !operator.equals(")")) {
                if (currentAnswer.isEmpty()) {
                    return;
                }
                char lastChar = currentAnswer.charAt(currentAnswer.length() - 1);

                if (operator.equals("x²")) {
                    if (!Character.isDigit(lastChar)) {
                        return;
                    }
                } else {
                    if (!Character.isDigit(lastChar) && lastChar != '²' && lastChar != ')') {
                        return;
                    }
                }
            }

            switch (operator) {
                case "√x":
                    operator = "√";
                    break;
                case "x²":
                    operator = "²";
                    break;
            }

            answerField.setText(currentAnswer + operator);
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

        if (answer.trim().isEmpty()) {
            System.out.println("Answer cannot be empty!");
            return;
        }

        if (!areParenthesesBalanced(answer)) {
            System.out.println("Invalid parentheses in expression");
            currentAnswer.setText("INVALID PARENTHESES");
            return;
        }

        try {
            double result = calculate(answer);

            if (Math.abs(result - game.getX()) < 0.001) {
                System.out.println("Correct! " + answer + " = " + result);
            } else {
                System.out.println("Wrong! " + answer + " = " + result + ", but X = " + game.getX());
            }

            gameAnswer.answer = answer;
            gameAnswer.username = username;
            gameAnswer.x = result;

            client.gameMsg(gameAnswer);
        } catch (Exception e) {
            System.out.println("Error calculating: " + answer + " - " + e.getMessage());
            currentAnswer.setText("ERROR");
        }
    }

    private boolean areParenthesesBalanced(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(')
                depth++;
            else if (c == ')') {
                depth--;
                if (depth < 0)
                    return false;
            }
        }
        return depth == 0;
    }

    private double calculate(String expression) {
        if (expression.contains("?") || expression.contains("!") || expression.contains("@") ||
                expression.contains("#") || expression.contains("$") || expression.contains("%") ||
                expression.contains("&") || expression.contains("[") || expression.contains("]")
                || expression.contains("{") ||
                expression.contains("}") || expression.contains("=") || expression.contains("<") ||
                expression.contains(">") || expression.contains(",") || expression.contains(";") ||
                expression.contains(":") || expression.contains("'") || expression.contains("\"")) {
            throw new IllegalArgumentException("Invalid characters in expression");
        }

        expression = expression.replace("×", "*")
                .replace("÷", "/");

        expression = expression.replace("²", "^2");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '√') {
                result.append("sqrt(");

                i++;
                if (i >= expression.length()) {
                    throw new IllegalArgumentException("√ must be followed by a number or parentheses");
                }

                if (expression.charAt(i) == '(') {
                    int start = i + 1;
                    int depth = 1;
                    int j = start;
                    while (j < expression.length() && depth > 0) {
                        char cc = expression.charAt(j);
                        if (cc == '(')
                            depth++;
                        else if (cc == ')')
                            depth--;
                        j++;
                    }
                    if (depth != 0) {
                        throw new IllegalArgumentException("Unbalanced parentheses after √");
                    }
                    String inner = expression.substring(start, j - 1);
                    result.append(inner).append(")");
                    i = j - 1;
                } else {
                    StringBuilder number = new StringBuilder();
                    while (i < expression.length()
                            && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                        number.append(expression.charAt(i));
                        i++;
                    }
                    if (number.length() > 0) {
                        result.append(number).append(")");
                        i--;
                    } else {
                        throw new IllegalArgumentException("√ must be followed by a number or parentheses");
                    }
                }
            } else {
                result.append(c);
            }
        }
        expression = result.toString();

        try {
            Expression exp = new ExpressionBuilder(expression).build();
            return exp.evaluate();
        } catch (Exception e) {
            throw new RuntimeException("Invalid mathematical expression: " + e.getMessage());
        }
    }

    @FXML
    private void handleSettings() {
        System.out.println("Open settings");
        // TODO: Implement settings modal
    }
}
