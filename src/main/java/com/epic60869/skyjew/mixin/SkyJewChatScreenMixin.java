package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewGlobalChat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class SkyJewChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void skyjew$sendChannelMessage(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!SkyJewGlobalChat.isInSkyJewChannel()) {
            return;
        }

        int key = event.key();
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) {
            return;
        }

        String message = input.getValue().trim();
        if (!message.isEmpty() && !message.startsWith("/")) {
            if (message.startsWith("!")) {
                SkyJewGlobalChat.sendBotCommand(message);
            } else {
                SkyJewGlobalChat.send(message);
            }
        }

        input.setValue("");
        ((ChatScreen) (Object) this).onClose();
        cir.setReturnValue(true);
    }
}
