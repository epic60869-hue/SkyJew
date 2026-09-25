package com.epic60869.skyjew.features.core;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Single entry point for chat and action bar messages, shared by all SkyJew features. */
public final class SkyJewChat {
    /** A received message with its formatting-stripped text. */
    public record Message(Component component, String text) {}

    public interface Listener {
        void onMessage(Message message);
    }

    private static final List<Listener> CHAT = new CopyOnWriteArrayList<>();
    private static final List<Listener> ACTION_BAR = new CopyOnWriteArrayList<>();

    private SkyJewChat() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> dispatch(overlay ? ACTION_BAR : CHAT, message));
    }

    public static void onChat(Listener listener) { CHAT.add(listener); }
    public static void onActionBar(Listener listener) { ACTION_BAR.add(listener); }

    private static void dispatch(List<Listener> listeners, Component component) {
        if (listeners.isEmpty()) return;
        Message message = new Message(component, SkyJewLocation.strip(component.getString()));
        for (Listener listener : listeners) {
            try {
                listener.onMessage(message);
            } catch (Exception e) {
                System.err.println("[SkyJew] Chat listener failed: " + e);
            }
        }
    }
}
