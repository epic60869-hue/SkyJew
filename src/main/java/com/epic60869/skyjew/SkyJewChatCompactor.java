package com.epic60869.skyjew;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * SkyJew's compact-chat implementation, based on the behavior described by
 * the Compacting mod: repeated messages seen again within a short window are
 * represented by one line with an occurrence counter.
 */
public final class SkyJewChatCompactor {
    private static final long COMPACTION_WINDOW_MS = 5_000L;
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> false);

    private SkyJewChatCompactor() {}

    public static boolean enabled() {
        SkyJewConfig config = SkyJewConfig.current();
        return config != null && config.chat != null && config.chat.compactChat;
    }

    /**
     * Called from ChatComponent.addMessage before vanilla adds the message.
     *
     * @return true when the caller should cancel the original add.
     */
    public static boolean handle(ChatComponent chat, Component incoming) {
        if (!enabled() || REENTRANT.get()) return false;

        long now = System.currentTimeMillis();
        String key = incoming.getString();
        if (key == null || key.isBlank() || isSeparator(key)) {
            if (key != null && !key.isBlank()) ENTRIES.remove(key);
            return false;
        }

        prune(now);

        Entry previous = ENTRIES.get(key);
        if (previous == null || now - previous.lastSeen > COMPACTION_WINDOW_MS) {
            ENTRIES.put(key, new Entry(incoming.copy(), 1, now));
            return false;
        }

        int count = previous.count + 1;
        removeMessage(chat, previous.content, previous.addedTime);

        MutableComponent compacted = previous.content.copy();
        compacted.append(Component.literal(" §7(x" + count + ")"));

        previous.count = count;
        previous.lastSeen = now;

        ENTRIES.put(key, new Entry(compacted.copy(), count, now));

        REENTRANT.set(true);
        try {
            chat.addMessage(compacted);
        } finally {
            REENTRANT.set(false);
        }
        return true;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    private static void prune(long now) {
        Iterator<Map.Entry<String, Entry>> iterator = ENTRIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (now - entry.lastSeen > COMPACTION_WINDOW_MS) {
                iterator.remove();
            }
        }
    }

    private static void removeMessage(ChatComponent chat, Component content, int addedTime) {
        ChatComponentAccess access = (ChatComponentAccess) (Object) chat;

        List<GuiMessage> allMessages = access.skyjew$getAllMessages();
        for (int i = allMessages.size() - 1; i >= 0; i--) {
            GuiMessage message = allMessages.get(i);
            if (message.addedTime() == addedTime && message.content().getString().equals(content.getString())) {
                allMessages.remove(i);
                break;
            }
        }

        access.skyjew$getTrimmedMessages().removeIf(line -> line.addedTime() == addedTime);
    }

    private static boolean isSeparator(String text) {
        String plain = text.replaceAll("\s", "");
        if (plain.length() < 3) return false;
        if (plain.matches("[-_=~*•·━─═]+")) return true;
        return plain.matches("[§&]([0-9a-fk-or])+[m_━─═-]{3,}");
    }

    private static final class Entry {
        private final Component content;
        private int count;
        private long lastSeen;
        private int addedTime;

        private Entry(Component content, int count, long lastSeen) {
            this.content = content;
            this.count = count;
            this.lastSeen = lastSeen;
            this.addedTime = Minecraft.getInstance().gui.getGuiTicks();
        }
    }

    public interface ChatComponentAccess {
        List<GuiMessage> skyjew$getAllMessages();
        List<GuiMessage.Line> skyjew$getTrimmedMessages();
    }
}
