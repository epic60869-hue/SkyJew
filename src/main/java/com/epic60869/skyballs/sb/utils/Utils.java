// SkyBalls stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyballs.sb.utils;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import com.epic60869.skyballs.features.core.SkyBallsLocation;

public final class Utils {
	/** Sidebar lines, top to bottom, without formatting. */
	public static final List<String> STRING_SCOREBOARD = new ScoreboardView();

	private Utils() {}

	public static boolean isOnSkyblock() {
		return SkyBallsLocation.onSkyblock();
	}

	public static boolean isInDungeons() {
		return SkyBallsLocation.inDungeon();
	}

	/** The SkyBlock island you're on, from the tab list's "Area:" line. */
	public static Location getLocation() {
		if (!SkyBallsLocation.onSkyblock()) return Location.UNKNOWN;
		if (SkyBallsLocation.inDungeon()) return Location.DUNGEON;
		String area = SkyBallsLocation.area();
		return switch (area) {
			case "Galatea" -> Location.GALATEA;
			case "Glacite Mineshafts", "Mineshaft" -> Location.GLACITE_MINESHAFTS;
			case "Kuudra" -> Location.KUUDRAS_HOLLOW;
			default -> Location.fromFriendlyName(area);
		};
	}

	/** Non-empty while on SkyBlock (Skyblocker uses Hypixel's raw location id here). */
	public static String getLocationRaw() {
		return SkyBallsLocation.onSkyblock() ? getLocation().id() : "";
	}

	/** SkyBalls does not track the SkyBlock profile name; dungeon data is stored under one profile. */
	public static String getProfile() {
		return "";
	}

	public static java.util.OptionalInt parseInt(String value) {
		try {
			return java.util.OptionalInt.of(Integer.parseInt(value.replace(",", "")));
		} catch (NumberFormatException e) {
			return java.util.OptionalInt.empty();
		}
	}

	public static void sendMessageToBypassEvents(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(message);
	}

	public static void checkForIllegalResourceModification(Identifier id, Resource resource, String message) {}

	private static final class ScoreboardView extends java.util.AbstractList<String> {
		@Override
		public String get(int index) {
			return SkyBallsLocation.scoreboard().get(index);
		}

		@Override
		public int size() {
			return SkyBallsLocation.scoreboard().size();
		}
	}
}
