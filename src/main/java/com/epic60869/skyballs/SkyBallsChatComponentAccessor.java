package com.epic60869.skyballs;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface SkyBallsChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> skyballs$getAllMessages();

    @Invoker("refreshTrimmedMessages")
    void skyballs$refreshTrimmedMessages();
}
