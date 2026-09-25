package com.epic60869.skyjew.mixin;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface SkyJewCheckboxAccessor {
    @Accessor
    void setSelected(boolean checked);
}
