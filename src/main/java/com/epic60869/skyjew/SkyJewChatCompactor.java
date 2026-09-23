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

    public static Component compact(Component incoming) {
        if (!enabled() || incoming == null) return incoming;

        String key = incoming.getString();
        if (key == null || key.isBlank() || isSeparator(key)) return incoming;

        long now = System.currentTimeMillis();
        prune(now);

        Entry previous = ENTRIES.get(key);
        if (previous == null || now - previous.lastSeen > WINDOW_MS) {
            ENTRIES.put(key, new Entry(1, now));
            return incoming;
        }

        int count = previous.count + 1;
        previous.count = count;
        previous.lastSeen = now;

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
        String plain = text.replaceAll("\\s", "");
        if (plain.length() < 3) return false;
        return plain.matches("[-_=~*•·━─═]+");
    }

    private static final class Entry {
        private int count;
        private long lastSeen;

        private Entry(int count, long lastSeen) {
            this.count = count;
            this.lastSeen = lastSeen;
        }
    }
}
