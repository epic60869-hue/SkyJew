package com.epic60869.skyjew;

import com.epic60869.skyjew.mixin.SkyJewPlayerTabOverlayAccessor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkyJewNopoFeatures {
    private static final Gson GSON = new Gson();
    private static final String CHAT_FILE = "skyjew-nopo-chat-emojis.json";
    private static final String SLAYER_FILE = "skyjew-slayer-drops.json";
    private static final String CROP_FILE = "skyjew-rare-crops.json";
    private static final Identifier PET_HUD_ID = Identifier.fromNamespaceAndPath("skyjew", "pet_display");

    private static Path configDir;
    private static boolean initialized;

    private static final Set<String> EMOJIS = new HashSet<>();
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([A-Za-z0-9_+\\-]+):");
    private static final Map<String, String> EMOJI_SYMBOLS = new LinkedHashMap<>();
    private static final Map<String, String> EMOJI_CANONICAL = new HashMap<>();
    private static final Map<String, SlayerData> SLAYERS = new LinkedHashMap<>();
    private static final Map<String, List<Long>> CROP_TIMES = new LinkedHashMap<>();
    private static String currentSlayer = null;

    private static final Pattern SLAYER_START = Pattern.compile(" +(?<slayer>Wolf|Zombie|Blaze|Vampire|Spider|Enderman|Guardian) Slayer LVL \\d.*");
    private static final Pattern SLAYER_DROP = Pattern.compile("(?:VERY RARE|RARE|INSANE|CRAZY RARE) DROP! \\((?<amount>\\d+x )?(?<item>[^)]+)\\)(?: .+)?");

    private static final Pattern RARE_CROP = Pattern.compile("(?:VERY )?RARE CROP! (?<crop>[a-zA-Z ]+) \\(\\+[0-9,.]+.*?\\)(?: \\(automatically donated\\))?");
    private static final Pattern PET_DROP = Pattern.compile("PET DROP! (?<pet>\\w+) \\(\\+[0-9,.]+.*?\\)");
    private static final Pattern MOSQUITO = Pattern.compile("MOSQUITO! You found an? (?<crop>.*)!");
    private static final Pattern RAT = Pattern.compile("RAT! You dropped an additional (?<crop>.*)!");
    private static final String CROP_FEVER = "WOAH! You caught a case of the CROP FEVER for 60 seconds!";

    private static List<Component> petDisplay = null;
    private static int petTick;
    private static String currentPet = "";
    private static int currentOverflowLevel = -1;

    private static final int[] PET_XP = {
        100,110,120,130,145,160,175,190,210,230,250,275,300,330,360,400,440,490,540,600,
        660,730,800,880,960,1050,1150,1260,1380,1510,1650,1800,1960,2130,2310,2500,2700,
        2920,3160,3420,3700,4000,4350,4750,5200,5700,6300,7000,7800,8700,9700,10800,
        12000,13300,14700,16200,17800,19500,21300,23200,25200,27400,29800,32400,35200,
        38200,41400,44800,48400,52200,56200,60400,64800,69400,74200,79200,84700,90700,
        97200,104200,111700,119700,128200,137200,146700,156700,167700,179700,192700,
        206700,221700,237700,254700,272700,291700,311700,333700,357700,383700,411700,
        441700,476700,516700,561700,611700,666700,726700,791700,861700,936700,1016700,
        1101700,1191700,1286700,1386700,1496700,1616700,1746700,1886700
    };

    private SkyJewNopoFeatures() {}

    public static void init(Path dir) {
        configDir = dir;
        loadJson();
        loadDefaultEmojiSymbols();
        loadEmojis();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { handleSlayer(message); handleRareCrop(message); });
        registerOverflowPets();
        registerPetHud();
        initialized = true;
    }

    public static void tick(Minecraft mc) {
        if (!initialized) return;
        petTick++;
        if (petTick >= 10) {
            petTick = 0;
            updatePetDisplay(mc);
        }
    }

    private static void registerOverflowPets() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (stack.getItem() != Items.PLAYER_HEAD) return;
            if (!isHypixel()) return;

            try {
                var custom = stack.get(DataComponents.CUSTOM_DATA);
                if (custom == null) return;
                String petInfo = custom.copyTag().getStringOr("petInfo", "");
                if (petInfo.isBlank()) return;

                JsonObject json = JsonParser.parseString(petInfo).getAsJsonObject();
                if (!json.has("exp")) return;

                float xp = json.get("exp").getAsFloat();
                String tier = json.has("tier") ? json.get("tier").getAsString() : "LEGENDARY";
                int rarityOffset = rarityOffset(tier);
                int overflowLevel = calcLevel(xp, rarityOffset);

                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).getString().contains("MAX LEVEL")) continue;

                    MutableComponent line = Component.literal("MAX LEVEL")
                        .withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true));
                    line.append(Component.literal(" ["));
                    line.append(Component.literal(overflowLevel + "✦")
                        .withStyle(s -> s.withColor(ChatFormatting.GOLD)));
                    line.append(Component.literal("]").withStyle(s -> s.withColor(ChatFormatting.GRAY)));
                    lines.set(i, line);
                    break;
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private static void registerPetHud() {
        HudElementRegistry.addLast(PET_HUD_ID, (context, deltaTracker) -> renderPetHud(context));
    }

    private static void renderPetHud(GuiGraphicsExtractor context) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.pets.display.enabled) return;
        if (!isHypixel() || petDisplay == null || petDisplay.isEmpty()) return;
        renderPetHudAt(context, config.pets.display.x, config.pets.display.y, false);
    }

    public static void renderPetHudPreview(GuiGraphicsExtractor context, int x, int y) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null) return;
        if (petDisplay != null && !petDisplay.isEmpty()) {
            renderPetHudAt(context, x, y, true);
            return;
        }

        var font = Minecraft.getInstance().font;
        context.fill(x - 6, y - 6, x + 230, y + 34, 0x99000000);
        context.text(font, Component.literal("Pet:"), x, y, 0xFFFFD83D, true);
        context.text(font, Component.literal("[Lvl 200] Golden Dragon"), x, y + 11, 0xFFFFAA00, false);
        context.text(font, Component.literal("2,345,678/2,500,000 XP (93.8%)"), x, y + 22, 0xFFFFD83D, false);
    }

    private static void renderPetHudAt(GuiGraphicsExtractor context, int x, int y, boolean preview) {
        var font = Minecraft.getInstance().font;
        if (preview) context.fill(x - 6, y - 6, x + 250, y + petDisplay.size() * 11 + 6, 0x99000000);
        for (int i = 0; i < petDisplay.size(); i++) {
            context.text(font, petDisplay.get(i), x, y + i * 11, -1);
        }
    }

    private static void updatePetDisplay(Minecraft mc) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.pets.display.enabled || !config.pets.display.autoDisplay) {
            petDisplay = null;
            return;
        }
        if (!isHypixel() || mc.getConnection() == null) {
            petDisplay = null;
            return;
        }

        // Minecraft 26.2 exposes the live TAB entries through the connection.
        // Keep the raw component rows so Nopo's indented Pet widget can be parsed.
        // Port of Nopo's HypixelUtils: use the same vanilla TAB comparator and
        // the live online player list, then feed the resulting components to the
        // Pet: widget parser.
        List<PlayerInfo> orderedPlayers = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        try {
            orderedPlayers.sort(SkyJewPlayerTabOverlayAccessor.getOrdering());
        } catch (Throwable ignored) {
        }
        List<Component> tab = orderedPlayers.stream()
            .map(PlayerInfo::getTabListDisplayName)
            .filter(Objects::nonNull)
            .toList();

        List<Component> petLines = new ArrayList<>();
        boolean inPetWidget = false;
        for (Component line : tab) {
            String text = line.getString();
            if (!inPetWidget) {
                if (text.trim().equalsIgnoreCase("Pet:") || text.trim().startsWith("Pet:")) {
                    inPetWidget = true;
                    petLines.add(line);
                }
                continue;
            }

            // TabWidget.PET keeps every following indented line until the next
            // non-indented row. This is the important part of Nopo's parser.
            if (!text.isEmpty() && text.charAt(0) == ' ') {
                petLines.add(line);
            } else {
                break;
            }
        }

        if (petLines.size() < 2) {
            petDisplay = null;
            return;
        }

        // Nopo's TabWidget always keeps the "Pet:" header as the first line.
        // Start with it so the rebuilt HUD has the same three-line structure.
        List<Component> display = new ArrayList<>();
        display.add(petLines.get(0));
        int level = -1;
        int overflowLevel = -1;
        String name = "";
        int rarityOffset = 20;
        boolean maxLevel = false;

        Pattern petNameRegex = Pattern.compile("^ +\\[Lvl (?<level>\\d+)] (?<name>.*)$");
        Pattern overflowXpRegex = Pattern.compile("^ +\\+(?<xp>[\\d,.]+) XP$");

        for (Component line : petLines) {
            String text = line.getString();

            Matcher petMatch = petNameRegex.matcher(text);
            if (petMatch.matches()) {
                level = Integer.parseInt(petMatch.group("level"));
                overflowLevel = level;
                name = petMatch.group("name");
                String rarityName = name.replace("✦", "").trim();
                rarityOffset = rarityOffsetFromComponent(line, rarityName);

                // Preserve the original Pet: header and any other widget rows,
                // then replace the normal [Lvl ...] row below.
                continue;
            }

            Matcher xpMatch = overflowXpRegex.matcher(text);
            if (xpMatch.matches() && level >= 0) {
                maxLevel = true;
                float currentXp = (float) parseDouble(xpMatch.group("xp"));

                // Match NopoMod's behaviour: add the XP already earned through
                // the real pet level using that pet's rarity, then calculate the
                // overflow level on the legendary curve.
                float totalXp = currentXp + calculativeXpForLevel(level, rarityOffset);
                overflowLevel = calcLevel(totalXp);
                if (level == 200) overflowLevel--;

                float progressXp = leftoverXp(totalXp);
                int nextOffset = (rarityOffset < 20 && overflowLevel < 100) ? 1 : 0;
                int xpForNextLevel = getXpForLevel(
                    Math.max(0, overflowLevel - nextOffset), 20
                );
                double percent = xpForNextLevel <= 0
                    ? 0
                    : (progressXp / xpForNextLevel) * 100.0;

                display.add(Component.literal(
                    " " + formatNumber(progressXp) + "/" + formatCompact(xpForNextLevel)
                        + " XP (" + String.format(Locale.US, "%.1f", percent) + "%)"
                ).withStyle(style -> style.withColor(ChatFormatting.YELLOW)));
                continue;
            }

            // The normal pet-name row is rebuilt below after we have calculated
            // its overflow level. Other indented rows are intentionally ignored,
            // matching NopoMod's PetDisplay behaviour.
        }

        if (level < 0 || name.isBlank()) {
            petDisplay = null;
            return;
        }

        Component nameComponent = findComponentText(petLines.get(1), name);
        if (nameComponent == null) {
            nameComponent = Component.literal(name);
        }

        MutableComponent levelLine = Component.literal(" [Lvl " + overflowLevel);
        if (rarityOffset < 20 && level != overflowLevel && overflowLevel < 100) {
            levelLine.append(Component.literal(" (" + level + ")"));
        }
        levelLine.append(Component.literal("] "));
        levelLine.append(nameComponent);

        display.add(1, levelLine);

        if (currentPet.equals(name) && currentOverflowLevel + 1 == overflowLevel && maxLevel
                && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("Your ")
                .append(nameComponent)
                .append(Component.literal(" leveled up to level "))
                .append(Component.literal(Integer.toString(overflowLevel))
                    .withStyle(style -> style.withColor(ChatFormatting.BLUE)))
                .append("!"));
        }

        petDisplay = display;
        currentPet = name;
        currentOverflowLevel = overflowLevel;
    }

    private static Component findComponentText(Component component, String text) {
        final Component[] found = {null};
        component.visit((style, value) -> {
            if (value.equals(text)) {
                found[0] = Component.literal(value).withStyle(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static int rarityOffsetFromComponent(Component component, String name) {
        final int[] result = {20};
        component.visit((style, value) -> {
            if (!value.equals(name)) return Optional.empty();
            String color = styleColorName(style);
            result[0] = switch (color) {
                case "white" -> 0;
                case "green" -> 6;
                case "blue" -> 11;
                case "dark_purple" -> 15;
                default -> 20;
            };
            return Optional.empty();
        }, Style.EMPTY);
        return result[0];
    }

    private static String styleColorName(Style style) {
        try {
            // 26.x TextColor exposes its vanilla colour name directly. Keep
            // reflection as a fallback so this remains tolerant of mapping
            // changes between 26.1 and 26.2.
            if (style.getColor() == null) return "";
            int rgb = style.getColor().getValue();
            return switch (rgb) {
                case 0xFFFFFF -> "white";
                case 0x55FF55 -> "green";
                case 0x5555FF -> "blue";
                case 0xAA00AA -> "dark_purple";
                default -> "";
            };
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int rarityOffset(String tier) {
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> 0;
            case "UNCOMMON" -> 6;
            case "RARE" -> 11;
            case "EPIC" -> 15;
            default -> 20;
        };
    }

    private static int getXpForLevel(int level, int offset) {
        int index = offset + Math.max(0, level);
        return index < PET_XP.length ? PET_XP[index] : 1886700;
    }

    private static int calculativeXpForLevel(int level, int offset) {
        int xp = 0;
        for (int i = 0; i < level; i++) xp += getXpForLevel(i, offset);
        return xp;
    }

    // NopoMod calculates overflow progress against the legendary curve after
    // adding the XP already accumulated by the lower-rarity pet.
    private static int calcLevel(float xp) {
        return calcLevel(xp, 20);
    }

    private static int calcLevel(float xp, int offset) {
        float remaining = xp;
        int level = 0;
        while (remaining > 0) {
            remaining -= getXpForLevel(level, offset);
            level++;
        }
        return Math.max(1, level);
    }

    private static float leftoverXp(float xp) {
        return leftoverXp(xp, 20);
    }

    private static float leftoverXp(float xp, int offset) {
        float remaining = xp;
        int level = 0;
        while (remaining > 0) {
            float needed = getXpForLevel(level, offset);
            if (remaining > needed) remaining -= needed;
            else return remaining;
            level++;
        }
        return 0;
    }

    private static void handleSlayer(Component message) {
        if (!isHypixel()) return;
        String text = clean(message.getString());
        Matcher start = SLAYER_START.matcher(text);
        if (start.matches()) {
            currentSlayer = start.group("slayer");
            SLAYERS.computeIfAbsent(currentSlayer, k -> new SlayerData()).kills++;
            saveJson();
            return;
        }

        Matcher drop = SLAYER_DROP.matcher(text);
        if (!drop.matches() || currentSlayer == null) return;

        String item = drop.group("item").trim();
        SlayerData data = SLAYERS.computeIfAbsent(currentSlayer, k -> new SlayerData());
        int since = data.kills - data.lastDropKills.getOrDefault(item, data.kills);
        data.lastDropKills.put(item, data.kills);
        saveJson();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§6SkyJew §7| Took " + since
                + (since == 1 ? " boss" : " bosses") + " to drop " + item));
        }
    }

    private static void handleRareCrop(Component message) {
        if (!isHypixel()) return;
        String text = clean(message.getString());
        String crop = null;

        Matcher m = RARE_CROP.matcher(text);
        if (m.matches()) crop = m.group("crop").trim();
        else if ((m = PET_DROP.matcher(text)).matches()) crop = m.group("pet").trim();
        else if ((m = MOSQUITO.matcher(text)).matches()) crop = m.group("crop").trim();
        else if ((m = RAT.matcher(text)).matches()) crop = m.group("crop").trim();
        else if (CROP_FEVER.equals(text)) crop = "CROP FEVER";

        if (crop == null || crop.isBlank()) return;
        long now = System.currentTimeMillis();
        List<Long> times = CROP_TIMES.computeIfAbsent(crop, k -> new ArrayList<>());
        if (!times.isEmpty()) {
            long previous = times.get(times.size() - 1);
            long elapsed = now - previous;
            if (elapsed > 0) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("§6SkyJew §7| Took "
                        + formatDuration(elapsed) + " to drop " + crop));
                }
            }
        }
        times.add(now);
        if (times.size() > 500) times.remove(0);
        saveJson();
    }

    private static String clean(String s) {
        return s.replaceAll("§.", "");
    }

    private static boolean isHypixel() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getCurrentServer() == null || mc.getCurrentServer().ip == null) return false;
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
            return ip.equals("hypixel.net") || ip.endsWith(".hypixel.net");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void loadEmojis() {
        EMOJIS.clear();
        EMOJI_CANONICAL.clear();
        try (InputStream in = SkyJewNopoFeatures.class.getResourceAsStream("/assets/skyjew/emojis.json")) {
            if (in == null) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("emojis")) {
                JsonObject emoji = element.getAsJsonObject();
                String name = emoji.get("name").getAsString();
                EMOJIS.add(name);
                EMOJI_CANONICAL.put(name, name);
                if (emoji.has("alternatives")) {
                    for (var alt : emoji.getAsJsonArray("alternatives")) {
                        String alias = alt.getAsString();
                        EMOJIS.add(alias);
                        EMOJI_CANONICAL.put(alias, name);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addDefaultEmoji(String name, String symbol) {
        EMOJIS.add(name);
        EMOJI_SYMBOLS.put(name, symbol);
    }

    private static void loadDefaultEmojiSymbols() {
        addDefaultEmoji("wave", "👋");
        addDefaultEmoji("smile", "😄");
        addDefaultEmoji("grin", "😁");
        addDefaultEmoji("joy", "😂");
        addDefaultEmoji("heart", "❤️");
        addDefaultEmoji("fire", "🔥");
        addDefaultEmoji("sob", "😭");
        addDefaultEmoji("cry", "😢");
        addDefaultEmoji("angry", "😠");
        addDefaultEmoji("laughing", "😆");
        addDefaultEmoji("rofl", "🤣");
        addDefaultEmoji("wink", "😉");
        addDefaultEmoji("thinking", "🤔");
        addDefaultEmoji("eyes", "👀");
        addDefaultEmoji("thumbsup", "👍");
        addDefaultEmoji("+1", "👍");
        addDefaultEmoji("thumbsdown", "👎");
        addDefaultEmoji("-1", "👎");
        addDefaultEmoji("clap", "👏");
        addDefaultEmoji("pray", "🙏");
        addDefaultEmoji("ok_hand", "👌");
        addDefaultEmoji("sunglasses", "😎");
    }

    public static Component replaceChatEmojis(Component message) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.chat.chatEmoji) return message;

        final MutableComponent result = Component.empty();
        message.visit((style, value) -> {
            if (value == null || value.isEmpty()) return Optional.empty();

            MutableComponent out = Component.empty();
            Matcher matcher = EMOJI_PATTERN.matcher(value);
            int cursor = 0;
            while (matcher.find()) {
                if (matcher.start() > cursor) {
                    out.append(Component.literal(value.substring(cursor, matcher.start())).withStyle(style));
                }
                String name = matcher.group(1);
                if (EMOJIS.contains(name)) {
                    String canonical = EMOJI_CANONICAL.getOrDefault(name, name);
                    // Nopo's implementation uses the vanilla GUI atlas. SkyJew
                    // ships the same sprites under its own namespace, so aliases
                    // resolve to the canonical sprite without a Nopo dependency.
                    out.append(Component.object(new AtlasSprite(
                        Identifier.withDefaultNamespace("gui"),
                        Identifier.fromNamespaceAndPath("skyjew", canonical)
                    )).withStyle(style));
                } else {
                    out.append(Component.literal(matcher.group()).withStyle(style));
                }
                cursor = matcher.end();
            }
            if (cursor < value.length()) {
                out.append(Component.literal(value.substring(cursor)).withStyle(style));
            }
            result.append(out);
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static boolean chatEmojisEnabled() {
        SkyJewConfig config = SkyJewConfig.current();
        return config != null && config.chat.chatEmoji;
    }

    public static List<String> getChatEmojiSuggestions() {
        if (!chatEmojisEnabled()) return List.of();
        return new ArrayList<>(EMOJIS.stream().map(name -> ":" + name + ":").sorted().toList());
    }

    public static boolean isChatEmoji(String value) {
        if (value == null) return false;
        String clean = value;
        if (clean.startsWith(":")) clean = clean.substring(1);
        if (clean.endsWith(":")) clean = clean.substring(0, clean.length() - 1);
        return chatEmojisEnabled() && EMOJIS.contains(clean);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%,.1f", value);
    }

    private static String formatCompact(int value) {
        if (value >= 1_000_000) return String.format(Locale.US, "%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format(Locale.US, "%.1fk", value / 1_000.0);
        return Integer.toString(value);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private static void loadJson() {
        SLAYERS.clear();
        CROP_TIMES.clear();
        readJson(SLAYER_FILE, JsonObject.class, root -> {
            if (root.has("slayers")) {
                for (var e : root.getAsJsonObject("slayers").entrySet()) {
                    JsonObject v = e.getValue().getAsJsonObject();
                    SlayerData data = new SlayerData();
                    data.kills = v.has("kills") ? v.get("kills").getAsInt() : 0;
                    if (v.has("lastDropKills")) {
                        for (var d : v.getAsJsonObject("lastDropKills").entrySet())
                            data.lastDropKills.put(d.getKey(), d.getValue().getAsInt());
                    }
                    SLAYERS.put(e.getKey(), data);
                }
            }
        });
        readJson(CROP_FILE, JsonObject.class, root -> {
            if (root.has("drops")) {
                for (var e : root.getAsJsonObject("drops").entrySet()) {
                    List<Long> list = new ArrayList<>();
                    for (var v : e.getValue().getAsJsonArray()) list.add(v.getAsLong());
                    CROP_TIMES.put(e.getKey(), list);
                }
            }
        });
    }

    private interface RootConsumer { void accept(JsonObject root); }

    private static <T> void readJson(String fileName, Class<T> ignored, RootConsumer consumer) {
        if (configDir == null) return;
        Path file = configDir.resolve(fileName);
        if (!Files.exists(file)) return;
        try {
            consumer.accept(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception ignoredEx) {
        }
    }

    private static void saveJson() {
        if (configDir == null) return;
        try {
            Files.createDirectories(configDir);

            JsonObject slayerRoot = new JsonObject();
            JsonObject slayers = new JsonObject();
            for (var entry : SLAYERS.entrySet()) {
                JsonObject data = new JsonObject();
                data.addProperty("kills", entry.getValue().kills);
                JsonObject drops = new JsonObject();
                entry.getValue().lastDropKills.forEach(drops::addProperty);
                data.add("lastDropKills", drops);
                slayers.add(entry.getKey(), data);
            }
            slayerRoot.add("slayers", slayers);
            Files.writeString(configDir.resolve(SLAYER_FILE), GSON.toJson(slayerRoot), StandardCharsets.UTF_8);

            JsonObject cropRoot = new JsonObject();
            JsonObject crops = new JsonObject();
            CROP_TIMES.forEach((key, values) -> {
                var array = new com.google.gson.JsonArray();
                for (Long value : values) array.add(value);
                crops.add(key, array);
            });
            cropRoot.add("drops", crops);
            Files.writeString(configDir.resolve(CROP_FILE), GSON.toJson(cropRoot), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static final class SlayerData {
        int kills;
        final Map<String, Integer> lastDropKills = new LinkedHashMap<>();
    }
}
