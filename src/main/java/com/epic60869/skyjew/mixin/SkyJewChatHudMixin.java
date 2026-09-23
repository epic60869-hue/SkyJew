package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewNopoFeatures;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent")
public abstract class SkyJewChatHudMixin {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component skyjew$replaceEmojis(Component message) {
        return SkyJewNopoFeatures.replaceChatEmojis(message);
    }
}
