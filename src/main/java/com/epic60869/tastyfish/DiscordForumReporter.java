package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Sends user-owned farming reports through a Discord forum webhook. */
public final class DiscordForumReporter {
    private static final Gson GSON = new Gson();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public void session(String webhook, String username, FarmingHistory.Session session) {
        if (blank(webhook) || session == null) return;
        String title = "🌾 Farming Session — " + username + " — " + session.crop();
        String body = "## 🌾 Farming Session\n" +
            "**Player:** " + safe(username) + "\n" +
            "**Crop:** " + safe(session.crop()) + "\n" +
            "**Duration:** " + formatDuration(session.activeMillis()) + "\n" +
            "**Profit:** " + formatNumber(session.profit()) + " coins\n" +
            "**Actions:** " + formatNumber(session.actions()) + "\n" +
            "**Pests:** " + formatNumber(sum(session.pests())) + "\n" +
            "\n**Items**\n" + formatMap(session.items()) + "\n" +
            "\nSession ID: `" + safe(session.sessionId()) + "`";
        post(webhook, title, body);
    }

    public void personalBest(String webhook, String username, long profit) {
        if (blank(webhook)) return;
        post(webhook, "🏆 1-Hour Personal Best — " + username,
            "## 🏆 New 1-Hour Personal Best\n**Player:** " + safe(username) +
            "\n**1-hour farming profit:** **" + formatNumber(profit) + " coins**");
    }

    public void streak(String webhook, String username, long streakMs) {
        if (blank(webhook) || streakMs <= 0) return;
        post(webhook, "🔥 Farming Streak — " + username,
            "## 🔥 Farming Streak\n**Player:** " + safe(username) +
            "\n**New streak:** **" + formatDuration(streakMs) + "**");
    }

    public void achievement(String webhook, String username, String achievement) {
        if (blank(webhook)) return;
        post(webhook, "🏅 Farming Achievement — " + username,
            "## 🏅 Achievement Unlocked\n**Player:** " + safe(username) +
            "\n**Achievement:** **" + safe(achievement) + "**");
    }

    private void post(String webhook, String threadName, String content) {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("content", content);
            root.addProperty("thread_name", threadName);
            root.addProperty("wait", true);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(root)))
                .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    System.err.println("[TastyFish] Discord forum report failed: HTTP " + response.statusCode());
                }
            }).exceptionally(error -> {
                System.err.println("[TastyFish] Discord forum report failed: " + error.getMessage());
                return null;
            });
        } catch (Exception e) {
            System.err.println("[TastyFish] Discord forum report failed: " + e.getMessage());
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String safe(String value) { return value == null ? "" : value.replace("@", "@​"); }
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
        if (map == null || map.isEmpty()) return "No tracked items.";
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            out.append("- `").append(e.getKey()).append("`: ").append(String.format("%,d", e.getValue())).append('\n');
            if (++count >= 25) break;
        }
        return out.length() == 0 ? "No tracked items." : out.toString();
    }
}
