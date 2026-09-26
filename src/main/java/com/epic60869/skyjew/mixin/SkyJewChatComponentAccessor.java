package com.epic60869.skyjew.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatComponent.class)
public interface SkyJewChatComponentAccessor {
    @Accessor("trimmedMessages")
    List<GuiMessage.Line> skyjew$trimmedMessages();

    @Accessor("chatScrollbarPos")
    int skyjew$chatScrollbarPos();
}
