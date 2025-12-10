package com.safmica.network.client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.safmica.listener.GameListener;
import com.safmica.listener.RoomListener;
import com.safmica.model.ClientSaveState;
import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.model.Player;
import com.safmica.model.PlayerEvent;
import com.safmica.model.PlayerLeaderboard;
import com.safmica.model.PlayerSurrender;
import com.safmica.model.Room;
import com.safmica.utils.ClientAutosaveUtil;
import com.safmica.utils.LoggerHandler;

import javafx.application.Platform;

public class TcpClientHandler extends Thread {
    private String host;
    private int port;
    private boolean isHost;
    private Socket client;
    private boolean isRunning = true;
    private Gson gson;
    private DataInputStream in;
    private DataOutputStream out;
    private List<RoomListener> roomListeners = new CopyOnWriteArrayList<>();
    private List<GameListener> gameListeners = new CopyOnWriteArrayList<>();

    public static final String TYPE_PLAYER_LIST = "PLAYER_LIST";
    public static final String TYPE_PLAYER_EVENT = "PLAYER_EVENT";
    public static final String TYPE_CONNECTED = "CONNECTED";
    public static final String TYPE_DISCONNECTED = "DISCONNECTED";
    public static final String TYPE_SETTING_UPDATE = "SETTING_UPDATE";
    public static final String TYPE_GAME_START = "GAME_START";
    public static final String TYPE_CARDS_BROADCAST = "CARDS_BROADCAST";
    public static final String TYPE_SUBMIT_ACK = "SUBMIT_ACK";
    public static final String TYPE_GAME_RESULT = "GAME_RESULT";
    public static final String TYPE_LEADERBOARD_UPDATE = "LEADERBOARD_UPDATE";
    public static final String TYPE_NEXT_ROUND = "NEXT_ROUND";
    public static final String TYPE_ROUND_OVER = "ROUND_OVER";
    public static final String TYPE_PLAYER_SURRENDER = "PLAYER_SURRENDER";
    public static final String TYPE_NEXT_ROUND_WITH_SURRENDER = "NEXT_ROUND_WITH_SURRENDER";
    public static final String TYPE_FINAL_ROUND = "FINAL_ROUND";
    public static final String TYPE_DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
    public static final String TYPE_CHANGE_USERNAME = "CHANGE_USERNAME";
    public static final String TYPE_USERNAME_ACCEPTED = "USERNAME_ACCEPTED";
    public static final String TYPE_ROOM_FULL = "ROOM_FULL";

    private String username;

    public TcpClientHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public TcpClientHandler(String host, int port, String username, boolean isHost) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public void addRoomListener(RoomListener l) {
        roomListeners.add(l);
    }

    public void removeRoomListener(RoomListener l) {
        roomListeners.remove(l);
    }

    public void addGameListener(GameListener l) {
        gameListeners.add(l);
    }

    public void removeGameListener(GameListener l) {
        gameListeners.remove(l);
    }

    public void startClient(String username) {
        this.username = username;
        startClient();
    }

