// Ported from Skyblocker (https://github.com/SkyblockerMod/Skyblocker, v6.10.4+26.2), licensed under LGPL-3.0.
package com.epic60869.skyjew.custom;

import com.epic60869.skyjew.custom.util.Compat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.slf4j.Logger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;


/**
 * Caches generated ProfileComponents for custom player head textures.
 */
public class CustomHelmetTextures {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final List<NamedTexture> TEXTURES = new ArrayList<>();
	public static final Object2ObjectOpenHashMap<String, ResolvableProfile> PROFILE_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Pattern LEVEL_PATTERN = Pattern.compile("\\[Lvl[^\\]]*\\]");
	public static void init() {
		RepoItems.runAfterItemsLoaded(CustomHelmetTextures::loadTextures);
	}

	private static void loadTextures() {
		try {
			if (!RepoItems.itemsLoaded()) return;

			TEXTURES.clear();
			ObjectSet<String> seen = new ObjectOpenHashSet<>();
			RepoItems.itemsStream()
					.filter(stack -> stack.is(Items.PLAYER_HEAD))
					.forEach(stack -> {
						String texture = Compat.getHeadTexture(stack);
						if (texture.isEmpty() || !seen.add(texture)) return;
						String name = cleanName(stack.get(DataComponents.CUSTOM_NAME).getString());
						TEXTURES.add(new NamedTexture(name, texture, Compat.neuName(stack)));
					});

			TEXTURES.sort(Comparator.comparing(NamedTexture::internalName));
			LOGGER.info("[SkyJew] Loaded and sorted {} helmet textures from repo", TEXTURES.size());
		} catch (Exception e) {
			LOGGER.error("[SkyJew] Failed to load helmet textures from repo", e);
		}
	}

	private static String cleanName(String name) {
		return LEVEL_PATTERN.matcher(name).replaceAll("").trim();
	}

	public static List<NamedTexture> getTextures() {
		return TEXTURES;
	}

	public static ResolvableProfile getProfile(String texture) {
		return PROFILE_CACHE.computeIfAbsent(texture, (String t) ->
				ResolvableProfile.createResolved(new GameProfile(UUID.nameUUIDFromBytes(t.getBytes(StandardCharsets.UTF_8)),
						"custom",
						Compat.propertyMapWithTexture(t))));
	}

	public record NamedTexture(String name, String texture, String internalName) {}
}
