package com.epic60869.skyjew.mixin;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiLineEditBox.class)
public interface SkyJewMultiLineEditBoxAccessor {
    @Accessor("textField")
    MultilineTextField skyjew$textField();
}
