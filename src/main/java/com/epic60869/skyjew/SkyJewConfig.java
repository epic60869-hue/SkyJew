package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SkyJew's configuration model.
 *
 * The configuration screen is powered directly by MoulConfig, the same
 * configuration engine used by SkyHanni/NotEnoughUpdates.
 */
public final class SkyJewConfig extends Config {
    private static final Gson LEGACY_GSON = new Gson();

    private static ManagedConfig<SkyJewConfig> managed;

    @Expose
    @Category(name = "General", desc = "Core SkyJew settings and utilities.")
    public General general = new General();

    @Expose
    @Category(name = "Chat", desc = "Chat quality-of-life features.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "Combat", desc = "Arrow counter, legion display, cocoon alerts and rare drops.")
    public com.epic60869.skyjew.features.FeatureConfigs.Combat combat = new com.epic60869.skyjew.features.FeatureConfigs.Combat();

    @Expose
    @Category(name = "Slayers", desc = "Slayer tracker, boss phases and drop tracking.")
    public Slayers slayers = new Slayers();

    @Expose
    @Category(name = "Farming", desc = "Garden HUDs, mouse lock and farming RNG tools.")
    public Farming farming = new Farming();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stats, hook timer, bait and rare creature alerts.")
    public com.epic60869.skyjew.features.FeatureConfigs.Fishing fishing = new com.epic60869.skyjew.features.FeatureConfigs.Fishing();

    @Expose
    @Category(name = "Mining", desc = "Commissions, Crystal Hollows map, Divan tools and mineshaft timer.")
    public Mining mining = new Mining();

    @Expose
    @Category(name = "Foraging", desc = "Sweep display.")
    public com.epic60869.skyjew.features.FeatureConfigs.Foraging foraging = new com.epic60869.skyjew.features.FeatureConfigs.Foraging();

    @Expose
    @Category(name = "Runecrafting", desc = "Valuable rune alerts.")
    public com.epic60869.skyjew.features.FeatureConfigs.Runecrafting runecrafting = new com.epic60869.skyjew.features.FeatureConfigs.Runecrafting();

    @Expose
    @Category(name = "Dungeons", desc = "Map, puzzle solvers, secrets, terminals, splits and timers.")
    public com.epic60869.skyjew.features.FeatureConfigs.Dungeons dungeons = new com.epic60869.skyjew.features.FeatureConfigs.Dungeons();

    @Expose
    @Category(name = "Pets", desc = "Pet displays and overflow XP tools.")
    public Pets pets = new Pets();

    @Expose
    @Category(name = "Misc", desc = "Nickname and small quality-of-life options.")
    public Misc misc = new Misc();


    public static final class General {
        @Expose
        @Accordion
        @ConfigOption(name = "Item Custom", desc = "Item and armor customization settings.")
        public ItemCustom itemCustom = new ItemCustom();

        @ConfigOption(name = "Notes", desc = "Open your SkyJew notes.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable notes = () -> openNotes();

        @ConfigOption(name = "Command Keys", desc = "Configure SkyJew command shortcuts.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable commandKeys = () -> openCommandKeys();

        @ConfigOption(name = "Gui Editor", desc = "Open the transparent HUD/GUI editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable guiEditor = () -> openHudEditor();

        @Expose
        public boolean firstBootAcknowledged = false;
    }

    public static final class ItemCustom {
        @ConfigOption(name = "Open Item Editor", desc = "Open the item and armor customization screen.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable openItemEditor = () -> openCustom();

        @Expose
        @ConfigOption(name = "Show Customize Button", desc = "Show a button in the inventory that opens the item and armor customization screen.")
        @ConfigEditorBoolean
        public boolean showCustomizeButton = true;
    }

    public static final class Chat {
        @Expose
        @Accordion
        @ConfigOption(name = "Custom Chat", desc = "Control how SkyJew command output from other players appears in chat.")
        public CustomChat customChat = new CustomChat();

        @Expose
        @ConfigOption(name = "Compact Chat", desc = "Compact repeated chat messages into one message with an occurrence counter.")
        @ConfigEditorBoolean
        public boolean compactChat = true;

        @Expose
        @ConfigOption(name = "Chat Emoji", desc = "Replace :emoji: shortcodes with SkyJew emoji sprites and provide emoji autocomplete while typing chat.")
        @ConfigEditorBoolean
        public boolean chatEmoji = true;

        @Expose
        @ConfigOption(name = "Current Chat Display", desc = "Show which chat you are typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyJew chat) just above the chat box while it is open.")
        @ConfigEditorBoolean
        public boolean currentChatDisplay = true;
    }

    public static final class CustomChat {
        @Expose
        @ConfigOption(name = "Hide Other Players' Commands", desc = "Hide SkyJew command result messages when they belong to another player. Your own command results remain visible.")
        @ConfigEditorBoolean
        public boolean hideOtherCommands = true;
    }

    public static final class Farming {
        @Expose
        @Accordion
        @ConfigOption(name = "Farming RNG HUD", desc = "Click to expand the farming RNG HUD options.")
        public FarmingRng rng = new FarmingRng();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Lock", desc = "Fully lock the camera while holding a farming tool.")
        public MouseLock mouseLock = new MouseLock();

        @Expose
        @Accordion
        @ConfigOption(name = "Garden", desc = "Yaw/pitch, pest cooldown, blocks per second and special drop animations.")
        public com.epic60869.skyjew.features.FeatureConfigs.Garden garden = new com.epic60869.skyjew.features.FeatureConfigs.Garden();

    }

    public static final class Mining {
        @Expose
        @Accordion
        @ConfigOption(name = "Mining Commissions", desc = "Show and configure the Mining Commission HUD.")
        public MiningCommissions commissions = new MiningCommissions();

        @Expose
        @Accordion
        @ConfigOption(name = "Mining Features", desc = "Crystal Hollows map, Divan tools alert and mineshaft timer.")
        public com.epic60869.skyjew.features.FeatureConfigs.MiningFeatures features = new com.epic60869.skyjew.features.FeatureConfigs.MiningFeatures();
    }

    public static final class MiningCommissions {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the commission HUD when commission data is present in the tab list.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a dark background behind the commission HUD.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the commission HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        public int x = 8;

        @Expose
        public int y = 80;

        @ConfigOption(name = "Edit Position", desc = "Open the HUD editor and drag the Mining Commissions HUD.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openHudEditor();
    }

    public static final class FarmingRng {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the farming RNG/progress overlay.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a background behind the farming RNG HUD.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the farming RNG HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        public int x = 8;

        @Expose
        public int y = 8;

        @ConfigOption(name = "Edit Position", desc = "Open the SkyJew HUD editor and drag the Farming RNG HUD.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openRngEditor();
    }

    public static final class Slayers {
        @Expose
        @Accordion
        @ConfigOption(name = "Slayer HUDs", desc = "Slayer tracker and boss phase HUDs.")
        public com.epic60869.skyjew.features.FeatureConfigs.Slayer huds = new com.epic60869.skyjew.features.FeatureConfigs.Slayer();

        @ConfigOption(name = "Kills Since Rare Drop", desc = "Show the kills-since-drop counter.")
        @ConfigEditorBoolean
        public boolean killsSinceDrop = true;
    }

    public static final class Pets {
        @Expose
        @Accordion
        @ConfigOption(name = "Pets Display", desc = "Pet display, overflow XP and positioning.")
        public PetDisplay display = new PetDisplay();
    }

    public static final class PetDisplay {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the active pet HUD.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Overflow Pet Levels", desc = "Show pet XP beyond the normal maximum level.")
        @ConfigEditorBoolean
        public boolean overflowLevels = true;

        @Expose
        @ConfigOption(name = "Auto-Pet Display", desc = "Keep the pet display synced with the active pet.")
        @ConfigEditorBoolean
        public boolean autoDisplay = true;

        @Expose
        @ConfigOption(name = "Scale", desc = "Scale the pet HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float scale = 1.0f;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a dark background behind the pet display.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose public int x = 10;
        @Expose public int y = 10;

        @ConfigOption(name = "Edit Position", desc = "Open the HUD editor and drag the Pet Display.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openHudEditor();
    }

    public static final class MouseLock {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Fully lock the camera while holding a farming tool.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Garden Only", desc = "Only lock the camera on the Garden.")
        @ConfigEditorBoolean
        public boolean gardenOnly = true;

        @Expose
        @ConfigOption(name = "Ground Only", desc = "Only apply Mouse Lock while the player is on the ground.")
        @ConfigEditorBoolean
        public boolean groundOnly = true;
    }

    public static final class PriceTooltip {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show item prices in tooltips.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "3 Day Avg. Price", desc = "The item's average lowest BIN price over the last 3 days.")
        @ConfigEditorBoolean
        public boolean threeDayAverage = true;

        @Expose
        @ConfigOption(name = "Lowest BIN Price", desc = "The item's current lowest Buy It Now price on the auction house.")
        @ConfigEditorBoolean
        public boolean lowestBin = true;

        @Expose
        @ConfigOption(name = "NPC Sell Price", desc = "How much an NPC buys the item for.")
        @ConfigEditorBoolean
        public boolean npcPrice = true;
    }

    public static final class ItemRarity {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a background behind SkyBlock items in your inventory, containers and hotbar using the item's rarity color.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Style", desc = "The shape of the item rarity background.")
        @ConfigEditorDropdown
        public SkyJewItemBackgrounds.Style style = SkyJewItemBackgrounds.Style.SQUARE;

        @Expose
        @ConfigOption(name = "Opacity", desc = "How opaque the item rarity background is.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
        public float opacity = 0.5f;
    }

    public static final class Misc {
        @Expose
        @Accordion
        @ConfigOption(name = "Party Commands", desc = "Let party members use !warp, !allinvite and !pt when you are leader.")
        public com.epic60869.skyjew.features.FeatureConfigs.PartyCommands partyCommands = new com.epic60869.skyjew.features.FeatureConfigs.PartyCommands();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Rarity", desc = "Rarity-coloured backgrounds behind SkyBlock items.")
        public ItemRarity itemRarity = new ItemRarity();

        @Expose
        @Accordion
        @ConfigOption(name = "Experimental Table", desc = "Experimentation table solvers: Chronomatron, Superpairs and Ultrasequencer.")
        public com.epic60869.skyjew.features.FeatureConfigs.Enchanting experimentalTable = new com.epic60869.skyjew.features.FeatureConfigs.Enchanting();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Price Tooltip", desc = "Add prices to SkyBlock item tooltips, like Skyblocker.")
        public PriceTooltip priceTooltip = new PriceTooltip();

        @Expose
        @Accordion
        @ConfigOption(name = "Nickname", desc = "Click to expand nickname settings.")
        public Nickname nickname = new Nickname();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Reset", desc = "Reset the mouse cursor when selected SkyBlock menus open.")
        public MouseReset mouseReset = new MouseReset();

        @Expose
        @ConfigOption(name = "Calendar Time to Real Time", desc = "When enabled, hovering a SkyBlock calendar date adds the equivalent real-world date and time in your computer's local time zone.")
        @ConfigEditorBoolean
        public boolean calendarTimeToRealTime = true;
    }

    public static final class MouseReset {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable automatic mouse reset for selected menus.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Accessory Bag", desc = "Reset the cursor when the Accessory Bag opens.")
        @ConfigEditorBoolean
        public boolean accessoryBag = true;

        @Expose
        @ConfigOption(name = "Ender Chest", desc = "Reset the cursor when an Ender Chest opens.")
        @ConfigEditorBoolean
        public boolean enderChest = true;

        @Expose
        @ConfigOption(name = "Backpack", desc = "Reset the cursor when a Backpack opens.")
        @ConfigEditorBoolean
        public boolean backpack = true;
    }

    public static final class Nickname {
        @Expose
        @ConfigOption(name = "Nickname Enabled", desc = "Use your SkyJew nickname in SkyBlock TAB and chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "See Other Nicks", desc = "Replace the real usernames of other SkyJew users with their synced nicknames in Hypixel chat, including normal, guild and private messages.")
        @ConfigEditorBoolean
        public boolean seeOtherNicks = true;

        @ConfigOption(name = "Open Nickname Menu", desc = "Open the dedicated /sj nick editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable openMenu = () -> openNick();

        @Expose
        public String name = "";

        @Expose
        public String style = "Plain";

        @Expose
        public String customHex = "";
    }

    public static final class Discord {
        @ConfigOption(name = "Open Discord", desc = "Open SkyJew's personal Discord linking screen.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable open = () -> openDiscord();
    }

    private static void openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SkyJewCustom.open(mc, mc.gui.screen()));
    }

    private static void openNotes() {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.gameDirectory.toPath().resolve("config");
        mc.execute(() -> mc.gui.setScreen(new SkyJewNotesScreen(dir)));
    }

    private static void openCommandKeys() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(com.epic60869.skyjew.commandkeys.CommandKeys.getConfigScreen(mc.gui.screen())));
    }

    private static void openRngEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewRngHudScreen(mc.gui.screen())));
    }

    private static void openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewHudEditorScreen(mc.gui.screen())));
    }

    private static void openNick() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewNickScreen(mc.gui.screen())));
    }

    private static void openDiscord() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewDiscordScreen(mc.gui.screen())));
    }

    @Override
    public StructuredText getTitle() {
        return StructuredText.of("§dSkyJew Mod");
    }

    private static final Gson SNAPSHOT_GSON = new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static String lastSaved;
    private static int saveCheckTicks;

    private static String snapshot() {
        try {
            return managed == null ? null : SNAPSHOT_GSON.toJson(managed.getInstance());
        } catch (Exception e) {
            return null;
        }
    }

    public static SkyJewConfig load(Path path) {
        try {
            migrateLegacy(path);
            migrateConfigShape(path);
        } catch (Exception e) {
            System.err.println("[SkyJew] Legacy config migration failed: " + e.getMessage());
        }

        FileHolder holder = new FileHolder(path);
        boolean configExisted = Files.exists(path);

        managed = new ManagedConfig<>(new io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder<>(
            holder.file, SkyJewConfig.class
        ));

        // Always materialize the current config after loading. This is important
        // for newly-added settings such as Mouse Reset: older SkyJew config files
        // may not contain the new nested section yet, and leaving defaults only
        // in memory makes them appear to reset after a restart.
        try {
            Files.createDirectories(path.getParent());
            managed.saveToFile();
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to persist config after load: " + e.getMessage());
        }

        // MoulConfig calls saveNow() when its GUI closes, which only runs these runnables.
        // Without this, changes made in /sj were lost unless something else saved later.
        managed.getInstance().saveRunnables.add(managed::saveToFile);

        // Also save whenever any setting changes. Relying on the GUI-close hook alone lost changes
        // (for example the item rarity style) when the screen was closed in ways that skip it.
        lastSaved = snapshot();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++saveCheckTicks % 20 != 0 || managed == null) return;
            String now = snapshot();
            if (now != null && !now.equals(lastSaved)) {
                lastSaved = now;
                try {
                    managed.saveToFile();
                } catch (Exception e) {
                    System.err.println("[SkyJew] Failed to save config: " + e.getMessage());
                }
            }
        });

        return managed.getInstance();
    }

    public static SkyJewConfig current() {
        return managed == null ? null : managed.getInstance();
    }

    public static void openGui() {
        if (managed != null) {
            managed.openConfigGui();
        }
    }

    public static void saveCurrent(SkyJewConfig config) {
        if (managed != null && managed.getInstance() == config) {
            managed.saveToFile();
            return;
        }

        Path path = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyjew-mod.json");
        config.save(path);
    }

    public void save(Path path) {
        if (managed != null && managed.getInstance() == this) {
            managed.saveToFile();
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, LEGACY_GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyJew] Failed to save config: " + e.getMessage());
        }
    }

    private static void migrateLegacy(Path path) throws IOException {
        if (Files.notExists(path)) return;

        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject old = LEGACY_GSON.fromJson(raw, JsonObject.class);
        if (old == null || old.has("general")) return;

        SkyJewConfig migrated = new SkyJewConfig();
        if (old.has("enabled")) 
        if (old.has("firstBootAcknowledged")) migrated.general.firstBootAcknowledged = old.get("firstBootAcknowledged").getAsBoolean();

        if (old.has("farmingRngEnabled")) migrated.farming.rng.enabled = old.get("farmingRngEnabled").getAsBoolean();
        if (old.has("farmingRngBackground")) migrated.farming.rng.background = old.get("farmingRngBackground").getAsBoolean();
        if (old.has("farmingRngScale")) migrated.farming.rng.scale = old.get("farmingRngScale").getAsFloat();
        if (old.has("farmingRngX")) migrated.farming.rng.x = old.get("farmingRngX").getAsInt();
        if (old.has("farmingRngY")) migrated.farming.rng.y = old.get("farmingRngY").getAsInt();
        if (old.has("farmingRngX")) 
        if (old.has("mouseLockEnabled")) migrated.farming.mouseLock.enabled = old.get("mouseLockEnabled").getAsBoolean();
        if (old.has("mouseLockGroundOnly")) migrated.farming.mouseLock.groundOnly = old.get("mouseLockGroundOnly").getAsBoolean();

        if (old.has("nickEnabled")) migrated.misc.nickname.enabled = old.get("nickEnabled").getAsBoolean();
        if (old.has("nickName")) migrated.misc.nickname.name = old.get("nickName").getAsString();
        if (old.has("nickMode")) migrated.misc.nickname.style = legacyStyle(old.get("nickMode").getAsString());
        if (old.has("nickColor")) migrated.misc.nickname.customHex = old.get("nickColor").getAsString();

        Path backup = path.resolveSibling(path.getFileName() + ".legacy-backup");
        Files.move(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Write the migrated object as ordinary JSON. ManagedConfig will load it
        // immediately on the next line.
        Files.writeString(path, LEGACY_GSON.toJson(migrated), StandardCharsets.UTF_8);
    }

    private static void migrateConfigShape(Path path) throws IOException {
        if (Files.notExists(path)) return;
        JsonObject root = LEGACY_GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
        if (root == null) return;
        boolean changed = false;

        if (root.has("pets") && root.get("pets").isJsonObject()) {
            JsonObject pets = root.getAsJsonObject("pets");
            if (pets.has("display") && pets.get("display").isJsonPrimitive()) {
                JsonObject display = new JsonObject();
                display.addProperty("enabled", pets.get("display").getAsBoolean());
                if (pets.has("overflowLevels")) display.add("overflowLevels", pets.remove("overflowLevels"));
                if (pets.has("autoDisplay")) display.add("autoDisplay", pets.remove("autoDisplay"));
                if (pets.has("x")) display.add("x", pets.remove("x"));
                if (pets.has("y")) display.add("y", pets.remove("y"));
                pets.add("display", display);
                changed = true;
            }
        }

        // Materialize Mouse Reset defaults in older configs so the setting
        // is persisted instead of falling back to an in-memory default.
        if (!root.has("misc") || !root.get("misc").isJsonObject()) {
            JsonObject misc = new JsonObject();
            root.add("misc", misc);
            changed = true;
        }
        JsonObject misc = root.getAsJsonObject("misc");
        if (!misc.has("mouseReset") || !misc.get("mouseReset").isJsonObject()) {
            JsonObject mouseReset = new JsonObject();
            mouseReset.addProperty("enabled", true);
            mouseReset.addProperty("accessoryBag", true);
            mouseReset.addProperty("enderChest", true);
            mouseReset.addProperty("backpack", true);
            misc.add("mouseReset", mouseReset);
            changed = true;
        } else {
            JsonObject mouseReset = misc.getAsJsonObject("mouseReset");
            if (!mouseReset.has("enabled")) { mouseReset.addProperty("enabled", true); changed = true; }
            if (!mouseReset.has("accessoryBag")) { mouseReset.addProperty("accessoryBag", true); changed = true; }
            if (!mouseReset.has("enderChest")) { mouseReset.addProperty("enderChest", true); changed = true; }
            if (!mouseReset.has("backpack")) { mouseReset.addProperty("backpack", true); changed = true; }
        }

        // The Enchanting tab became the Experimental Table section in Misc.
        if (root.has("enchanting") && root.get("enchanting").isJsonObject()) {
            if (!misc.has("experimentalTable")) misc.add("experimentalTable", root.get("enchanting"));
            root.remove("enchanting");
            changed = true;
        }

        if (root.has("farming") && root.get("farming").isJsonObject()) {
            JsonObject farming = root.getAsJsonObject("farming");
            if (farming.has("commissions")) {
                JsonObject mining = root.has("mining") && root.get("mining").isJsonObject()
                    ? root.getAsJsonObject("mining") : new JsonObject();
                if (!mining.has("commissions")) mining.add("commissions", farming.remove("commissions"));
                root.add("mining", mining);
                changed = true;
            }
        }

        if (changed) Files.writeString(path, LEGACY_GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static String legacyStyle(String style) {
        if (style == null || style.isBlank() || style.equals("plain")) return "Plain";
        if (style.equalsIgnoreCase("rainbow")) return "Rainbow";
        return switch (style.toLowerCase()) {
            case "dark_blue" -> "Dark Blue";
            case "dark_green" -> "Dark Green";
            case "dark_aqua" -> "Dark Aqua";
            case "dark_red" -> "Dark Red";
            case "dark_purple" -> "Dark Purple";
            case "light_purple" -> "Light Purple";
            case "dark_gray" -> "Dark Gray";
            default -> Character.toUpperCase(style.charAt(0)) + style.substring(1);
        };
    }

    private static final class FileHolder {
        private final java.io.File file;
        private FileHolder(Path path) {
            this.file = path.toFile();
        }
    }
}
