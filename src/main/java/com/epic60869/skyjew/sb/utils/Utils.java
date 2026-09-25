// SkyJew stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.utils;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import com.epic60869.skyjew.features.core.SkyJewLocation;

public final class Utils {
	/** Sidebar lines, top to bottom, without formatting. */
	public static final List<String> STRING_SCOREBOARD = new ScoreboardView();

	private Utils() {}

	public static boolean isOnSkyblock() {
		return SkyJewLocation.onSkyblock();
	}

	public static boolean isInDungeons() {
		return SkyJewLocation.inDungeon();
	}

	/** SkyJew does not track the SkyBlock profile name; dungeon data is stored under one profile. */
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
			return SkyJewLocation.scoreboard().get(index);
		}

		@Override
		public int size() {
			return SkyJewLocation.scoreboard().size();
		}
	}
}
