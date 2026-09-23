package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewChatCompactor;
import com.epic60869.skyjew.SkyJewChatComponentAccessor;
import com.epic60869.skyjew.SkyJewNopoFeatures;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class SkyJewChatHudMixin {
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

        SkyJewChatComponentAccessor accessor = (SkyJewChatComponentAccessor) this;
        List<GuiMessage> all = accessor.skyjew$getAllMessages();

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
            accessor.skyjew$refreshTrimmedMessages();
        }

        ci.cancel();
    }
}
