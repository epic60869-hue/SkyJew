package com.epic60869.tastyfish;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TastyFishDiscordScreen extends Screen {
    private static TastyFishDiscordScreen active;

    private final Screen previousScreen;
    private EditBox messageBox;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private String view = "home";
    private String selectedGuildId = "";
    private String selectedGuildName = "";
    private String selectedChannelId = "";
    private String selectedChannelName = "";
    private String selectedDmUserId = "";
    private String selectedDmName = "";

    private List<Entry> entries = new ArrayList<>();
    private List<MessageRow> messages = new ArrayList<>();
    private String error = "";
    private boolean loading;

    private TastyFishDiscordScreen(Screen previousScreen) {
        super(Component.literal("TastyFish Discord"));
        this.previousScreen = previousScreen;
        active = this;
    }

    public static void handleResult(String requestId, boolean ok, JsonObject result, String detail) {
        TastyFishDiscordScreen screen = active;
        if (screen == null || screen.minecraft == null) return;
        if (!ok) {
            screen.loading = false;
            screen.error = detail.isBlank() ? "Discord request failed." : detail;
            return;
        }
        screen.loading = false;
        screen.error = "";
        screen.applyResult(result);
    }

    @Override
    protected void init() {
        super.init();
        active = this;
        rebuildLayout();
        messageBox = new EditBox(minecraft, panelX + 255, panelY + panelH - 43, panelW - 345, 28,
            Component.literal("Message"));
        messageBox.setMaxLength(1900);
        messageBox.setHint(Component.literal("Type a Discord message..."));
        addRenderableWidget(messageBox);
        requestHome();
    }

    private void rebuildLayout() {
        panelW = Math.min(1120, width - 32);
        panelH = Math.min(650, height - 32);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    private void requestHome() {
        view = "home";
        loading = true;
        JsonObject data = new JsonObject();
        TastyFishGlobalChat.requestDiscord("home", data);
    }

    private void requestChannels(String guildId, String guildName) {
        selectedGuildId = guildId;
        selectedGuildName = guildName;
        selectedChannelId = "";
        selectedDmUserId = "";
        view = "channels";
        loading = true;
        JsonObject data = new JsonObject();
        data.addProperty("guildId", guildId);
        TastyFishGlobalChat.requestDiscord("channels", data);
    }

    private void requestMessages(String channelId, String channelName) {
        selectedChannelId = channelId;
        selectedChannelName = channelName;
        selectedDmUserId = "";
        view = "messages";
        loading = true;
        JsonObject data = new JsonObject();
        data.addProperty("channelId", channelId);
        TastyFishGlobalChat.requestDiscord("messages", data);
    }

    private void requestDm(String userId, String userName) {
        selectedDmUserId = userId;
        selectedDmName = userName;
        selectedChannelId = "";
        view = "dm";
        loading = true;
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        TastyFishGlobalChat.requestDiscord("openDm", data);
    }

    private void sendMessage() {
        String message = messageBox == null ? "" : messageBox.getValue().trim();
        if (message.isBlank()) return;

        JsonObject data = new JsonObject();
        data.addProperty("message", message);

        if ("dm".equals(view) && !selectedDmUserId.isBlank()) {
            data.addProperty("userId", selectedDmUserId);
            loading = true;
            TastyFishGlobalChat.requestDiscord("sendDm", data);
        } else if ("messages".equals(view) && !selectedChannelId.isBlank()) {
            data.addProperty("channelId", selectedChannelId);
            loading = true;
            TastyFishGlobalChat.requestDiscord("send", data);
        } else {
            return;
        }

        messageBox.setValue("");
    }

    private void applyResult(JsonObject result) {
        String action = result.has("action") ? result.get("action").getAsString() : "";

        if ("home".equals(action)) {
            entries = new ArrayList<>();
            if (result.has("guilds") && result.get("guilds").isJsonArray()) {
                JsonArray guilds = result.getAsJsonArray("guilds");
                for (int i = 0; i < guilds.size(); i++) {
                    JsonObject g = guilds.get(i).getAsJsonObject();
                    entries.add(new Entry(
                        "guild",
                        g.get("id").getAsString(),
                        g.get("name").getAsString()
                    ));
                }
            }
            if (result.has("dms") && result.get("dms").isJsonArray()) {
                JsonArray dms = result.getAsJsonArray("dms");
                for (int i = 0; i < dms.size(); i++) {
                    JsonObject d = dms.get(i).getAsJsonObject();
                    entries.add(new Entry(
                        "dm",
                        d.get("id").getAsString(),
                        d.get("name").getAsString()
                    ));
                }
            }
            messages = new ArrayList<>();
            return;
        }

        if ("channels".equals(action)) {
            entries = new ArrayList<>();
            if (result.has("guild")) {
                selectedGuildName = result.getAsJsonObject("guild").get("name").getAsString();
            }
            if (result.has("channels") && result.get("channels").isJsonArray()) {
                JsonArray channels = result.getAsJsonArray("channels");
                for (int i = 0; i < channels.size(); i++) {
                    JsonObject ch = channels.get(i).getAsJsonObject();
                    entries.add(new Entry(
                        "channel",
                        ch.get("id").getAsString(),
                        ch.get("name").getAsString()
                    ));
                }
            }
            return;
        }

        if ("messages".equals(action) || "openDm".equals(action)) {
            messages = new ArrayList<>();
            if (result.has("messages") && result.get("messages").isJsonArray()) {
                JsonArray rows = result.getAsJsonArray("messages");
                for (int i = 0; i < rows.size(); i++) {
                    JsonObject m = rows.get(i).getAsJsonObject();
                    messages.add(new MessageRow(
                        m.has("author") ? m.get("author").getAsString() : "Unknown",
                        m.has("content") ? m.get("content").getAsString() : "",
                        m.has("timestamp") ? m.get("timestamp").getAsString() : ""
                    ));
                }
            }
            if ("openDm".equals(action) && result.has("user")) {
                JsonObject u = result.getAsJsonObject("user");
                selectedDmUserId = u.get("id").getAsString();
                selectedDmName = u.get("name").getAsString();
            }
            return;
        }

        if ("send".equals(action) || "sendDm".equals(action)) {
            if (result.has("message")) {
                JsonObject m = result.getAsJsonObject("message");
                messages.add(new MessageRow(
                    m.has("author") ? m.get("author").getAsString() : "TastyFish",
                    m.has("content") ? m.get("content").getAsString() : "",
                    m.has("timestamp") ? m.get("timestamp").getAsString() : ""
                ));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        rebuildLayout();

        graphics.fill(0, 0, width, height, 0xCC050509);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF202225);
        graphics.fill(panelX, panelY, panelX + 230, panelY + panelH, 0xFF18191C);
        graphics.fill(panelX + 230, panelY, panelX + panelW, panelY + 48, 0xFF2B2D31);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(font, Component.literal("Discord"), panelX + 18, panelY + 17, 0xFFFFFFFF, true);
        graphics.text(font, Component.literal(
            view.equals("home") ? "Servers & DMs" :
            view.equals("channels") ? selectedGuildName :
            view.equals("dm") ? selectedDmName : "#" + selectedChannelName
        ), panelX + 255, panelY + 17, 0xFFFFFFFF, true);

        drawButton(graphics, panelX + panelW - 86, panelY + 10, 70, 28, "Close",
            mouseX, mouseY);
        if (!view.equals("home")) {
            drawButton(graphics, panelX + 238, panelY + 10, 70, 28, "Back",
                mouseX, mouseY);
        }

        if (view.equals("messages") || view.equals("dm")) {
            drawMessages(graphics);
            drawButton(graphics, panelX + panelW - 82, panelY + panelH - 43, 70, 28, "Send",
                mouseX, mouseY);
        } else {
            drawEntries(graphics, mouseX, mouseY);
        }

        if (loading) {
            graphics.text(font, Component.literal("Loading Discord..."),
                panelX + 250, panelY + 58, 0xFFAAAAAA, false);
        }
        if (!error.isBlank()) {
            graphics.text(font, Component.literal(error),
                panelX + 250, panelY + panelH - 62, 0xFFFF5555, false);
        }
    }

    private void drawEntries(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = panelX + 12;
        int y = panelY + 58;
        int max = Math.min(entries.size(), (panelH - 75) / 30);

        for (int i = 0; i < max; i++) {
            Entry entry = entries.get(i);
            boolean hover = mouseX >= x && mouseX < panelX + 218
                && mouseY >= y + i * 30 && mouseY < y + i * 30 + 26;

            graphics.fill(x, y + i * 30, panelX + 218, y + i * 30 + 26,
                hover ? 0xFF3A3D43 : 0xFF24262A);

            String prefix = "guild".equals(entry.type) ? "◆ " :
                "dm".equals(entry.type) ? "DM  " : "# ";
            graphics.text(font, Component.literal(prefix + entry.name),
                x + 8, y + i * 30 + 8, 0xFFE5E7EB, false);
        }

        if (entries.isEmpty() && !loading) {
            graphics.text(font, Component.literal("No Discord entries available."),
                x, y, 0xFFAAAAAA, false);
        }

        if (view.equals("home")) {
            graphics.text(font, Component.literal("Click a server to browse its channels."),
                panelX + 250, panelY + 75, 0xFFAAAAAA, false);
        }
    }

    private void drawMessages(GuiGraphicsExtractor graphics) {
        int x = panelX + 250;
        int y = panelY + 60;
        int bottom = panelY + panelH - 58;
        int lineY = bottom;

        for (int i = messages.size() - 1; i >= 0 && lineY > y; i--) {
            MessageRow row = messages.get(i);
            String text = row.author + ": " + row.content;
            List<String> wrapped = wrap(text, Math.max(20, (panelW - 275) / 7));
            for (int j = wrapped.size() - 1; j >= 0 && lineY > y; j--) {
                graphics.text(font, Component.literal(wrapped.get(j)),
                    x, lineY, 0xFFE5E7EB, false);
                lineY -= 11;
            }
            lineY -= 5;
        }

        if (messages.isEmpty() && !loading) {
            graphics.text(font, Component.literal("No messages."),
                x, y, 0xFFAAAAAA, false);
        }
    }

    private List<String> wrap(String text, int chars) {
        List<String> result = new ArrayList<>();
        String remaining = text == null ? "" : text;
        while (remaining.length() > chars) {
            int cut = remaining.lastIndexOf(' ', chars);
            if (cut <= 0) cut = chars;
            result.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) result.add(remaining);
        return result;
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                            String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        graphics.fill(x, y, x + w, y + h, hover ? 0xFF5865F2 : 0xFF40444B);
        int tw = font.width(Component.literal(label));
        graphics.text(font, Component.literal(label), x + (w - tw) / 2, y + 9, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        int mx = (int) event.x();
        int my = (int) event.y();

        if (mx >= panelX + panelW - 86 && mx < panelX + panelW - 16
            && my >= panelY + 10 && my < panelY + 38) {
            minecraft.gui.setScreen(previousScreen);
            active = null;
            return true;
        }

        if (!view.equals("home") && mx >= panelX + 238 && mx < panelX + 308
            && my >= panelY + 10 && my < panelY + 38) {
            if (view.equals("messages") || view.equals("dm")) {
                requestChannels(selectedGuildId, selectedGuildName);
            } else {
                requestHome();
            }
            return true;
        }

        if (view.equals("home")) {
            int x = panelX + 12;
            int y = panelY + 58;
            for (int i = 0; i < entries.size() && i < (panelH - 75) / 30; i++) {
                if (mx >= x && mx < panelX + 218 && my >= y + i * 30 && my < y + i * 30 + 26) {
                    Entry entry = entries.get(i);
                    if ("guild".equals(entry.type)) requestChannels(entry.id, entry.name);
                    else requestDm(entry.id, entry.name);
                    return true;
                }
            }
        } else if (view.equals("channels")) {
            int x = panelX + 12;
            int y = panelY + 58;
            for (int i = 0; i < entries.size() && i < (panelH - 75) / 30; i++) {
                if (mx >= x && mx < panelX + 218 && my >= y + i * 30 && my < y + i * 30 + 26) {
                    Entry entry = entries.get(i);
                    requestMessages(entry.id, entry.name);
                    return true;
                }
            }
        } else if ((view.equals("messages") || view.equals("dm"))
            && mx >= panelX + panelW - 82 && mx < panelX + panelW - 12
            && my >= panelY + panelH - 43 && my < panelY + panelH - 15) {
            sendMessage();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private record Entry(String type, String id, String name) {}
    private record MessageRow(String author, String content, String timestamp) {}
}
