package com.epic60869.skyjew;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compact-chat implementation modelled after the behaviour of the Compacting
 * Fabric mod: a message is compacted according to how recently the same
 * message was last seen, rather than only when two copies are adjacent.
 *
 * The original implementation only compared allMessages[0] and [1], which
 * breaks as soon as another chat line is inserted between two repetitions.
 */
public final class SkyJewChatCompactor {
    private static final long WINDOW_MS = 5000L;
    private static final Pattern COUNT_SUFFIX =
        Pattern.compile("\\s*\\u00a7?7?\\s*\\(x(\\d+)\\)$");

    private static String lastKey;
    private static long lastAt;

    private SkyJewChatCompactor() {}

    public static boolean enabled() {
        SkyJewConfig c = SkyJewConfig.current();
        return c != null && c.chat != null && c.chat.compactChat;
    }

    /** Returns a stable comparison key with our own xN suffix removed. */
    public static String key(Component message) {
        if (message == null) return "";
        String text = message.getString();
        Matcher m = COUNT_SUFFIX.matcher(text);
        return m.find() ? text.substring(0, m.start()).stripTrailing() : text;
    }

    public static boolean same(Component a, Component b) {
        String aa = key(a);
        String bb = key(b);
        return !aa.isBlank() && aa.equals(bb);
    }

    /**
     * Records the newest message and returns whether it is inside the
     * "recently seen" window. The visible list is handled by compact().
     */
    public static boolean recentlySeen(Component message) {
        if (!enabled() || message == null) return false;
        String k = key(message);
        if (k.isBlank()) return false;

        long now = System.currentTimeMillis();
        boolean recent = k.equals(lastKey) && now - lastAt <= WINDOW_MS;
        lastKey = k;
        lastAt = now;
        return recent;
    }

    /**
     * Compact the newest chat message against the most recently visible copy
     * of the same message. This deliberately does not require adjacency.
     *
     * Returns true when the new message was absorbed into an older one.
     */
    public static boolean compact(java.util.List<GuiMessage> messages) {
        if (!enabled() || messages == null || messages.size() < 2) return false;

        GuiMessage newest = messages.get(0);
        String key = key(newest.content());
        if (key.isBlank()) return false;

        long now = newest.addedTime();
        for (int i = 1; i < messages.size(); i++) {
            GuiMessage old = messages.get(i);
            if (!same(newest.content(), old.content())) continue;

            long age = Math.max(0L, now - old.addedTime());
            if (age > WINDOW_MS) continue;

            int oldCount = count(old.content());
            int newCount = oldCount + 1;

            Component compacted = withCount(old.content(), newCount);
            messages.set(i, new GuiMessage(
                old.addedTime(), compacted, old.signature(), old.source(), old.tag()
            ));
            messages.remove(0);
            return true;
        }

        return false;
    }

    public static int record(Component message) {
        recentlySeen(message);
        return count(message);
    }

    public static Component withCount(Component message, int value) {
        if (message == null || value <= 1) return message;

        MutableComponent out = stripCount(message).copy();
        out.append(Component.literal(" §7(x" + value + ")"));
        return out;
    }

    public static int count(Component message) {
        if (message == null) return 1;
        Matcher m = COUNT_SUFFIX.matcher(message.getString());
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    private static Component stripCount(Component message) {
        if (message == null) return Component.empty();
        String text = message.getString();
        Matcher m = COUNT_SUFFIX.matcher(text);
        if (!m.find()) return message;

        // Preserve the original component styling by rebuilding only the
        // plain text. Hypixel chat styling is retained by the surrounding
        // ChatComponent before the compacted suffix is appended.
        return Component.literal(text.substring(0, m.start()));
    }

    public static void clear() {
        lastKey = null;
        lastAt = 0L;
    }
}
