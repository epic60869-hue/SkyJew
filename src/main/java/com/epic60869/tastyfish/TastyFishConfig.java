package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TastyFishConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Farming is served through the public Tasty Fish website and proxied internally.
    public boolean farmingServerEnabled = true;
    public String farmingServerEndpoint = "https://tastyfish.org/api/farming";
    public String farmingServerApiKey = "";
    public int uploadIntervalSeconds = 30;

    // One Discord destination is used for farming reports. The server detects whether
    // the ID belongs to a normal text channel or a Discord forum channel.
    public String discordDestinationId = "";

    // Legacy fields retained only so older config files continue to load.
    @Deprecated public String endpoint = "https://tastyfish.org/api/farming";
    @Deprecated public String discordChannelId = "";
    @Deprecated public String discordForumId = "";
    @Deprecated public String discordReportEndpoint = "https://tastyfish.org/api/farming/v1/report";
    @Deprecated public String discordReportSecret = "";

    public boolean enabled = true;
    public boolean farmingRngEnabled = true;
    public boolean farmingRngBackground = false;
    public int farmingRngX = 8;
    public int farmingRngY = 8;
    public float farmingRngScale = 1.0f;

    public boolean farmingAnalyticsEnabled = true;
    public boolean farmingSessionRecorderEnabled = true;
    public boolean farmingPersonalBestEnabled = true;
    public boolean farmingStreakEnabled = true;
    public boolean farmingAchievementsEnabled = true;

    public boolean discordForumEnabled = true;
    public boolean discordSendSessions = true;
    public boolean discordSendPersonalBests = true;
    public boolean discordSendStreaks = true;
    public boolean discordSendAchievements = true;

    // Public Tasty Fish website is intentionally fixed; it is not user-editable.
    public boolean guildLeaderboardHudEnabled = true;
    public String guildLeaderboardWebsite = "https://tastyfish.org";
    public int guildLeaderboardRefreshSeconds = 30;
    public int guildLeaderboardHudX = 8;
    public int guildLeaderboardHudY = 8;
    public float guildLeaderboardHudScale = 1.0f;

    public static TastyFishConfig load(Path path) {
        try {
            if (Files.notExists(path)) {
                TastyFishConfig c = new TastyFishConfig();
                c.save(path);
                return c;
            }
            TastyFishConfig c = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), TastyFishConfig.class);
            if (c == null) c = new TastyFishConfig();
            if (c.uploadIntervalSeconds < 10) c.uploadIntervalSeconds = 10;
            c.farmingRngScale = Math.max(0.5f, Math.min(3.0f, c.farmingRngScale));
            if (c.guildLeaderboardRefreshSeconds < 10) c.guildLeaderboardRefreshSeconds = 10;
            c.guildLeaderboardHudScale = Math.max(0.5f, Math.min(3.0f, c.guildLeaderboardHudScale));
            if (c.farmingServerEndpoint == null || c.farmingServerEndpoint.isBlank()) c.farmingServerEndpoint = "https://tastyfish.org/api/farming";
            c.guildLeaderboardWebsite = "https://tastyfish.org";
            if (c.endpoint == null || c.endpoint.isBlank()) c.endpoint = c.farmingServerEndpoint;
            if (c.discordReportEndpoint == null || c.discordReportEndpoint.isBlank()) c.discordReportEndpoint = c.farmingServerEndpoint + "/v1/report";
            if (c.discordDestinationId == null || c.discordDestinationId.isBlank()) {
                if (c.discordChannelId != null && !c.discordChannelId.isBlank()) c.discordDestinationId = c.discordChannelId.trim();
                else if (c.discordForumId != null && !c.discordForumId.isBlank()) c.discordDestinationId = c.discordForumId.trim();
            }
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
