// SkyJew stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class SkyblockEvents {
	public static final Event<MayorChange> MAYOR_CHANGE = EventFactory.createArrayBacked(MayorChange.class, callbacks -> () -> {
		for (MayorChange callback : callbacks) callback.onMayorChange();
	});

	private SkyblockEvents() {}

	@FunctionalInterface
	public interface MayorChange {
		void onMayorChange();
	}
}
