package com.safmica.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.safmica.model.SaveState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class AutosaveUtil {
    private static final Gson gson = new Gson();
    private static final Path DATA_DIR = Paths.get(".", "data", "server");
    private static final Path AUTOSAVE_PATH = DATA_DIR.resolve("autosave.json");

    public static synchronized boolean writeAtomic(SaveState state) {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }

            String json = gson.toJson(state);
            Path temp = DATA_DIR.resolve("autosave.json.tmp");
            Files.write(temp, json.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, AUTOSAVE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicEx) {
                Files.move(temp, AUTOSAVE_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LoggerHandler.logError("Failed writing autosave file.", e);
            return false;
        }
    }

    public static synchronized Optional<SaveState> read() {
        try {
            if (!Files.exists(AUTOSAVE_PATH)) return Optional.empty();
            byte[] bytes = Files.readAllBytes(AUTOSAVE_PATH);
            String json = new String(bytes, StandardCharsets.UTF_8);
            SaveState state = gson.fromJson(json, SaveState.class);
            return Optional.ofNullable(state);
        } catch (JsonSyntaxException | IOException e) {
            LoggerHandler.logError("Failed reading/parsing autosave file.", e);
            return Optional.empty();
        }
    }

    public static synchronized boolean delete() {
        try {
            if (Files.exists(AUTOSAVE_PATH)) {
                Files.delete(AUTOSAVE_PATH);
            }
            return true;
        } catch (IOException e) {
            LoggerHandler.logError("Failed deleting autosave file.", e);
            return false;
        }
    }
}
