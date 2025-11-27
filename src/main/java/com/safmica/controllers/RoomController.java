package com.safmica.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private int totalRoundNow;
    private int totalCardNow;

    private TcpClientHandler clientHandler;
    private TcpServerHandler server;
    private boolean isHost;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        playersList.setItems(players);
        playersList.setCellFactory(listView -> new PlayerListCell());
    }

    public void initAsHost(int port, String username) {
        this.isHost = true;
        server = new TcpServerHandler(port);

        try {
            server.startServer();
            System.out.println("DEBUG : SERVER START ON PORT " + port);

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

        } catch (IOException ex) {
            LoggerHandler.logError("Error starting server", ex);
        }
    }

    public void initAsClient(String host, int port, String username) {
        this.isHost = false;

        connectAsClient(host, port, username);
    }

    private void connectAsClient(String host, int port, String username) {
        clientHandler = new TcpClientHandler(host, port, username);
        clientHandler.addRoomListener(this);
        clientHandler.startClient();
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
            // TODO: Add some notify ui lol
        });
    }

    @Override
    public void onPlayerDisconnected(String username) {
        Platform.runLater(() -> {
            System.out.println(username + " left the room");

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

            // TODO: Add some notify ui lol
        });
    }

    @Override
    public void onSettingChange(Room room) {
        Platform.runLater(() -> {
            totalCard.setText("Total Cards = " + room.getTotalCard());
            totalRound.setText("Total Rounds = " + room.getTotalRound());
        });
    }

    @FXML
    private void handleStart() {
        if (!isHost) {
            System.out.println("Only host can start the game!");
            return;
        }

        // TODO : BROADCAST TO CLIENT
        try {
            App.setRoot("game");
        } catch (IOException | IllegalStateException e) {
            LoggerHandler.logFXMLFailed("Game", e);
        }
        // TODO: Implement start game logic
    }

    @FXML
    private void handleSetting() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/safmica/views/components/room_settings.fxml"));
            Parent root = loader.load();

            RoomSettingsController controller = loader.getController();

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(settingButton.getScene().getWindow());
            modal.setTitle("Room Settings");
            modal.setScene(new Scene(root));
            modal.showAndWait();

            if (controller.isSaved()) {
                int newTotalCard = controller.getSelectedTotalCard();
                int newTotalRound = controller.getSelectedTotalRound();
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
