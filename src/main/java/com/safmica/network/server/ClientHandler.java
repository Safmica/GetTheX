package com.safmica.network.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.utils.LoggerHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends Thread {
    private final TcpServerHandler server;
    private Socket client;
    private Gson gson;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;
    private final String TYPE_GAME_ANSWER = "GAME_ANSWER";
    private final String TYPE_OFFER_SURRENDER = "OFFER_SURRENDER";
    private final String TYPE_CHANGE_USERNAME = "CHANGE_USERNAME";
    private final String TYPE_DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
    private final String TYPE_USERNAME_ACCEPTED = "USERNAME_ACCEPTED";
    private final String TYPE_RECONNECT_REQUEST = "RECONNECT_REQUEST";
    private final String TYPE_RECONNECT_ACCEPT = "RECONNECT_ACCEPT";
    private final String TYPE_RECONNECT_REJECT = "RECONNECT_REJECT";

    public ClientHandler(Socket client, TcpServerHandler server, String username) {
        this.client = client;
        this.server = server;
        this.username = username;
        try {
            this.in = new DataInputStream(client.getInputStream());
            this.out = new DataOutputStream(client.getOutputStream());

        } catch (IOException e) {
            LoggerHandler.logError("Error initializing streams for client: " + client.getInetAddress().getHostAddress(),
                    e);
        }
    }

    @Override
    public void run() {
        gson = new Gson();
        try {
            DataInputStream dis = new DataInputStream(client.getInputStream());

            while (isConnected() && client != null && !client.isClosed()) {
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
                    case TYPE_GAME_ANSWER: {
                        System.out.println("SERVER GOT SUBMIT");
                        Type listType = new TypeToken<Message<GameAnswer>>() {
                        }.getType();
                        Message<GameAnswer> listMsg = gson.fromJson(json, listType);
                        GameAnswer listAnswers = listMsg.data;
                        if (listAnswers != null) {
                            if (listAnswers.username == null || listAnswers.username.isEmpty()) {
                                listAnswers.username = this.username;
                            }
                            server.enqueueAnswer(listAnswers);
                        }
                        break;
                    }
                    case TYPE_OFFER_SURRENDER: {
                        System.out.println("SERVER GOT OFFER SURRENDER");
                        Type msgType = new TypeToken<Message<String>>() {
                        }.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        if (msg.data != null) {
                            server.offerSurrender(msg.data);
                        }
                        break;
                    }
                    case TYPE_CHANGE_USERNAME: {
                        Type msgType = new TypeToken<Message<String>>() {}.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        if (msg != null && msg.data != null && !msg.data.trim().isEmpty()) {
                            String newName = msg.data.trim();
                            boolean ok = server.changeUsername(this, newName);
                            if (ok) {
                                Message<String> okMsg = new Message<>(TYPE_USERNAME_ACCEPTED, newName);
                                String okJson = gson.toJson(okMsg);
                                sendMessage(okJson);
                            } else {
                                Message<String> dupMsg = new Message<>(TYPE_DUPLICATE_USERNAME, this.username);
                                String dupJson = gson.toJson(dupMsg);
                                sendMessage(dupJson);
                            }
                        }
                        break;
                    }
                    case TYPE_RECONNECT_REQUEST: {
                        Type msgType = new TypeToken<Message<String>>() {}.getType();
                        Message<String> msg = gson.fromJson(json, msgType);
                        if (msg != null && msg.data != null) {
                            String requestedId = msg.data.trim();
                            boolean ok = server.acceptReconnect(this, requestedId);
                            if (ok) {
                                Message<String> okMsg = new Message<>(TYPE_RECONNECT_ACCEPT, this.username);
                                sendMessage(gson.toJson(okMsg));
                            } else {
                                Message<String> rejMsg = new Message<>(TYPE_RECONNECT_REJECT, requestedId);
                                sendMessage(gson.toJson(rejMsg));
                            }
                        }
                        break;
                    }
                    default: {
                        // todo: give some handle (if not lazy)
                    }
                }
            }
        } catch (SocketException e) {
            if (isConnected()) {
                System.out.println("WARNING: Connection to client lost.");
            }
        } catch (IOException e) {
            if (isConnected()) {
                LoggerHandler.logError("Error in client handler run loop.", e);
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            LoggerHandler.logError("Error closing client socket for " + username, e);
        }

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
            server.unregisterClient(username, this);
        } catch (Exception e) {
            LoggerHandler.logError("Error unregistering client " + username, e);
        }
    }

    public synchronized void sendMessage(String message) {
        try {
            if (out != null && !client.isClosed()) {
                byte[] data = message.getBytes(StandardCharsets.UTF_8);

                out.writeInt(data.length);
                out.write(data);
                out.flush();
            }
        } catch (Exception e) {
            disconnect();
            cleanup();
        }
    }

    public boolean usernameEquals(String other) {
        if (other == null)
            return false;
        return other.equals(this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isConnected() {
        return client != null && !client.isClosed() && client.isConnected();
    }

    public void disconnect() {
        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            LoggerHandler.logError("Error closing client socket for " + username, e);
        }
    }
}