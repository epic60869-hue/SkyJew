package com.epic60869.skyjew;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tells you in chat when a newer SkyJew is out: "New SkyJew Mod Version 1.2.3 --> 1.2.5" (the newest release, even if
 * you're several versions behind), with a link to the download. Checked from GitHub's latest release when you join a
 * server, at most every few hours, and each new version is only announced once per game session.
 */
public final class SkyJewUpdateChecker {
    private static final String LATEST_URL = "https://api.github.com/repos/2m3s/SkyJew/releases/latest";
    private static final long CHECK_EVERY_MS = 3 * 60 * 60_000L;

    private static long lastCheck;
    private static String announced = "";

    private SkyJewUpdateChecker() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> check());
    }

    private static boolean enabled() {
        SkyJewConfig c = SkyJewConfig.current();
        return c == null || c.misc.updateNotifications;
    }

    private static String installed() {
        return FabricLoader.getInstance().getModContainer("skyjew")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    private static void check() {
        long now = System.currentTimeMillis();
        if (!enabled() || now - lastCheck < CHECK_EVERY_MS) return;
        lastCheck = now;
        HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_URL))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "SkyBalls/" + installed())
            .GET().build();
        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() != 200) return;
            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            String latest = release.get("tag_name").getAsString().replaceFirst("^[vV]", "").trim();
            String url = release.has("html_url") ? release.get("html_url").getAsString() : "https://github.com/2m3s/SkyJew/releases/latest";
            String current = installed();
            if (current.isEmpty() || compare(latest, current) <= 0 || latest.equals(announced)) return;
            announced = latest;
            // Give the server's join messages a moment so this doesn't get buried.
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> announce(current, latest, url));
        }).exceptionally(e -> null);
    }

    private static void announce(String current, String latest, String url) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;
            Component message = Component.literal("New SkyBalls Mod Version ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(current).withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(false)))
                .append(Component.literal(" --> ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withBold(false)))
                .append(Component.literal(latest).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(false)))
                .append(Component.literal("  [Download]").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(false).withUnderlined(true)
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the SkyBalls " + latest + " release on GitHub")))));
            mc.gui.hud.getChat().addClientSystemMessage(message);
        });
    }

    /** Compares versions like "1.2.10" and "1.2.9" number by number; a missing part counts as 0. */
    static int compare(String a, String b) {
        String[] x = a.split("[.\\-+]"), y = b.split("[.\\-+]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? number(x[i]) : 0;
            int q = i < y.length ? number(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int number(String part) {
        String digits = part.replaceAll("\\D.*$", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
