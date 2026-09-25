// SkyJew stand-in for a Skyblocker class used by the ported dungeon/experiment code (Skyblocker is LGPL-3.0).
package com.epic60869.skyjew.sb.utils.render.title;

import com.epic60869.skyjew.features.core.SkyJewAlerts;
import net.minecraft.network.chat.Component;

public final class TitleContainer {
	private TitleContainer() {}

	public static void addTitleAndPlaySound(Title title, int ticks) {
		SkyJewAlerts.title(title.getText(), Component.empty());
	}
}
