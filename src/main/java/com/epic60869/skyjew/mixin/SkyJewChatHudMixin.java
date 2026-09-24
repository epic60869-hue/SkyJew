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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
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
    @ModifyArg(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        index = 0
    )
    private Component skyjew$replaceSimpleChat(Component message) {
        return SkyJewNopoFeatures.replaceChatEmojis(message);
    }

    @ModifyArgs(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void skyjew$replaceFullChat(Args args) {
        if (args.size() > 0 && args.get(0) instanceof Component message) {
            args.set(0, SkyJewNopoFeatures.replaceChatEmojis(message));
        }
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skyjew$hideOtherCommandOutput(Component message, MessageSignature signature,
                                                 GuiMessageSource source, GuiMessageTag tag,
                                                 CallbackInfo ci) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.chat.customChat.hideOtherCommands) return;

        String text = message.getString();
        var matcher = java.util.regex.Pattern
            .compile("(?i)\\[SJ\\] \\[[^]]+\\] ([A-Za-z0-9_]{1,16})['’]s ")
            .matcher(text);

        if (matcher.find()) {
            String owner = matcher.group(1);
            String self = net.minecraft.client.Minecraft.getInstance().getUser().getName();
            if (!owner.equalsIgnoreCase(self)) ci.cancel();
        }
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("TAIL")
    )
    private void skyjew$compact(Component message, MessageSignature signature,
                                GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (!SkyJewChatCompactor.enabled() || allMessages.isEmpty()) return;

        GuiMessage newest = allMessages.get(0);
        int count = SkyJewChatCompactor.record(newest.content());
        if (allMessages.size() < 2 || count < 2) return;

        GuiMessage previous = allMessages.get(1);
        if (!SkyJewChatCompactor.same(newest.content(), previous.content())) return;

        Component compacted = SkyJewChatCompactor.withCount(previous.content(), count);
        allMessages.set(0, new GuiMessage(
            newest.addedTime(), compacted, newest.signature(), newest.source(), newest.tag()
        ));
        allMessages.remove(1);
        refreshTrimmedMessages();
    }
}
