package com.epic60869.skyballs.mixin;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface SkyBallsCheckboxAccessor {
    @Accessor
    void setSelected(boolean checked);
}
