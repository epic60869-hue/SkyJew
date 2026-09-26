package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewGlobalChat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if (key != InputConstants.KEY_RETURN && key != InputConstants.KEY_NUMPADENTER) {
            return;
        }

        String message = input.getValue().trim();
        // Let Minecraft handle slash commands normally. This is what makes
        // /sj chat leave (and every other /command) usable while channel mode is active.
        if (message.startsWith("/")) {
            return;
        }
        if (!message.isEmpty()) {
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

    @Shadow
    private net.minecraft.client.gui.components.ChatComponent.DisplayMode displayMode;

    /** Image preview when hovering an image link in chat. */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void skyjew$imagePreview(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        var finder = new net.minecraft.client.gui.ActiveTextCollector.ClickableStyleFinder(mc.font, mouseX, mouseY);
        mc.gui.hud.getChat().captureClickableText(finder, mc.getWindow().getGuiScaledHeight(), mc.gui.hud.getGuiTicks(), displayMode);
        com.epic60869.skyjew.SkyJewImagePreview.render(graphics, finder.result(), mouseX, mouseY);
    }
}
