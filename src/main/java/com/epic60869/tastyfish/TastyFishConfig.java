package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TastyFishConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean farmingRngEnabled = true;
    public boolean farmingRngBackground = false;
    public int farmingRngX = 8;
    public int farmingRngY = 8;
    public float farmingRngScale = 1.0f;

    public static TastyFishConfig load(Path path) {
        try {
            if (Files.notExists(path)) {
                TastyFishConfig c = new TastyFishConfig();
                c.save(path);
                return c;
            }

            TastyFishConfig c = GSON.fromJson(
                Files.readString(path, StandardCharsets.UTF_8),
                TastyFishConfig.class
            );

            if (c == null) c = new TastyFishConfig();
            c.farmingRngScale = Math.max(0.5f, Math.min(3.0f, c.farmingRngScale));
            return c;
        } catch (Exception e) {
            System.err.println("[TastyFish] Failed to load config: " + e.getMessage());
            return new TastyFishConfig();
        }
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[TastyFish] Failed to save config: " + e.getMessage());
        }
    }
}
