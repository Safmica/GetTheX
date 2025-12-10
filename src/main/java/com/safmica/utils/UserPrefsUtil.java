package com.safmica.utils;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class UserPrefsUtil {
    private static final Gson gson = new Gson();
    private static final Path DATA_DIR = Paths.get(".", "data", "client");
    private static final Path PREFS = DATA_DIR.resolve("userprefs.json");

    public static synchronized boolean save(UserPrefs prefs) {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            String json = gson.toJson(prefs);
            Files.write(PREFS, json.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            LoggerHandler.logError("Failed saving userprefs.", e);
            return false;
        }
    }

    public static synchronized Optional<UserPrefs> read() {
        try {
            if (!Files.exists(PREFS)) return Optional.empty();
            String json = new String(Files.readAllBytes(PREFS), StandardCharsets.UTF_8);
            UserPrefs p = gson.fromJson(json, UserPrefs.class);
            return Optional.ofNullable(p);
        } catch (IOException e) {
            LoggerHandler.logError("Failed reading userprefs.", e);
            return Optional.empty();
        }
    }
}
