package com.epic60869.skyballs.custom;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import com.epic60869.skyballs.custom.util.Compat;

/**
 * SkyBalls's replacement for Skyblocker's NEU repo and item repository. Single repo files are
 * fetched on demand, and the SkyBlock item list comes from Hypixel's public items resource.
 */
public final class RepoItems {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String NEU_REPO = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/";
	private static final String HYPIXEL_ITEMS = "https://api.hypixel.net/v2/resources/skyblock/items";
	private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private static final Map<String, RepoItem> ITEMS = new LinkedHashMap<>();
	private static final List<Runnable> AFTER_ITEMS_LOADED = new ArrayList<>();
	private static volatile boolean itemsLoaded;

	private record RepoItem(String id, String name, Item item, @Nullable String texture) {}

	/** A SkyBlock player head, without creating an item stack. */
	public record Head(String id, String name, String texture) {}

	private RepoItems() {}

	public static void init() {
		runAsync(RepoItems::loadItems);
	}

	public static void runAsync(Runnable runnable) {
		CompletableFuture.runAsync(runnable).exceptionally(e -> {
			LOGGER.error("[SkyBalls] Repo task failed", e);
			return null;
		});
	}

	public static String neuRepoFile(String path) throws IOException, InterruptedException {
		return fetch(NEU_REPO + path);
	}

	public static boolean itemsLoaded() {
		return itemsLoaded;
	}

	/** Runs once the item list has loaded and the client has finished starting. */
	public static void runAfterItemsLoaded(Runnable runnable) {
		synchronized (AFTER_ITEMS_LOADED) {
			if (!itemsLoaded()) {
				AFTER_ITEMS_LOADED.add(runnable);
				return;
			}
		}
		runAsync(runnable);
	}

	private static void runPendingCallbacks() {
		List<Runnable> callbacks;
		synchronized (AFTER_ITEMS_LOADED) {
			if (!itemsLoaded()) return;
			callbacks = List.copyOf(AFTER_ITEMS_LOADED);
			AFTER_ITEMS_LOADED.clear();
		}
		callbacks.forEach(RepoItems::runAsync);
	}

	public static List<Head> heads() {
		synchronized (ITEMS) {
			return ITEMS.values().stream()
					.filter(item -> item.item() == Items.PLAYER_HEAD && item.texture() != null)
					.map(item -> new Head(item.id(), item.name(), item.texture()))
					.toList();
		}
	}

	/** Every item's name with its colour codes, for suggestions; empty until the item list has loaded. */
	public static List<String> allNames() {
		synchronized (ITEMS) {
			List<String> names = new ArrayList<>();
			for (RepoItem item : ITEMS.values()) if (item.name() != null && !item.name().isBlank()) names.add(item.name());
			return names;
		}
	}

	/** The item id for a plain item name (no colour codes, any case), e.g. "Enchanted Diamond"; null if unknown. */
	public static @Nullable String idByName(String name) {
		String wanted = net.minecraft.ChatFormatting.stripFormatting(name).trim();
		synchronized (ITEMS) {
			for (RepoItem item : ITEMS.values()) {
				if (item.name() != null && net.minecraft.ChatFormatting.stripFormatting(item.name()).trim().equalsIgnoreCase(wanted)) return item.id();
			}
		}
		return null;
	}

	public static @Nullable String displayName(String id) {
		synchronized (ITEMS) {
			RepoItem item = ITEMS.get(id);
			return item == null ? null : item.name();
		}
	}

	public static ItemStack itemStack(String id) {
		RepoItem item;
		synchronized (ITEMS) {
			item = ITEMS.get(id);
		}
		return item == null ? Compat.barrier() : createStack(item);
	}

	private static ItemStack createStack(RepoItem repoItem) {
		ItemStack stack = repoItem.texture() != null ? Compat.createSkull(repoItem.texture()) : new ItemStack(repoItem.item());
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(repoItem.name()));
		CompoundTag tag = new CompoundTag();
		tag.putString("id", repoItem.id());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	private static void loadItems() {
		try {
			JsonObject root = JsonParser.parseString(fetch(HYPIXEL_ITEMS)).getAsJsonObject();
			Map<String, RepoItem> loaded = new LinkedHashMap<>();
			for (JsonElement element : root.getAsJsonArray("items")) {
				JsonObject item = element.getAsJsonObject();
				if (!item.has("id")) continue;
				String id = item.get("id").getAsString();
				String name = item.has("name") ? item.get("name").getAsString() : id;
				String material = item.has("material") ? item.get("material").getAsString() : "";
				String texture = skinTexture(item.get("skin"));
				loaded.put(id, new RepoItem(id, name, material(material), texture));
			}
			synchronized (ITEMS) {
				ITEMS.clear();
				ITEMS.putAll(loaded);
			}
			itemsLoaded = true;
			LOGGER.info("[SkyBalls] Loaded {} SkyBlock items", loaded.size());
			runPendingCallbacks();
		} catch (Exception e) {
			LOGGER.error("[SkyBalls] Failed to load SkyBlock items", e);
		}
	}

	/** Hypixel sends {@code skin} either as the texture string or as {@code {"value": ..., "signature": ...}}. */
	private static @Nullable String skinTexture(@Nullable JsonElement skin) {
		if (skin == null || skin.isJsonNull()) return null;
		if (skin.isJsonPrimitive()) return skin.getAsString();
		if (skin.isJsonObject() && skin.getAsJsonObject().has("value")) return skin.getAsJsonObject().get("value").getAsString();
		return null;
	}

	private static Item material(String material) {
		if (material.equalsIgnoreCase("SKULL_ITEM")) return Items.PLAYER_HEAD;
		Identifier id = Identifier.tryParse(material.toLowerCase(Locale.ROOT));
		return id == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.BARRIER);
	}

	private static String fetch(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(15))
				.header("User-Agent", "SkyBalls/1.0")
				.GET().build();
		HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for " + url);
		return response.body();
	}
}
