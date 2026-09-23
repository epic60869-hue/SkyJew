package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewChatCompactor;
import com.epic60869.skyjew.SkyJewNopoFeatures;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class SkyJewChatHudMixin {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component skyjew$replaceAndCompact(Component message) {
        Component replaced = SkyJewNopoFeatures.replaceChatEmojis(message);
        return SkyJewChatCompactor.compact(replaced);
    }
}
