// Trimmed from Skyblocker (LGPL-3.0) to the sections SkyBalls's port uses.
package com.epic60869.skyballs.sb.config.configs;

public class HelperConfig {
	public Experiments experiments = new Experiments();

	public static class Experiments {
		public boolean enableChronomatronSolver = true;

		public boolean enableSuperpairsSolver = true;

		public boolean enableUltrasequencerSolver = true;

		public boolean blockIncorrectClicks = false;
	}
}
