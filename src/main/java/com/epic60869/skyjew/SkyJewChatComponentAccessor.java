package com.epic60869.skyjew;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface SkyJewChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> skyjew$getAllMessages();

    @Invoker("refreshTrimmedMessages")
    void skyjew$refreshTrimmedMessages();
}
