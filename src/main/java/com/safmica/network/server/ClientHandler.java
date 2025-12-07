package com.safmica.network.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.utils.LoggerHandler;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketException;
 

public class ClientHandler extends Thread {
    private final TcpServerHandler server;
    private Socket client;
    private Gson gson;
    private String username;
    private BufferedReader in;
    private PrintWriter out;
    private final String TYPE_GAME_ANSWER = "GAME_ANSWER";

    public ClientHandler(Socket client, TcpServerHandler server, String username) {
        this.client = client;
        this.server = server;
        this.username = username;
        try {
            this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            this.out = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException e) {
            LoggerHandler.logError("Error initializing streams for client: " + client.getInetAddress().getHostAddress(),
                    e);
        }
    }

    @Override
    public void run() {
        gson = new Gson();
        try {
            while (isConnected() && client != null && !client.isClosed()) {
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
                    case TYPE_GAME_ANSWER: {
                        System.out.println("SERVER GOT SUBMIT");
                        Type listType = new TypeToken<Message<GameAnswer>>() {
                        }.getType();
                        Message<GameAnswer> listMsg = gson.fromJson(line, listType);
                        GameAnswer listAnswers = listMsg.data;
                        if (listAnswers != null) {
                            if (listAnswers.username == null || listAnswers.username.isEmpty()) {
                                listAnswers.username = this.username;
                            }
                            server.enqueueAnswer(listAnswers);

                            Message<String> ack = new Message<>("SUBMIT_ACK", "RECEIVED");
                            String ackJson = gson.toJson(ack);
                            sendMessage(ackJson);
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

    public void sendMessage(String message) {
        try {
            if (out != null && !client.isClosed()) {
                out.println(message);
                out.flush();
            }
        } catch (Exception e) {
            // todo: give some handle
        }
    }

    public boolean usernameEquals(String other) {
        if (other == null) return false;
        return other.equals(this.username);
    }

    public boolean isConnected() {
        return client != null && !client.isClosed() && client.isConnected();
    }
}