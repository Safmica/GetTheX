package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import com.safmica.App;
import com.safmica.listener.RoomListener;
import com.safmica.model.Player;
import com.safmica.model.Room;
import com.safmica.network.client.TcpClientHandler;
import com.safmica.network.server.TcpServerHandler;
import com.safmica.utils.LoggerHandler;
import com.safmica.utils.ui.PlayerListCell;
import com.safmica.utils.ui.NotificationUtil;
import javafx.scene.layout.Pane;

public class RoomController implements RoomListener {

    @FXML
    private ListView<Player> playersList;
    @FXML
    private Button startButton;
    @FXML
    private Button settingButton;
    @FXML
    private Label totalCard;
    @FXML
    private Label totalRound;
    @FXML
    private Label playerLimit;
    private int totalRoundNow;
    private int totalCardNow;
    private int playerLimitNow;

    private String username;
    private TcpClientHandler clientHandler;
    private TcpServerHandler server;
    private boolean isHost;
    private Room room;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        playersList.setItems(players);

        playersList.setCellFactory(listView -> new PlayerListCell(null, () -> this.username));
    }

    public void initAsHost(TcpServerHandler existingServer, int port, String username) {
        this.isHost = true;
        this.username = username;
        this.server = existingServer;

        if (startButton != null) {
            startButton.setVisible(true);
            startButton.setManaged(true);
        }

        if (settingButton != null) {
            settingButton.setVisible(true);
            settingButton.setManaged(true);
        }

        new Thread(() -> {
            try {
                Thread.sleep(200);
                connectAsClient("localhost", port, username);
                System.out.println("DEBUG : HOST CONNECTED AS CLIENT");
            } catch (Exception e) {
                LoggerHandler.logError("Error connecting host as client", e);
            }
        }).start();
    }

    public void initAsClient(String host, int port, String username) {
        this.isHost = false;
        this.username = username;
        connectAsClient(host, port, username);
    }

    public void restoreFromSession(com.safmica.GameSession session) {
        if (session == null) return;
        this.username = session.getUsername();
        this.room = session.getRoom();
        this.server = session.getServer();
        this.clientHandler = session.getClient();
        List<Player> sessPlayers = session.getPlayers();
        if (sessPlayers != null) {
            Platform.runLater(() -> players.setAll(sessPlayers));
        }

        this.isHost = this.server != null;

        if (this.clientHandler != null) {
            this.clientHandler.addRoomListener(this);
        }

        if (this.room != null) {
            Platform.runLater(() -> {
                totalCard.setText("Total Cards = " + room.getTotalCard());
                totalRound.setText("Total Rounds = " + room.getTotalRound());
            });
        }

        Platform.runLater(() -> {
            if (startButton != null) {
                startButton.setVisible(isHost);
                startButton.setManaged(isHost);
            }
            if (settingButton != null) {
                settingButton.setVisible(isHost);
                settingButton.setManaged(isHost);
            }
        });
    }

    private void connectAsClient(String host, int port, String username) {
        clientHandler = new TcpClientHandler(host, port, username);
        clientHandler.addRoomListener(this);
        clientHandler.startClient();
        Platform.runLater(() -> playersList.setCellFactory(listView -> new PlayerListCell(clientHandler, () -> this.username)));
    }

    private void cleanup() {
        if (clientHandler != null) {
            clientHandler.stopClient();
            clientHandler = null;
        }

        if (server != null) {
            server.stopServer();
            server = null;
        }
    }

    @Override
    public void onPlayerListChanged(List<Player> serverPlayers) {
        Platform.runLater(() -> {
            players.setAll(serverPlayers);
        });
    }

    @Override
    public void onPlayerConnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " joined the room");
            Pane parent = null;
            try {
                if (playersList != null && playersList.getScene() != null && playersList.getScene().getRoot() instanceof Pane) {
                    parent = (Pane) playersList.getScene().getRoot();
                }
            } catch (Exception ignored) {}
            if (parent != null) {
                NotificationUtil.showInfo(parent, username + " joined the room");
            }
        });
    }

    @Override
    public void onGameStart() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = App.getFXMLLoader("game");
                Parent root = loader.load();
                
                GameController gameController = loader.getController();
                gameController.initializeGame(username, room, players, isHost ? server : null, clientHandler);
                
                App.setRoot(root);
            } catch (IOException | IllegalStateException e) {
                LoggerHandler.logFXMLFailed("Game", e);
            }
        });
    }

    @Override
    public void onPlayerDisconnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " left the room");
            Pane parent = null;
            try {
                if (playersList != null && playersList.getScene() != null && playersList.getScene().getRoot() instanceof Pane) {
                    parent = (Pane) playersList.getScene().getRoot();
                }
            } catch (Exception ignored) {}
            if (parent != null) {
                NotificationUtil.showError(parent, username + " left the room");
            }

            Player disconnectedPlayer = players.stream()
                    .filter(p -> p.getName().equals(username) && p.isHost())
                    .findFirst()
                    .orElse(null);

            if (disconnectedPlayer != null && !isHost) {
                System.out.println("Host disconnected - leaving room...");
                cleanup();

                try {
                    App.setRoot("menu");
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Menu", e);
                }
            }
        });
    }

    @Override
    public void onDuplicateUsernameAssigned(String assignedName) {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog(assignedName);
            dialog.setTitle("Duplicate Username");
            dialog.setHeaderText("Username already taken");
            dialog.setContentText("Please enter a new username:");
            dialog.initOwner(playersList.getScene().getWindow());

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String newName = result.get().trim();
                if (!newName.isEmpty() && clientHandler != null) {
                    clientHandler.requestChangeUsername(newName);
                }
            }
        });
    }

    @Override
    public void onUsernameAccepted(String newName) {
        this.username = newName;
        Platform.runLater(() -> {
            System.out.println("Username updated to: " + newName);

            if (playersList != null) {
                playersList.refresh();
            }
        });
    }

    @Override
    public void onConnectionError(String message) {
        Platform.runLater(() -> {
            Pane parent = null;
            try {
                if (playersList != null && playersList.getScene() != null && playersList.getScene().getRoot() instanceof Pane) {
                    parent = (Pane) playersList.getScene().getRoot();
                }
            } catch (Exception ignored) {}

            if (parent != null) {
                NotificationUtil.showError(parent, message);
            } else {
                System.out.println("Connection error: " + message);
            }
        });
    }

    @Override
    public void onSettingChange(Room room) {
        this.room = room;
        Platform.runLater(() -> {
            totalCard.setText("Total Cards = " + room.getTotalCard());
            totalRound.setText("Total Rounds = " + room.getTotalRound());
            playerLimit.setText("Max Players = " + room.getPlayerLimit());
            totalCardNow = room.getTotalCard();
            totalRoundNow = room.getTotalRound();
            playerLimitNow = room.getPlayerLimit();
        });
    }

    @FXML
    private void handleStart() {
        if (!isHost) {
            System.out.println("Only host can start the game!");
            return;
        }

        server.startGame();
    }

    @FXML
    private void handleSetting() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/safmica/views/components/room_settings.fxml"));
            Parent root = loader.load();

            RoomSettingsController controller = loader.getController();
        controller.setCurrentTotalCard(room != null ? room.getTotalCard() : 4);
        controller.setCurrentTotalRound(room != null ? room.getTotalRound() : 3);
        controller.setCurrentPlayerLimit(room != null ? room.getPlayerLimit() : 1);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(settingButton.getScene().getWindow());
            modal.setTitle("Room Settings");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            if (controller.isSaved()) {
                int newTotalCard = controller.getSelectedTotalCard();
                int newTotalRound = controller.getSelectedTotalRound();
                int newPlayerLimit = controller.getSelectedPlayerLimit();
                if (newTotalCard != totalCardNow) {
                    if (newTotalCard < 4 || newTotalCard > 6) {
                        System.out.println("DEBUG : TOTAL CARD MUST BETWEEN 4-6");
                        return;
                    }
                    server.updateRoomSettings(newTotalCard, "TOTAL_CARD");
                }
                if (newTotalRound != totalRoundNow) {
                    if (newTotalRound < 3 || newTotalRound > 15) {
                        System.out.println("DEBUG : TOTAL ROUND MUST BETWEEN 3-15");
                        return;
                    } else if (newTotalRound % 2 == 0) {
                        System.out.println("DEBUG : TOTAL ROUND MUST BE AN ODD NUMBER");
                        return;
                    }
                    server.updateRoomSettings(newTotalRound, "TOTAL_ROUND");
                }
                if (newPlayerLimit != playerLimitNow) {
                    if (newPlayerLimit < 1 || newPlayerLimit > 10) {
                        System.out.println("DEBUG : PLAYER LIMIT MUST BETWEEN 1-10");
                        return;
                    }
                    server.updateRoomSettings(newPlayerLimit, "PLAYER_LIMIT");
                }
                System.out.println("Settings saved: Total Rounds = " + newTotalRound);
            }

        } catch (IOException e) {
            LoggerHandler.logError("Failed to load settings", e);
        }
    }

    @FXML
    private void handleExit() {
        cleanup();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            Platform.runLater(() -> {
                try {
                    App.setRoot("menu");
                } catch (IOException | IllegalStateException e) {
                    LoggerHandler.logFXMLFailed("Menu", e);
                }
            });
        }).start();
    }
}