    public void startClient() {
        if (username == null) {
            throw new IllegalStateException("Username must be set before starting client");
        }

        try {
            client = new Socket(host, port);
            this.in = new DataInputStream(client.getInputStream());
            this.out = new DataOutputStream(client.getOutputStream());

            sendMessage(username);
            this.start();
        } catch (IOException e) {
            LoggerHandler.logError("Failed to start client or connect to server.", e);
            String msg = "Failed to connect to server: " + e.getMessage();
            try {
                Platform.runLater(() -> {
                    for (RoomListener l : roomListeners) {
                        try { l.onConnectionError(msg); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
            stopClient();
        } catch (Exception e) {
            LoggerHandler.logError("Unexpected error while starting client.", e);
            String msg = "Unexpected error while starting client: " + e.getMessage();
            try {
                Platform.runLater(() -> {
                    for (RoomListener l : roomListeners) {
                        try { l.onConnectionError(msg); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
            stopClient();
        }
    }

    @Override
    public void run() {
        gson = new Gson();
        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());

            while (isRunning && client != null && !client.isClosed()) {
                int length;
                try {
                    length = dis.readInt();
                } catch (EOFException e) {
                    break;
                }

                if (length <= 0) {
                    continue;
                }

                byte[] buf = new byte[length];
                dis.readFully(buf);

                String json = new String(buf, StandardCharsets.UTF_8);

                JsonElement el;
                try {
                    el = gson.fromJson(json, JsonElement.class);
                } catch (Exception ex) {
                    LoggerHandler.logError("Malformed JSON received: " + json, ex);
                    continue;
                }

                if (!el.isJsonObject())
                    continue;

                JsonObject obj = el.getAsJsonObject();

                String type = obj.has("type") && !obj.get("type").isJsonNull()
                        ? obj.get("type").getAsString()
                        : null;

                if (type == null)
                    continue;

                switch (type) {
                    case TYPE_PLAYER_LIST: {
                        Type listType = new TypeToken<Message<List<Player>>>() {
                        }.getType();
                        Message<List<Player>> listMsg = gson.fromJson(json, listType);
                        List<Player> users = listMsg.data;                        
                        Platform.runLater(() -> {
                            for (RoomListener l : roomListeners) {
                                l.onPlayerListChanged(users);
                            }
                        });
                        break;
                    }

                    case TYPE_PLAYER_EVENT: {
                        Type eventType = new TypeToken<Message<PlayerEvent>>() {
                        }.getType();
                        Message<PlayerEvent> eventMsg = gson.fromJson(json, eventType);
                        if (eventMsg != null && eventMsg.data != null) {
                            PlayerEvent event = eventMsg.data;
                            Platform.runLater(() -> {
                                for (RoomListener l : roomListeners) {
                                    if (event.getEventType().equals(TYPE_CONNECTED)) {
                                        l.onPlayerConnected(event.getUsername());
                                    } else if (event.getEventType().equals(TYPE_DISCONNECTED)) {
                                        l.onPlayerDisconnected(event.getUsername());
                                    }
                                }
                            });
                        }
                        break;
                    }

                    case TYPE_SETTING_UPDATE: {
                        Type roomType = new TypeToken<Message<Room>>() {
                        }.getType();
                        Message<Room> roomInfo = gson.fromJson(json, roomType);
                        Room room = roomInfo.data;
                        System.out.println("DEBUG : total cards" + room.getTotalCard());
                        System.out.println("DEBUG : total rounds" + room.getTotalRound());
                        Platform.runLater(() -> {
                            for (RoomListener l : roomListeners) {
                                l.onSettingChange(room);
                            }
                        });
                        break;
                    }
                    case TYPE_GAME_START: {
                        System.out.println("GAME START");
                        try {
                            if (username != null && host != null && port > 0) {
                                if (!isHost) {
                                    String playerId = null;
                                    Optional<ClientSaveState> maybeExisting = ClientAutosaveUtil.read();
                                    if (maybeExisting.isPresent()) {
                                        playerId = maybeExisting.get().getPlayerId();
                                    }
                                    
                                    ClientSaveState clientSave = new ClientSaveState(
                                        username, host, port, playerId
                                    );
                                    ClientAutosaveUtil.writeAtomic(clientSave);
                                } else {
                                    System.out.println("DEBUG: Host detected, skipping client autosave");
                                }
                            }
                        } catch (Exception e) {
                            LoggerHandler.logError("Failed to create client autosave at round start.", e);
                        }
                        Platform.runLater(() -> {
                            for (RoomListener l : roomListeners) {
                                l.onGameStart();
                            }
                        });
                        break;
                    }
                    case TYPE_CARDS_BROADCAST: {
                        Type gameType = new TypeToken<Message<Game>>() {
                        }.getType();
                        Message<Game> game = gson.fromJson(json, gameType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onCardsBroadcast(game.data);
                            }
                        });
                        break;
                    }
                    case TYPE_SUBMIT_ACK: {
                        System.out.println("CLIENT GOT ACK");
                        Type stringType = new TypeToken<Message<String>>() {
                        }.getType();
                        Message<String> msg = gson.fromJson(json, stringType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onSubmitAck(msg.data);
                            }
                        });
                        break;
                    }
                    case TYPE_GAME_RESULT: {
                        System.out.println("CLIENT GOT GAME RESULT");
                        Type gameAnswerType = new TypeToken<Message<GameAnswer>>() {
                        }.getType();
                        Message<GameAnswer> gameResult = gson.fromJson(json, gameAnswerType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onGetGameResult(gameResult.data);
                            }
                        });
                        break;
                    }
                    case TYPE_LEADERBOARD_UPDATE: {
                        Type playerLeaderboardType = new TypeToken<Message<List<PlayerLeaderboard>>>() {
                        }.getType();
                        Message<List<PlayerLeaderboard>> leaderboard = gson.fromJson(json, playerLeaderboardType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onLeaderboardUpdate(leaderboard.data);
                            }
                        });
                        break;
                    }
                    case TYPE_NEXT_ROUND: {
                        System.out.println("NEXT ROUND");
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onNextRound();
                            }
                        });
                        break;
                    }
                    case TYPE_ROUND_OVER: {
                        System.out.println("ROUND OVER");
                        try {
                            ClientAutosaveUtil.delete();
                            System.out.println("DEBUG: Client autosave deleted at round over");
                        } catch (Exception e) {
                            LoggerHandler.logError("Failed to delete client autosave at round over.", e);
                        }
                        
                        Type msgType = new TypeToken<Message<String>>() {
                        }.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onRoundOver(msg.data);
                            }
                        });
                        break;
                    }
                    case TYPE_PLAYER_SURRENDER: {
                        Type msgType = new TypeToken<Message<PlayerSurrender>>() {
                        }.getType();
                        Message<PlayerSurrender> msg = gson.fromJson(json, msgType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onPlayerSurrender(msg.data);
                            }
                        });
                        break;
                    }
                    case TYPE_NEXT_ROUND_WITH_SURRENDER: {
                        Type msgType = new TypeToken<Message<String>>() {
                        }.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onNextRoundWithSurrender(msg.data);
                            }
                        });
                        break;
                    }
                    case TYPE_FINAL_ROUND: {
                        Type msgType = new TypeToken<Message<List<String>>>() {
                        }.getType();
                        Message<List<String>> msg = gson.fromJson(json, msgType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onFinalRound(msg.data);
                            }
                        });
                        break;
                    }
                    case TYPE_DUPLICATE_USERNAME: {
                        Type msgType = new TypeToken<Message<String>>() {}.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        if (msg != null && msg.data != null) {
                            String assigned = msg.data;
                            this.username = assigned;
                            Platform.runLater(() -> {
                                for (RoomListener l : roomListeners) {
                                    l.onDuplicateUsernameAssigned(assigned);
                                }
                            });
                        }
                        break;
                    }
                    case TYPE_USERNAME_ACCEPTED: {
                        Type msgType = new TypeToken<Message<String>>() {}.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        if (msg != null && msg.data != null) {
                            String newName = msg.data;
                            this.username = newName;
                            Platform.runLater(() -> {
                                for (RoomListener l : roomListeners) {
                                    l.onUsernameAccepted(newName);
                                }
                            });
                        }
                        break;
                    }
                    case TYPE_ROOM_FULL: {
                        Type msgType = new TypeToken<Message<String>>() {}.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        String message = (msg != null && msg.data != null) ? msg.data : "Room is full";
                        Platform.runLater(() -> {
                            for (RoomListener l : roomListeners) {
                                l.onConnectionError(message);
                            }
                        });
                        stopClient();
                        break;
                    }
                    default: {
                        // todo: give some handle (if not lazy)
                    }
                }
            }
        } catch (SocketException e) {
            if (isRunning) {
                System.out.println("WARNING: Connection to server lost.");
                try {
                    javafx.application.Platform.runLater(() -> {
                        for (RoomListener l : roomListeners) {
                            try { l.onConnectionError("Connection to server lost"); } catch (Exception ignored) {}
                        }
                    });
                } catch (Exception ignored) {}
                stopClient();
            }
        } catch (IOException e) {
            if (isRunning) {
                LoggerHandler.logError("Error in client handler run loop.", e);
            }
        } finally {
            cleanup();
        }
    }

    public void gameMsg(GameAnswer answer) {
        Message<GameAnswer> msg = new Message<>("GAME_ANSWER", answer);
        msg(msg);
    }

    public void offerSurrender() {
        Message<String> msg = new Message<>("OFFER_SURRENDER", username);
        msg(msg);
    }

    public void requestChangeUsername(String newName) {
        if (newName == null || newName.trim().isEmpty()) return;
        Message<String> msg = new Message<>(TYPE_CHANGE_USERNAME, newName.trim());
        String json = new Gson().toJson(msg);
        sendMessage(json);
    }

    public void requestReconnect(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) return;
        Message<String> msg = new Message<>(TYPE_RECONNECT_REQUEST, playerId.trim());
        String json = new Gson().toJson(msg);
        sendMessage(json);
    }

    private void msg(Message<?> message) {
        String json = gson.toJson(message);
        sendMessage(json);
    }

    public void sendMessage(String message) {
        try {
            if (out != null && !client.isClosed()) {
                byte[] data = message.getBytes(StandardCharsets.UTF_8);

                out.writeInt(data.length);
                out.write(data);
                out.flush();
            }
        } catch (Exception e) {
            // TODO: handle disconnect or cleanup
        }
    }

    private void cleanup() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception e) {
        }

        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
        }
    }

    public void stopClient() {
        this.isRunning = false;

        new Thread(() -> {
            try {
                if (client != null && !client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            cleanup();
        }).start();
    }
}