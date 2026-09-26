package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.SkyBallsStaff;
import com.epic60869.skyballs.mixin.SkyBallsChatComponentAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy Chat, from NoFrills' Chat Tweaks: with chat open, right-click a message (or press the copy key over it) to
 * copy the whole message, Shift+right-click (or the copy line key) to copy just that line. A short preview of what
 * was copied shows in chat. SkyBalls rank prefixes like "[OWNER] " are left out of what's copied.
 */
public final class CopyChat {
    private CopyChat() {}

    private static SkyBallsConfig.CopyChat config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.chat.copyChat;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ChatScreen)) return;
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                SkyBallsConfig.CopyChat c = config();
                if (c == null || !c.enabled || !c.rightClick || event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return true;
                boolean line = Minecraft.getInstance().hasShiftDown();
                return !copy(event.x(), event.y(), line);
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
                SkyBallsConfig.CopyChat c = config();
                if (c == null || !c.enabled) return true;
                boolean message = c.copyKey != GLFW.GLFW_KEY_UNKNOWN && event.key() == c.copyKey;
                boolean line = c.copyLineKey != GLFW.GLFW_KEY_UNKNOWN && event.key() == c.copyLineKey;
                if (!message && !line) return true;
                Minecraft mc = Minecraft.getInstance();
                double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
                double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
                copy(x, y, line);
                return false;
            });
        });
    }

    /** Copies the chat message (or line) under the mouse; false if there's none there. */
    private static boolean copy(double mouseX, double mouseY, boolean singleLine) {
        String text = hovered(mouseX, mouseY, singleLine);
        if (text.isEmpty()) return false;
        text = stripRanks(text);
        SkyBallsConfig.CopyChat c = config();
        if (c != null && c.trim) text = text.trim();
        Minecraft mc = Minecraft.getInstance();
        mc.keyboardHandler.setClipboard(text);
        if (c != null && c.preview) {
            String type = singleLine ? "Line" : "Message";
            int length = c.previewLength;
            String shown = length > 0 && text.length() > length ? text.substring(0, length) + "..." : text;
            Component message = Component.literal("[SB] ").withStyle(ChatFormatting.DARK_GREEN)
                .append(Component.literal(type + " copied" + (length == 0 ? "." : ": ")).withStyle(ChatFormatting.GREEN));
            if (length != 0) {
                message = message.copy().append(Component.literal("\"").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(shown).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\"").withStyle(ChatFormatting.GREEN));
            }
            mc.gui.hud.getChat().addClientSystemMessage(message);
        }
        return true;
    }

    /** SkyBalls rank prefixes ("[OWNER] ") aren't part of what the player wrote. */
    private static String stripRanks(String text) {
        String out = text;
        for (SkyBallsStaff.Rank rank : SkyBallsStaff.allRanks()) out = out.replace("[" + rank.label() + "] ", "");
        return out;
    }

    /** The plain text of the chat message (or line) under the mouse, like NoFrills' getHoveredMsg. */
    private static String hovered(double mouseX, double mouseY, boolean singleLine) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.hud.getChat();
        SkyBallsChatComponentAccessor access = (SkyBallsChatComponentAccessor) chat;
        List<GuiMessage.Line> all = access.skyballs$trimmedMessages();
        double chatScale = mc.options.chatScale().get();
        int chatBottom = Mth.floor(mc.getWindow().getGuiScaledHeight() - 40);
        int entryHeight = (int) (9.0 * (mc.options.chatLineSpacing().get() + 1.0));
        int chatHeight = ChatComponent.getHeight(mc.options.chatHeightFocused().get());
        int start = access.skyballs$chatScrollbarPos();
        int end = Math.min(all.size(), start + chatHeight / Math.max(1, entryHeight));
        if (start >= end) return "";
        List<GuiMessage.Line> visible = all.subList(start, end);
        int width = (int) (ChatComponent.getWidth(mc.options.chatWidth().get()) * chatScale) + 8;
        for (int index = 0; index < visible.size(); index++) {
            int entryBottom = (int) (chatBottom - index * (entryHeight * chatScale));
            int entryTop = (int) (entryBottom - (entryHeight * chatScale));
            if (mouseX >= 0 && mouseX <= width && mouseY >= entryTop && mouseY < entryBottom) {
                if (singleLine) return plain(visible.get(index).content());
                StringBuilder out = new StringBuilder();
                for (GuiMessage.Line line : fullMessage(visible, index)) out.append(plain(line.content()));
                return out.toString();
            }
        }
        return "";
    }

    private static List<GuiMessage.Line> fullMessage(List<GuiMessage.Line> visible, int index) {
        List<GuiMessage.Line> lines = new ArrayList<>();
        for (int i = index + 1; i < visible.size(); i++) {
            GuiMessage.Line line = visible.get(i);
            if (line.endOfEntry()) break;
            lines.addFirst(line);
        }
        for (int i = index; i >= 0; i--) {
            GuiMessage.Line line = visible.get(i);
            lines.add(line);
            if (line.endOfEntry()) break;
        }
        return lines;
    }

    private static String plain(FormattedCharSequence sequence) {
        StringBuilder out = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }
}
