package com.epic60869.skyjew;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SkyJewChatCompactor {
    private static final long WINDOW_MS = 5_000L;
    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    private SkyJewChatCompactor() {}

    public static boolean enabled() {
        SkyJewConfig config = SkyJewConfig.current();
        return config != null && config.chat != null && config.chat.compactChat;
    }

    /**
     * Records an incoming message. Returns 1 for a new message, or the
     * occurrence count when it repeats consecutively within the time window.
     */
    public static int nextCount(Component incoming, boolean consecutive) {
        if (!enabled() || incoming == null) return 1;

        String key = incoming.getString();
        if (key == null || key.isBlank() || isSeparator(key)) return 1;

        long now = System.currentTimeMillis();
        prune(now);

        Entry previous = ENTRIES.get(key);
        if (!consecutive || previous == null || now - previous.lastSeen > WINDOW_MS) {
            ENTRIES.put(key, new Entry(incoming.copy(), 1, now));
            return 1;
        }

        previous.count++;
        previous.lastSeen = now;
        return previous.count;
    }

    public static Component withCount(Component incoming, int count) {
        if (count <= 1) return incoming;
        MutableComponent result = incoming.copy();
        result.append(Component.literal(" §7(x" + count + ")"));
        return result;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    private static void prune(long now) {
        Iterator<Map.Entry<String, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastSeen > WINDOW_MS) it.remove();
        }
    }

    private static boolean isSeparator(String text) {
        String plain = text.replaceAll("\s", "");
        if (plain.length() < 3) return false;
        return plain.matches("[-_=~*•·━─═]+");
    }

    private static final class Entry {
        private final Component original;
        private int count;
        private long lastSeen;

        private Entry(Component original, int count, long lastSeen) {
            this.original = original;
            this.count = count;
            this.lastSeen = lastSeen;
        }
    }
}
