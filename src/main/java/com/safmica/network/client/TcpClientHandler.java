package com.safmica.network.client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.safmica.listener.GameListener;
import com.safmica.listener.RoomListener;
import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.model.Player;
import com.safmica.model.PlayerEvent;
import com.safmica.model.Room;
import com.safmica.network.server.ClientHandler;
import com.safmica.utils.LoggerHandler;

import javafx.application.Platform;

public class TcpClientHandler extends Thread {
    private String host;
    private int port;
    private Socket client;
    private boolean isRunning = true;
    private Gson gson;
    private BufferedReader in;
    private PrintWriter out;
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

    private String username;

    public TcpClientHandler(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public TcpClientHandler(String host, int port, String username) {
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
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            out.println(username);
            this.start();
        } catch (IOException e) {
            LoggerHandler.logError("Failed to start client or connect to server.", e);
            stopClient();
        } catch (Exception e) {
            LoggerHandler.logError("Unexpected error while starting client.", e);
            stopClient();
        }
    }

    @Override
    public void run() {
        gson = new Gson();
        try {
            while (isRunning && client != null && !client.isClosed()) {
                String line = in.readLine();
                if (line == null)
                    break;

                JsonElement el = null;
                try {
                    JsonReader jrRoot = new JsonReader(new java.io.StringReader(line));
                    jrRoot.setLenient(true);
                    el = gson.fromJson(jrRoot, JsonElement.class);
                } catch (Exception ex) {
                    LoggerHandler.logError("Malformed JSON received from server: " + line, ex);
                    continue;
                }
                if (el == null || !el.isJsonObject())
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
                        Message<List<Player>> listMsg = gson.fromJson(line, listType);
                        List<Player> users = listMsg.data;
                        System.out.println("user" + users);
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
                        Message<PlayerEvent> eventMsg = gson.fromJson(line, eventType);
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
                        Message<Room> roomInfo = gson.fromJson(line, roomType);
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
                        Message<Game> game = gson.fromJson(line, gameType);
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
                        Message<String> msg = gson.fromJson(line, stringType);
                        Platform.runLater(() -> {
                            for (GameListener l : gameListeners) {
                                l.onSubmitAck(msg.data);
                            }
                        });
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

    private void msg(Message<?> message) {
        String json = gson.toJson(message);
        sendMessage(json);
    }

    public void sendMessage(String message) {
        try {
            if (out != null && !client.isClosed()) {
                out.println(message);
                out.flush();
            }
        } catch (Exception e) {
            //todo: give some handle
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