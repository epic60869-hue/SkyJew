package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;\nimport net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SkyJewGlobalChat {
    private static final String RELAY_URL =
        System.getProperty("skyjew.chat.url", "wss://skyjew.org/tf-chat");

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);
    private static final AtomicLong REQUEST_IDS = new AtomicLong();
    private static volatile WebSocket socket;
    private static volatile long reconnectAt = 0L;
    private static volatile String username = "Unknown";

    private SkyJewGlobalChat() {}

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        username = mc.getUser().getName();
        connect();
    }

    public static void send(String message) {
        String clean = String.valueOf(message == null ? "" : message).trim();
        if (clean.isEmpty()) return;

        WebSocket ws = socket;
        if (ws == null) {
            connect();
            mcMessage(Component.literal("[Mod] Global chat is still connecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        if (!ws.isInputClosed() && !ws.isOutputClosed()) {
            JsonObject packet = new JsonObject();
            packet.addProperty("type", "message");
            packet.addProperty("username", username);\n            packet.addProperty("nickname", SkyJewNick.outgoingName());\n            packet.addProperty("nicknameMode", SkyJewNick.mode());
            packet.addProperty("message", clean.substring(0, Math.min(clean.length(), 500)));
            ws.sendText(GSON.toJson(packet), true);
        } else {
            connect();
            mcMessage(Component.literal("[Mod] Global chat is reconnecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
        }
    }

    public static void requestDiscord(String action, JsonObject data) {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[Mod] Discord is still connecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        JsonObject packet = data == null ? new JsonObject() : data.deepCopy();
        packet.addProperty("type", "discord");
        packet.addProperty("action", action);
        packet.addProperty("requestId", Long.toString(REQUEST_IDS.incrementAndGet()));
        ws.sendText(GSON.toJson(packet), true);
    }

    public static void sendDiscordDm(String target, String message) {
        String cleanTarget = String.valueOf(target == null ? "" : target).trim();
        String cleanMessage = String.valueOf(message == null ? "" : message).trim();

        if (cleanTarget.isEmpty()) {
            mcMessage(Component.literal("[Mod] Usage: /sj dm <discord-user> <message/link>")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        if (cleanMessage.isEmpty()) {
            mcMessage(Component.literal("[Mod] The Discord DM cannot be empty.")
                .withStyle(Style.EMPTY.withColor(0xFF5555)));
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("type", "dm");
        packet.addProperty("username", username);
        packet.addProperty("target", cleanTarget);
        packet.addProperty("message", cleanMessage.substring(0, Math.min(cleanMessage.length(), 1900)));

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[Mod] Discord link is still connecting. Try again in a moment.")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        ws.sendText(GSON.toJson(packet), true);
    }

    private static void connect() {
        if (!CONNECTING.compareAndSet(false, true)) return;

        HTTP.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(RELAY_URL), new Listener())
            .whenComplete((ws, error) -> {
                CONNECTING.set(false);
                if (error != null) {
                    socket = null;
                    scheduleReconnect();
                    return;
                }

                socket = ws;

                JsonObject hello = new JsonObject();
                hello.addProperty("type", "hello");
                hello.addProperty("username", username);
                ws.sendText(GSON.toJson(hello), true);
            });
    }

    private static void scheduleReconnect() {
        reconnectAt = System.currentTimeMillis() + 5000L;
    }

    public static void tick() {
        if (socket == null && reconnectAt > 0 && System.currentTimeMillis() >= reconnectAt) {
            reconnectAt = 0;
            connect();
        }
    }

    private static void mcMessage(Component message) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(message);
            }
        });
    }

    private static final class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String raw = buffer.toString();
                buffer.setLength(0);
                handle(raw);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (socket == webSocket) socket = null;
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (socket == webSocket) socket = null;
            scheduleReconnect();
        }

        private void handle(String raw) {
            try {
                JsonObject packet = JsonParser.parseString(raw).getAsJsonObject();
                String type = packet.has("type") ? packet.get("type").getAsString() : "";

                if ("discordResult".equals(type)) {
                    String requestId = packet.has("requestId")
                        ? packet.get("requestId").getAsString() : "";
                    boolean ok = packet.has("ok") && packet.get("ok").getAsBoolean();
                    JsonObject result = packet.has("result") && packet.get("result").isJsonObject()
                        ? packet.getAsJsonObject("result") : new JsonObject();
                    String detail = packet.has("message") ? packet.get("message").getAsString() : "";

                    Minecraft.getInstance().execute(() ->
                        SkyJewDiscordScreen.handleResult(requestId, ok, result, detail));
                    return;
                }

                if ("dmResult".equals(type)) {
                    boolean ok = packet.has("ok") && packet.get("ok").getAsBoolean();
                    String target = packet.has("target") ? packet.get("target").getAsString() : "Discord user";
                    String detail = packet.has("message") ? packet.get("message").getAsString() : "";

                    if (ok) {
                        mcMessage(Component.literal("[Mod] Discord DM sent to " + target + ".")
                            .withStyle(Style.EMPTY.withColor(0x55FF55)));
                    } else {
                        mcMessage(Component.literal("[Mod] Discord DM failed: " + detail)
                            .withStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                    return;
                }

                if (!"message".equals(type)) return;

                String name = packet.has("username") ? packet.get("username").getAsString() : "Unknown";\n                String displayName = packet.has("nickname") ? packet.get("nickname").getAsString() : name;
                String message = packet.has("message") ? packet.get("message").getAsString() : "";
                if (message.isBlank()) return;

                name = name.replaceAll("[^A-Za-z0-9_]", "");
                if (name.isBlank()) name = "Unknown";

                String source = packet.has("source") ? packet.get("source").getAsString() : "mod";
                String prefix = "discord".equalsIgnoreCase(source) ? "[Discord]" : "[Mod]";

                String line = prefix + " [" + name + "] " + message;
                mcMessage(Component.literal(line));
            } catch (Exception ignored) {
            }
        }
    }
}
