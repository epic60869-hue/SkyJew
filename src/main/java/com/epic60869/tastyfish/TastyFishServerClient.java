package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client for the standalone TastyFish farming server.
 * The Minecraft mod no longer talks to the main bot/web application for farming.
 */
public final class TastyFishServerClient {
    private static final Gson GSON = new Gson();
    private static final String MOD_VERSION = "1.0.8";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final AtomicBoolean uploadInProgress = new AtomicBoolean(false);
    private final AtomicBoolean reportInProgress = new AtomicBoolean(false);

    public void upload(TastyFishConfig config, String username, UUID uuid, String profile, String sessionId,
                       SkysoftSessionReader.Snapshot snapshot) {
        if (config == null || !config.farmingServerEnabled || config.farmingServerEndpoint.isBlank()
            || config.farmingServerApiKey.isBlank() || snapshot == null || !snapshot.valid()) return;
        if (username == null || username.isBlank() || uuid == null || sessionId == null || sessionId.isBlank()) return;
        if (!uploadInProgress.compareAndSet(false, true)) return;

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("uuid", uuid.toString());
        body.addProperty("profile", profile == null ? "" : profile);
        body.addProperty("sessionId", sessionId);
        body.addProperty("modVersion", MOD_VERSION);
        body.addProperty("profit", snapshot.profit());
        body.addProperty("activeMillis", snapshot.activeMillis());
        body.addProperty("actions", snapshot.actions());
        body.add("items", GSON.toJsonTree(snapshot.items()));
        body.add("pests", GSON.toJsonTree(snapshot.pests()));

        post(config, "/v1/ingest", body, "farming update")
            .whenComplete((ignored, error) -> {
                uploadInProgress.set(false);
                if (error != null) System.err.println("[TastyFish] Farming server update failed: " + rootMessage(error));
            });
    }

    public void report(TastyFishConfig config, String username, String type, String message) {
        if (config == null || !config.farmingServerEnabled || config.farmingServerEndpoint.isBlank()
            || config.farmingServerApiKey.isBlank() || username == null || username.isBlank() || message == null || message.isBlank()) return;
        if (!reportInProgress.compareAndSet(false, true)) return;

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("type", type);
        body.addProperty("message", message);

        post(config, "/v1/report", body, "Discord report")
            .whenComplete((ignored, error) -> {
                reportInProgress.set(false);
                if (error != null) System.err.println("[TastyFish] Farming server Discord report failed: " + rootMessage(error));
            });
    }

    private java.util.concurrent.CompletableFuture<String> post(TastyFishConfig config, String path, JsonObject body, String label) {
        String base = config.farmingServerEndpoint.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(base + path))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-TastyFish-Mod-Key", config.farmingServerApiKey.trim())
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    throw new RuntimeException(label + " HTTP " + response.statusCode() + ": " + response.body());
                return response.body();
            });
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    public static String newSessionId() {
        return UUID.randomUUID().toString();
    }
}
