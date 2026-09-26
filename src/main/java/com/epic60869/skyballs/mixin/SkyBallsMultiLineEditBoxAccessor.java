package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiLineEditBox.class)
public interface SkyBallsMultiLineEditBoxAccessor {
    @Accessor("textField")
    MultilineTextField skyballs$textField();
}
