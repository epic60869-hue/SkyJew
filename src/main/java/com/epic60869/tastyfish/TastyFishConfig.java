package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TastyFishConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String endpoint = "https://shadowisabot.com/api/farming/update";
    public String apiKey = "PUT_YOUR_FARMING_API_KEY_HERE";
    public int uploadIntervalSeconds = 30;
    public boolean enabled = true;
    public boolean farmingRngEnabled = true;
    public boolean farmingRngBackground = false;
    public int farmingRngX = 8;
    public int farmingRngY = 8;
    public float farmingRngScale = 1.0f;

    // Farming analytics
    public boolean farmingAnalyticsEnabled = true;
    public boolean farmingSessionRecorderEnabled = true;
    public boolean farmingPersonalBestEnabled = true;
    public boolean farmingStreakEnabled = true;
    public boolean farmingAchievementsEnabled = true;
    public String discordForumWebhook = "";
    public boolean discordForumEnabled = false;
    public boolean discordSendSessions = true;
    public boolean discordSendPersonalBests = true;
    public boolean discordSendStreaks = true;
    public boolean discordSendAchievements = true;

    public static TastyFishConfig load(Path path) {
        try {
            if (Files.notExists(path)) { TastyFishConfig c = new TastyFishConfig(); c.save(path); return c; }
            TastyFishConfig c = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), TastyFishConfig.class);
            if (c == null) c = new TastyFishConfig();
            if (c.uploadIntervalSeconds < 10) c.uploadIntervalSeconds = 10;
            c.farmingRngScale = Math.max(0.5f, Math.min(3.0f, c.farmingRngScale));
            return c;
        } catch (Exception e) { System.err.println("[TastyFish] Failed to load config: " + e.getMessage()); return new TastyFishConfig(); }
    }

    public void save(Path path) {
        try { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8); }
        catch (IOException e) { System.err.println("[TastyFish] Failed to save config: " + e.getMessage()); }
    }
}
