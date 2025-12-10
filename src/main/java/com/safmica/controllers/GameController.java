package com.safmica.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import java.io.IOException;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.App;
import com.safmica.listener.GameListener;
import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.model.PlayerLeaderboard;
import com.safmica.model.PlayerSurrender;
import com.safmica.model.Room;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.TcpServerHandler;
import com.safmica.utils.LoggerHandler;
import com.safmica.GameSession;
import javafx.stage.StageStyle;
import com.safmica.utils.ui.NotificationUtil;

public class GameController implements GameListener, RoomListener {

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
    private StackPane overlay;
    @FXML
    private Label currentPlayerNameLabel;
    @FXML
    private Label currentAnswerStatus;
    @FXML
    private Label currentAnswer;
    @FXML
    private Label currentPlayerAnswer;
    @FXML
    private Button submitButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button surrendButton;
    @FXML
    private Label surrendPlaceholder;
    @FXML
    private Label roundLabel;

    private TcpClientHandler client;
    private TcpServerHandler server;
    private String username;

    private Room room;
    private Game game;
    private GameAnswer gameAnswer = new GameAnswer();
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
            server.setLeaderboard();
            server.randomizeCards();
        }
        client.addGameListener(this);
        client.addRoomListener(this);
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

    private void setRound() {
        roundLabel.setText("Round " + Integer.toString(game.getRound()));
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

            Label nameLabel = new Label(player.getName());
            nameLabel.getStyleClass().add("player-name");

            playerBox.getChildren().addAll(avatarLabel, nameLabel);
            playersContainer.getChildren().add(playerBox);
        }
    }

    @Override
    public void onPlayerListChanged(List<Player> serverPlayers) {
        Platform.runLater(() -> {
            players.setAll(serverPlayers);
            setPlayers();
        });
    }

    @Override
    public void onPlayerConnected(String username) {
        Platform.runLater(() -> {
            NotificationUtil.showInfo(overlay != null ? overlay : playersContainer, username + " joined the room");
        });
    }

    @Override
    public void onPlayerDisconnected(String username) {
        Platform.runLater(() -> {
            NotificationUtil.showError(overlay != null ? overlay : playersContainer, username + " left the room");
        });
    }

    @Override
    public void onDuplicateUsernameAssigned(String assignedName) {}

    @Override
    public void onUsernameAccepted(String newName) {}

    @Override
    public void onConnectionError(String message) {}

    @Override
    public void onSettingChange(Room room) {}

    @Override
    public void onGameStart() {}

    private void setCurrentPlayer() {
        Player currentPlayer = players.stream()
                .filter(p -> p.getName().equals(username))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null) {
            currentPlayerNameLabel.setText(currentPlayer.getName());
        }
    }

    @Override
    public void onCardsBroadcast(Game game) {
        this.game = game;
        setRound();
        setCards();
    }

    @Override
    public void onSubmitAck(String msg) {
        if (msg.toUpperCase().equals("RECEIVED")) {
            if (overlay != null)
                NotificationUtil.showSuccess(overlay, msg);
            else
                NotificationUtil.showSuccess(cardsContainer, msg);
            disableSubmit(10);
        } else {
            msg = msg.toUpperCase();
            switch (msg) {
                case "LIMIT":
                    msg = "YOU'VE REACH THE LIMIT";
                    disableSubmit(0);
                    break;
                case "COOLDOWN":
                    msg = "SUBMIT IS COOLDOWN";
                    disableSubmit(10);
                    break;
                case "ROUND_OVER":
                    msg = "ROUND IS OVER";
                    disableSubmit(0);
                    break;
                case "NOT_ALLOWED_FINAL":
                    msg = "YOU'RE NOT FINALIST";
                    disableSubmit(0);
                    break;
            }
            if (overlay != null)
                NotificationUtil.showError(overlay, msg);
            else
                NotificationUtil.showError(cardsContainer, msg);
        }
    }

    @Override
    public void onGetGameResult(GameAnswer gameAnswer) {
        game.setCurrentAnswer(gameAnswer);
        currentAnswer.setText(gameAnswer.answer);
        currentPlayerAnswer.setText(gameAnswer.username);
        Platform.runLater(() -> {
            currentAnswerStatus.getStyleClass().removeAll("current-answer-status-correct",
                    "current-answer-status-wrong");
            if (gameAnswer.status) {
                currentAnswerStatus.setText("CORRECT ANSWER");
                totalScoreLabel.setText(formatScore(gameAnswer.x));
                currentAnswerStatus.getStyleClass().add("current-answer-status-correct");
            } else {
                currentAnswerStatus.setText("WRONG ANSWER");
                totalScoreLabel.setText(formatScore(gameAnswer.x));
                currentAnswerStatus.getStyleClass().add("current-answer-status-wrong");
            }
        });
    }

    private String formatScore(double value) {
        double rounded = Math.round(value);
        if (Math.abs(value - rounded) < 1e-9) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void disableSubmit(int seconds) {
        if (seconds == 0) {
            disableSubmitStyle();
        } else {
            disableSubmitStyle();
            PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
            pause.setOnFinished(e -> enableSubmitStyle());
            pause.play();
        }
    }

    private void disableSubmitStyle() {
        Platform.runLater(() -> {
            submitButton.setDisable(true);
            submitButton.getStyleClass().removeAll("button");
            submitButton.getStyleClass().add("submit-button-disable");
        });
    }

    private void enableSubmitStyle() {
        Platform.runLater(() -> {
            submitButton.setDisable(false);
            submitButton.getStyleClass().removeAll("submit-button-disable");
            submitButton.getStyleClass().add("button");
        });
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
                    if (!Character.isDigit(lastChar) && lastChar != ')') {
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
        client.offerSurrender();
        surrendButton.setDisable(true);
    }

    @FXML
    private void handleSubmit() {
        String answer = answerField.getText();

        if (answer.trim().isEmpty()) {
            System.out.println("Answer cannot be empty!");
            NotificationUtil.showError(overlay, "Answer cannot be empty");
            return;
        }

        if (cardUsed.stream().filter(Boolean::booleanValue).count() != cards.size()) {
            System.out.println("Must implement all of the number");
            NotificationUtil.showError(overlay, "Must implement all of the number");
            return;
        }

        if (!areParenthesesBalanced(answer)) {
            NotificationUtil.showError(overlay, "Invalid Parentheses");
            return;
        }

        try {
            gameAnswer.answer = answer;
            gameAnswer.username = username;
            gameAnswer.round = game.getRound();

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

    @FXML
    private void handleSettings() {
        ContextMenu menu = new ContextMenu();
        MenuItem exit = new MenuItem("Exit");
        MenuItem leaderboard = new MenuItem("Leaderboard");

        exit.setOnAction(ev -> {
            if (client != null) {
                client.stopClient();
            }
            if (server != null) {
                server.disconnectAllClients();
                server.stopServer();
            }
            try {
                App.setRoot("menu");
            } catch (IOException | IllegalStateException e) {
                LoggerHandler.logFXMLFailed("Menu", e);
            }
        });

        leaderboard.setOnAction(ev -> {
            showLeaderboardModal();
        });

        menu.getItems().addAll(leaderboard, exit);
        menu.show(settingsButton, Side.BOTTOM, 0, 0);
    }

    private void showLeaderboardModal() {
        Stage dialog = new Stage();
        dialog.initOwner(submitButton.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Leaderboard");

        VBox root = new VBox(8);
        root.setPadding(new javafx.geometry.Insets(12));

        if (game.getLeaderboard() == null || game.getLeaderboard().isEmpty()) {
            root.getChildren().add(new Label("No leaderboard available"));
        } else {
            List<PlayerLeaderboard> copy = new ArrayList<>(game.getLeaderboard());
            copy.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

            for (PlayerLeaderboard p : copy) {
                HBox row = new HBox(10);
                Label name = new Label(p.getName());
                name.setPrefWidth(200);
                Label score = new Label(Integer.toString(p.getScore()));
                row.getChildren().addAll(name, score);
                root.getChildren().add(row);
            }
        }

        Button close = new Button("Close");
        close.setOnAction(e -> dialog.close());
        root.getChildren().add(close);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @Override
    public void onLeaderboardUpdate(List<PlayerLeaderboard> leaderboards) {
        game.setLeaderboard(leaderboards);
        System.out.println("TRIGGER");
    }

    @Override
    public void onNextRound() {
        Platform.runLater(() -> {
            totalScoreLabel.setText("0");
            answerField.setText("");
            submitButton.setDisable(false);
            submitButton.getStyleClass().removeAll("submit-button-disable");
            submitButton.getStyleClass().add("button");
            currentAnswerStatus.getStyleClass().removeAll("current-answer-status-wrong",
                    "current-answer-status-correct");
            currentAnswerStatus.getStyleClass().add("current-answer-status");
            currentAnswerStatus.setText("Waiting...");
            currentPlayerAnswer.setText("No one is submit");
            currentAnswer.setText("Empty");
        });
    }

    @Override
    public void onRoundOver(String winner) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initOwner(submitButton.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Round Over");

            VBox root = new VBox(12);
            root.setPadding(new javafx.geometry.Insets(12));
            root.setAlignment(Pos.CENTER);

            String message = (winner == null || winner.isEmpty()) ? "Round Over" : winner;
            Label msg = new Label(message);
            msg.setWrapText(true);
            msg.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER);

            Button playAgain = new Button("Play Again");
            Button exit = new Button("Exit");

            playAgain.setOnAction(ev -> {
                GameSession sessionObj = GameSession.getInstance();
                sessionObj.setPlayers(new ArrayList<>(players));
                sessionObj.setRoom(room);
                sessionObj.setUsername(username);
                sessionObj.setServer(server);
                sessionObj.setClient(client);

                dialog.close();
                try {
                    FXMLLoader loader = App.getFXMLLoader("room");
                    javafx.scene.Parent loadedRoot = loader.load();
                    com.safmica.controllers.RoomController rc = loader.getController();
                    rc.restoreFromSession(sessionObj);
                    App.setRoot(loadedRoot);
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Room", e);
                }
            });

            exit.setOnAction(ev -> {
                GameSession sess = GameSession.getInstance();
                sess.setPlayers(new ArrayList<>(players));
                sess.setRoom(room);
                sess.setUsername(username);
                sess.setServer(server);
                sess.setClient(client);

                dialog.close();
                try {
                    FXMLLoader loader = App.getFXMLLoader("menu");
                    javafx.scene.Parent loadedRoot = loader.load();
                    // optionally MenuController can read GameSession if needed
                    App.setRoot(loadedRoot);
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Menu", e);
                }
            });

            buttons.getChildren().addAll(playAgain, exit);
            root.getChildren().addAll(msg, buttons);

            dialog.setOnCloseRequest(e -> e.consume());
            dialog.initStyle(StageStyle.UNDECORATED);

            Scene scene = new Scene(root);
            dialog.setScene(scene);
            dialog.showAndWait();
        });
    }

    @Override
    public void onPlayerSurrender(PlayerSurrender playerSurrender) {
        NotificationUtil.showError(overlay, playerSurrender.username + " Offer to Surrender");
        Platform.runLater(() -> {
            surrendPlaceholder.setText(playerSurrender.placeholder);
        });
    }

    @Override
    public void onNextRoundWithSurrender(String msg) {
        NotificationUtil.showError(overlay, msg);
        Platform.runLater(() -> {
            surrendPlaceholder.setText("");
            surrendButton.setDisable(false);
        });
    }

    @Override
    public void onFinalRound(List<String> finalists) {
        if (!finalists.contains(username)) {
            submitButton.setDisable(true);
        } else {
            submitButton.setDisable(false);
        }

        totalScoreLabel.setText("0");
        answerField.setText("");
        submitButton.getStyleClass().removeAll("submit-button-disable");
        submitButton.getStyleClass().add("button");
        currentAnswerStatus.getStyleClass().removeAll("current-answer-status-wrong",
                "current-answer-status-correct");
        currentAnswerStatus.getStyleClass().add("current-answer-status");
        currentAnswerStatus.setText("Waiting...");
        currentPlayerAnswer.setText("No one is submit");
        currentAnswer.setText("Empty");

        roundLabel.setText("FINAL ROUND");

        NotificationUtil.showInfo(overlay, "FINAL ROUND START");
    }
}
