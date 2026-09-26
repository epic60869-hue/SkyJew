// SkyJew stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.config;

import java.util.function.Consumer;

/**
 * Holds the Skyblocker config sections used by the ported dungeon/experiment code.
 * SkyJew copies its own toggles into it (see SkyJewDungeons); values are not saved here.
 */
public final class SkyblockerConfigManager {
	private static final SkyblockerConfig CONFIG = new SkyblockerConfig();

	private SkyblockerConfigManager() {}

	public static SkyblockerConfig get() {
		return CONFIG;
	}

	public static void update(Consumer<SkyblockerConfig> action) {
		action.accept(CONFIG);
	}

	/** SkyJew: same as {@link #update}; the waypoint options save themselves (see Waypoints). */
	public static void updateOnly(Consumer<SkyblockerConfig> action) {
		action.accept(CONFIG);
		com.epic60869.skyjew.sb.skyblock.waypoint.Waypoints.saveOptions();
	}
}
