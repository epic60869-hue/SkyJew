// SkyJew stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.skyblock.tabhud.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import com.epic60869.skyjew.SkyJewTabWidgetManager;

/** Reads the ordered tab list through SkyJew's tab widget manager. */
public final class PlayerListManager {
	private PlayerListManager() {}

	public static @Nullable Matcher regexAt(int idx, Pattern p) {
		String str = strAt(idx);
		if (str == null) return null;
		Matcher m = p.matcher(str);
		return m.matches() ? m : null;
	}

	public static @Nullable String strAt(int idx) {
		List<PlayerInfo> list = SkyJewTabWidgetManager.players();
		if (list.size() <= idx) return null;
		Component txt = list.get(idx).getTabListDisplayName();
		if (txt == null) return null;
		String str = txt.getString().trim();
		return str.isEmpty() ? null : str;
	}
}
