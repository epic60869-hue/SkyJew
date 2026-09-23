package com.epic60869.tastyfish.mixin;

import com.epic60869.tastyfish.TastyFishNopoFeatures;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent")
public abstract class TastyFishChatHudMixin {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component tastyfish$replaceEmojis(Component message) {
        return TastyFishNopoFeatures.replaceChatEmojis(message);
    }
}
