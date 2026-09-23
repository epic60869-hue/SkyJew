package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewChatCompactor;
import com.epic60869.skyjew.SkyJewNopoFeatures;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class SkyJewChatHudMixin implements SkyJewChatCompactor.ChatComponentAccess {
    @Shadow
    private List<GuiMessage> allMessages;

    @Override
    public List<GuiMessage> skyjew$getAllMessages() {
        return allMessages;
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component skyjew$replaceEmojis(Component message) {
        return SkyJewNopoFeatures.replaceChatEmojis(message);
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skyjew$compactChat(Component message, CallbackInfo ci) {
        if (SkyJewChatCompactor.handle((ChatComponent) (Object) this, message)) {
            ci.cancel();
        }
    }
}
