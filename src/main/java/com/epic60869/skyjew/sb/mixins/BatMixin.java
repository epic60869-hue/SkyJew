// Ported from Skyblocker's mixins (LGPL-3.0) for SkyJew's dungeon port.
package com.epic60869.skyjew.sb.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;

import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonManager;

@Mixin(Bat.class)
public abstract class BatMixin extends AmbientCreature {
	protected BatMixin(EntityType<? extends AmbientCreature> entityType, Level world) {
		super(entityType, world);
	}

	@Override
	public void onClientRemoval() {
		super.onClientRemoval();
		if (this.getHealth() <= 0) {
			DungeonManager.onBatRemoved(this);
		}
	}
}
