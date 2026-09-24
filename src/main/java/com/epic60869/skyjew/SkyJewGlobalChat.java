package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

public final class SkyJewGlobalChat {
    private static final String CONFIGURED_RELAY_URL =
        System.getProperty("skyjew.chat.url", "").trim();
    private static final String[] RELAY_URLS = CONFIGURED_RELAY_URL.isBlank()
        ? new String[] {"wss://tastyfish.org/mod-api/tf-chat"}
        : new String[] {CONFIGURED_RELAY_URL};

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);
    private static final AtomicLong REQUEST_IDS = new AtomicLong();
    private static volatile WebSocket socket;
    private static volatile long reconnectAt = 0L;
    private static volatile String username = "Unknown";
    private static volatile int relayIndex = 0;
    private static final Queue<String> PENDING_MESSAGES = new ArrayDeque<>();
    private static volatile boolean inSkyJewChannel = false;
    private static volatile long lastNicknameSync = 0L;
    private static volatile boolean nicknameUpdatePending = false;
    private static final long NICKNAME_SYNC_INTERVAL_MS = 5000L;

    public static boolean isInSkyJewChannel() { return inSkyJewChannel; }

    public static void enterSkyJewChannel() {
        inSkyJewChannel = true;
        mcMessage(Component.literal("You are now in the SkyJew channel")
            .withStyle(Style.EMPTY.withColor(0x55FFFF).withBold(true))
            .append(Component.literal(" — anything you type will be sent to SkyJew chat.")
                .withStyle(Style.EMPTY.withColor(0xAAAAAA))));
    }

    public static void leaveSkyJewChannel() {
        inSkyJewChannel = false;
        mcMessage(Component.literal("You have left the SkyJew channel.")
            .withStyle(Style.EMPTY.withColor(0xFFAA00).withBold(true)));
    }

    private SkyJewGlobalChat() {}

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        username = mc.getUser().getName();
        connect();
    }

    public static void sendBotCommand(String command) {
        String clean = String.valueOf(command == null ? "" : command).trim();
        if (clean.isEmpty() || !clean.startsWith("!")) return;

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SkyJew] Bot command is still connecting...")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("type", "command");
        packet.addProperty("username", username);
        packet.addProperty("command", clean.substring(0, Math.min(clean.length(), 500)));
        ws.sendText(GSON.toJson(packet), true);
        mcMessage(Component.literal("[SJ] Sending " + clean).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
    }

    public static void send(String message) {
        String clean = String.valueOf(message == null ? "" : message).trim();
        if (clean.isEmpty()) return;

        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            synchronized (PENDING_MESSAGES) {
                if (PENDING_MESSAGES.size() >= 20) PENDING_MESSAGES.poll();
                PENDING_MESSAGES.offer(clean.substring(0, Math.min(clean.length(), 500)));
            }
            connect();
            mcMessage(Component.literal("[SJ] Global chat is connecting; your message will be sent when connected.")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        sendNow(ws, clean);
    }

    private static void sendNow(WebSocket ws, String clean) {
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "message");
        packet.addProperty("username", username);
        packet.addProperty("minecraftUuid", Minecraft.getInstance().getUser().getProfileId().toString());
        packet.addProperty("nickname", SkyJewNick.outgoingName());
        packet.addProperty("nicknameEnabled", SkyJewNick.enabled());
        packet.addProperty("nicknameMode", SkyJewNick.mode());
        packet.addProperty("nicknameHex", SkyJewNick.customHex());
        packet.addProperty("message", clean.substring(0, Math.min(clean.length(), 500)));
        ws.sendText(GSON.toJson(packet), true);
    }

    private static void flushPending(WebSocket ws) {
        synchronized (PENDING_MESSAGES) {
            while (!PENDING_MESSAGES.isEmpty()) {
                sendNow(ws, PENDING_MESSAGES.poll());
            }
        }
    }

    public static void sendNicknameUpdate() {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            // Keep the newest saved nickname until the relay is connected again.
            nicknameUpdatePending = true;
            connect();
            return;
        }

        JsonObject packet = new JsonObject();
        packet.addProperty("type", "nickname");
        packet.addProperty("minecraftUuid", Minecraft.getInstance().getUser().getProfileId().toString());
        packet.addProperty("enabled", SkyJewNick.enabled());
        packet.addProperty("name", SkyJewNick.outgoingName());
        packet.addProperty("mode", SkyJewNick.mode());
        packet.addProperty("customHex", SkyJewNick.customHex());
        ws.sendText(GSON.toJson(packet), true);
        nicknameUpdatePending = false;
    }

    public static void requestDiscord(String action, JsonObject data) {
        WebSocket ws = socket;
        if (ws == null || ws.isInputClosed() || ws.isOutputClosed()) {
            connect();
            mcMessage(Component.literal("[SJ] Discord is still connecting...")
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
            mcMessage(Component.literal("[SJ] Usage: /sj dm <discord-user> <message/link>")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        if (cleanMessage.isEmpty()) {
            mcMessage(Component.literal("[SJ] The Discord DM cannot be empty.")
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
            mcMessage(Component.literal("[SJ] Discord link is still connecting. Try again in a moment.")
                .withStyle(Style.EMPTY.withColor(0xFFFF55)));
            return;
        }

        ws.sendText(GSON.toJson(packet), true);
    }

    private static void connect() {
        if (!CONNECTING.compareAndSet(false, true)) return;

        String relayUrl = RELAY_URLS[Math.min(relayIndex, RELAY_URLS.length - 1)];

        HTTP.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(relayUrl), new Listener())
            .whenComplete((ws, error) -> {
                CONNECTING.set(false);
                if (error != null) {
                    socket = null;
                    if (relayIndex + 1 < RELAY_URLS.length) {
                        relayIndex++;
                    }
                    mcMessage(Component.literal("[SJ] Global chat connection failed: "
                        + shortError(error) + " — retrying.")
                        .withStyle(Style.EMPTY.withColor(0xFF5555)));
                    scheduleReconnect();
                    return;
                }

                relayIndex = 0;
                socket = ws;

                JsonObject hello = new JsonObject();
                hello.addProperty("type", "hello");
                hello.addProperty("username", username);
                hello.addProperty("minecraftUuid", Minecraft.getInstance().getUser().getProfileId().toString());
                hello.addProperty("nicknameEnabled", SkyJewNick.enabled());
                hello.addProperty("nickname", SkyJewNick.outgoingName());
                hello.addProperty("nicknameMode", SkyJewNick.mode());
                hello.addProperty("nicknameHex", SkyJewNick.customHex());
                ws.sendText(GSON.toJson(hello), true);
                flushPending(ws);
                // Always publish the current nickname state after a connection
                // is established. This makes the relay authoritative for both
                // newly joined clients and clients reconnecting after a drop.
                nicknameUpdatePending = false;
                lastNicknameSync = System.currentTimeMillis();
                sendNicknameUpdate();
            });
    }

    private static String shortError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private static void scheduleReconnect() {
        reconnectAt = System.currentTimeMillis() + 5000L;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (socket == null && reconnectAt > 0 && now >= reconnectAt) {
            reconnectAt = 0;
            connect();
        }
        if (socket != null && !socket.isInputClosed() && !socket.isOutputClosed()
            && now - lastNicknameSync >= NICKNAME_SYNC_INTERVAL_MS) {
            lastNicknameSync = now;
            sendNicknameUpdate();
        }
    }

    private static Component linkify(Component text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://\\S+");
        MutableComponent result = Component.empty();

        text.visit((style, value) -> {
            if (value == null || value.isEmpty()) return java.util.Optional.empty();

            java.util.regex.Matcher matcher = pattern.matcher(value);
            int last = 0;
            while (matcher.find()) {
                if (matcher.start() > last) {
                    result.append(Component.literal(value.substring(last, matcher.start())).setStyle(style));
                }

                String url = matcher.group();
                while (url.length() > 1 && ")]>.".indexOf(url.charAt(url.length() - 1)) >= 0) {
                    url = url.substring(0, url.length() - 1);
                }

                try {
                    result.append(Component.literal(url).setStyle(style.withUnderlined(true)
                        .withColor(0x55AAFF)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal(url)))));
                } catch (Exception ignored) {
                    result.append(Component.literal(url).setStyle(style));
                }
                last = matcher.end();
            }

            if (last < value.length()) {
                result.append(Component.literal(value.substring(last)).setStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        return result;
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

                if ("nicknameUpdate".equals(type)) {
                    try {
                        UUID uuid = UUID.fromString(packet.get("minecraftUuid").getAsString());
                        boolean enabled = packet.has("enabled") && packet.get("enabled").getAsBoolean();
                        String username = packet.has("username") ? packet.get("username").getAsString() : "";
                        String name = packet.has("name") ? packet.get("name").getAsString() : "";
                        String mode = packet.has("mode") ? packet.get("mode").getAsString() : "Plain";
                        String hex = packet.has("customHex") ? packet.get("customHex").getAsString() : "";
                        SkyJewNick.updateRemote(uuid, username, enabled, name, mode, hex);
                    } catch (Exception ignored) {}
                    return;
                }

                // Nicknames are persistent. A socket disconnect is not a
                // reason to erase another player's nickname from this client.
                // The authoritative nicknameUpdate packet handles enable/disable.

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
                        mcMessage(Component.literal("[SJ] Discord DM sent to " + target + ".")
                            .withStyle(Style.EMPTY.withColor(0x55FF55)));
                    } else {
                        mcMessage(Component.literal("[SJ] Discord DM failed: " + detail)
                            .withStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                    return;
                }

                if (!"message".equals(type)) return;

                String name = packet.has("username") ? packet.get("username").getAsString() : "Unknown";
                String displayName = packet.has("nickname") ? packet.get("nickname").getAsString() : name;
                String message = packet.has("message") ? packet.get("message").getAsString() : "";
                if (message.isBlank()) return;

                UUID messageUuid = null;
                try {
                    if (packet.has("minecraftUuid")) {
                        messageUuid = UUID.fromString(packet.get("minecraftUuid").getAsString());
                        String messageUsername = packet.has("username") ? packet.get("username").getAsString() : "";
                        boolean nickEnabled = packet.has("nicknameEnabled")
                            && packet.get("nicknameEnabled").getAsBoolean();
                        String nickMode = packet.has("nicknameMode")
                            ? packet.get("nicknameMode").getAsString() : "Plain";
                        String nickHex = packet.has("nicknameHex")
                            ? packet.get("nicknameHex").getAsString() : "";

                        // nicknameUpdate is the authoritative state packet.
                        // A chat message from an older/stale connection may not
                        // contain nickname styling, so it must never erase a
                        // nickname that was already synced from the relay.
                        if (nickEnabled) {
                            SkyJewNick.updateRemote(messageUuid, messageUsername, true, displayName, nickMode, nickHex);
                        }
                    }
                } catch (Exception ignored) {}

                name = name.replaceAll("[^A-Za-z0-9_]", "");
                if (name.isBlank()) name = "Unknown";

                String source = packet.has("source") ? packet.get("source").getAsString() : "mod";
                String prefix = "discord".equalsIgnoreCase(source) ? "[Discord]" : "[SJ]";

                Component shownName;
                try {
                    shownName = SkyJewNick.displayName(messageUuid, displayName);
                } catch (Exception ignored) {
                    shownName = SkyJewNick.displayName(displayName);
                }
                Component messageComponent = SkyJewNopoFeatures.replaceChatEmojis(Component.literal(message));
                MutableComponent line = Component.literal(prefix + " [")
                    .append(shownName)
                    .append(Component.literal("]: "))
                    .append(linkify(messageComponent));
                mcMessage(line);
            } catch (Exception ignored) {
            }
        }
    }
}
