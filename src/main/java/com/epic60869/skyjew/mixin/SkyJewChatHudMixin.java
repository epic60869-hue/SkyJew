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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class SkyJewChatHudMixin {
    @Shadow @Final private List<GuiMessage> allMessages;

    @Shadow
    private void refreshTrimmedMessages() {}
    @ModifyArgs(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At(value = "HEAD")
    )
    private void skyjew$replaceEmojiArgs(Args args) {
        if (args.size() > 0 && args.get(0) instanceof Component message) {
            args.set(0, SkyJewNopoFeatures.replaceChatEmojis(message));
        }
    }


    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skyjew$hideOtherCommandOutput(
        Component message,
        MessageSignature signature,
        GuiMessageSource source,
        GuiMessageTag tag,
        CallbackInfo ci
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.chat.customChat.hideOtherCommands) return;

        String text = message.getString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(?i)\\[SJ\\] \\[[^]]+\\] ([A-Za-z0-9_]{1,16})['’]s ")
            .matcher(text);
        if (!matcher.find()) return;

        String owner = matcher.group(1);
        String self = net.minecraft.client.Minecraft.getInstance().getUser().getName();
        if (!owner.equalsIgnoreCase(self)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skyjew$compact(
        Component message,
        MessageSignature signature,
        GuiMessageSource source,
        GuiMessageTag tag,
        CallbackInfo ci
    ) {
        Component replaced = SkyJewNopoFeatures.replaceChatEmojis(message);
        if (!SkyJewChatCompactor.enabled()) {
            return;
        }

        List<GuiMessage> all = allMessages;

        boolean consecutive = !all.isEmpty()
            && all.get(0).content().getString().replaceAll(" §7\\(x\\d+\\)$", "").equals(replaced.getString());

        int count = SkyJewChatCompactor.nextCount(replaced, consecutive);
        if (count <= 1) {
            return;
        }

        if (!all.isEmpty()) {
            GuiMessage previous = all.get(0);
            Component compacted = SkyJewChatCompactor.withCount(replaced, count);
            all.set(0, new GuiMessage(
                previous.addedTime(),
                compacted,
                previous.signature(),
                previous.source(),
                previous.tag()
            ));
            refreshTrimmedMessages();
        }

        ci.cancel();
    }
}
