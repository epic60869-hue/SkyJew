package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Sends reports to the TastyFish website, which uses its Discord bot to post
 * into the configured channel/forum. The client never stores a Discord bot
 * token or webhook URL.
 */
public final class DiscordForumReporter {
    private static final Gson GSON = new Gson();
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public void session(TastyFishConfig config, String username, FarmingHistory.Session session) {
        if (!enabled(config) || session == null) return;
        post(config, "session", username, "Farming Session — " + username + " — " + session.crop(),
            sessionBody(username, session));
    }

    public void personalBest(TastyFishConfig config, String username, long profit) {
        if (!enabled(config)) return;
        post(config, "personal_best", username, "1-Hour Personal Best — " + username,
            "## 🏆 New 1-Hour Personal Best\n**Player:** " + safe(username) +
                "\n**1-hour farming profit:** **" + formatNumber(profit) + " coins**");
    }

    public void streak(TastyFishConfig config, String username, long streakMs) {
        if (!enabled(config) || streakMs <= 0) return;
        post(config, "streak", username, "Farming Streak — " + username,
            "## 🔥 Farming Streak\n**Player:** " + safe(username) +
                "\n**New streak:** **" + formatDuration(streakMs) + "**");
    }

    public void achievement(TastyFishConfig config, String username, String achievement) {
        if (!enabled(config)) return;
        post(config, "achievement", username, "Farming Achievement — " + username,
            "## 🏅 Achievement Unlocked\n**Player:** " + safe(username) +
                "\n**Achievement:** **" + safe(achievement) + "**");
    }

    private void post(TastyFishConfig config, String reportType, String username, String title, String content) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("type", reportType);
            root.addProperty("username", safe(username));
            root.addProperty("title", title);
            root.addProperty("content", content);
            root.addProperty("channelId", clean(config.discordChannelId));
            root.addProperty("forumId", clean(config.discordForumId));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.discordReportEndpoint.trim()))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");

            if (!blank(config.discordReportSecret)) {
                builder.header("Authorization", "Bearer " + config.discordReportSecret.trim());
            }

            HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(root)))
                .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    System.err.println("[TastyFish] Discord report relay failed: HTTP " + response.statusCode());
                }
            }).exceptionally(error -> {
                System.err.println("[TastyFish] Discord report relay failed: " + error.getMessage());
                return null;
            });
        } catch (Exception e) {
            System.err.println("[TastyFish] Discord report relay failed: " + e.getMessage());
        }
    }

    private static boolean enabled(TastyFishConfig config) {
        return config != null && config.discordForumEnabled
            && !blank(config.discordReportEndpoint)
            && (!blank(config.discordChannelId) || !blank(config.discordForumId));
    }

    private static String sessionBody(String username, FarmingHistory.Session session) {
        return "## 🌾 Farming Session\n" +
            "**Player:** " + safe(username) + "\n" +
            "**Crop:** " + safe(session.crop()) + "\n" +
            "**Duration:** " + formatDuration(session.activeMillis()) + "\n" +
            "**Profit:** " + formatNumber(session.profit()) + " coins\n" +
            "**Actions:** " + formatNumber(session.actions()) + "\n" +
            "**Pests:** " + formatNumber(sum(session.pests())) + "\n" +
            "\n**Items**\n" + formatMap(session.items()) +
            "\nSession ID: `" + safe(session.sessionId()) + "`";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String safe(String value) { return value == null ? "" : value.replace("@", "@\u200b"); }
    private static long sum(Map<String, Long> map) { return map == null ? 0L : map.values().stream().mapToLong(Long::longValue).sum(); }
    private static String formatNumber(double value) { return String.format("%,.0f", value); }
    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        return h > 0 ? String.format("%dh %02dm %02ds", h, m, s) : String.format("%dm %02ds", m, s);
    }
    private static String formatMap(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "No tracked items.\n";
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            out.append("- `").append(e.getKey()).append("`: ").append(String.format("%,d", e.getValue())).append('\n');
            if (++count >= 25) break;
        }
        return out.length() == 0 ? "No tracked items.\n" : out.toString();
    }
}
