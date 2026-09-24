package com.epic60869.skyjew;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SkyJewChatCompactor {
    private static final long WINDOW_MS = 5000L;
    private static String lastKey;
    private static long lastAt;
    private static int count;

    private SkyJewChatCompactor() {}

    public static boolean enabled() {
        SkyJewConfig c = SkyJewConfig.current();
        return c != null && c.chat != null && c.chat.compactChat;
    }

    public static boolean same(Component a, Component b) {
        return a != null && b != null && strip(a.getString()).equals(strip(b.getString()));
    }

    public static int record(Component message) {
        if (!enabled() || message == null) return 1;
        String key = message.getString();
        long now = System.currentTimeMillis();
        if (key.isBlank()) return 1;
        if (key.equals(lastKey) && now - lastAt <= WINDOW_MS) count++;
        else { lastKey = key; count = 1; }
        lastAt = now;
        return count;
    }

    public static Component withCount(Component message, int value) {
        if (value <= 1) return message;
        MutableComponent out = message.copy();
        out.append(Component.literal(" §7(x" + value + ")"));
        return out;
    }

    public static void clear() { lastKey = null; lastAt = 0; count = 0; }

    private static String strip(String s) {
        return s.replaceFirst(" §7\\(x\\d+\\)$", "");
    }
}
