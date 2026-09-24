package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewChatCompactor;
import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewNopoFeatures;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ChatComponent.class)
public abstract class SkyJewChatHudMixin {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow private void refreshTrimmedMessages() {}

    /*
     * Hypixel/vanilla can reach ChatComponent through either addMessage
     * overload. The previous mixin only covered the 4-argument path, which is
     * why emojis appeared in SkyJew relay chat but not normal server chat.
     */
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component skyjew$replaceSimpleChat(Component message) {
        return SkyJewNopoFeatures.replaceChatEmojis(message);
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component skyjew$replaceFullChat(Component message) {
        return SkyJewNopoFeatures.replaceChatEmojis(message);
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("TAIL")
    )
    private void skyjew$compact(Component message, MessageSignature signature,
                                GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (!SkyJewChatCompactor.enabled() || allMessages.isEmpty()) return;

        // The new message is already present at index 0. Find the most recent
        // matching message anywhere in the visible history instead of requiring
        // it to be directly adjacent.
        if (SkyJewChatCompactor.compact(allMessages)) {
            refreshTrimmedMessages();
        }
    }
