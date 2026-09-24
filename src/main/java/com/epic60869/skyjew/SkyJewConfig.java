package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
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
    @Category(name = "Farming", desc = "Farming overlays and RNG tools.")
    public Farming farming = new Farming();

    @Expose
    @Category(name = "Mining", desc = "Mining overlays and commission tools.")
    public Mining mining = new Mining();

    @Expose
    @Category(name = "Slayers", desc = "Slayer utilities and future drop tracking.")
    public Slayers slayers = new Slayers();

    @Expose
    @Category(name = "Pets", desc = "Pet displays and overflow XP tools.")
    public Pets pets = new Pets();

    @Expose
    @Category(name = "Experiments", desc = "Experimentation Table assistance.")
    public Experiments experiments = new Experiments();

    @Expose
    @Category(name = "Misc", desc = "Nickname and small quality-of-life options.")
    public Misc misc = new Misc();


    public static final class General {
        @ConfigOption(name = "Custom Item Editor", desc = "Open the SkyJew custom item editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable customItemEditor = () -> openCustom();

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

    public static final class Chat {
        @Expose
        @ConfigOption(name = "Compact Chat", desc = "Compact repeated chat messages into one message with an occurrence counter.")
        @ConfigEditorBoolean
        public boolean compactChat = true;

        @Expose
        @ConfigOption(name = "Chat Emoji", desc = "Replace :emoji: shortcodes with SkyJew emoji sprites and provide emoji autocomplete while typing chat.")
        @ConfigEditorBoolean
        public boolean chatEmoji = true;
    }

    public static final class Farming {
        @Expose
        @Accordion
        @ConfigOption(name = "Farming RNG HUD", desc = "Click to expand the farming RNG HUD options.")
        public FarmingRng rng = new FarmingRng();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Lock", desc = "Reduce camera sensitivity while using supported farming tools.")
        public MouseLock mouseLock = new MouseLock();

        @Expose
        @Accordion
        @ConfigOption(name = "Mining Commissions HUD", desc = "Show Dwarven Mines, Crystal Hollows, and Glacite commission progress read from the Hypixel tab list.")
        public MiningCommissions commissions = new MiningCommissions();
    }

    public static final class Mining {
        @Expose
        @Accordion
        @ConfigOption(name = "Mining Commissions", desc = "Show and configure the Mining Commission HUD.")
        public MiningCommissions commissions = new MiningCommissions();
    }

    public static final class MiningCommissions {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the commission HUD when commission data is present in the tab list.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Draw a dark background behind the commission HUD.")
        @ConfigEditorBoolean
        public boolean background = true;

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

        @Expose public int x = 10;
        @Expose public int y = 10;

        @ConfigOption(name = "Edit Position", desc = "Open the HUD editor and drag the Pet Display.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openHudEditor();
    }

    public static final class Experiments {
        @Expose
        @Accordion
        @ConfigOption(name = "Experimental Table", desc = "Experimentation Table solver and protections.")
        public ExperimentalTable table = new ExperimentalTable();
    }

    public static final class ExperimentalTable {
        @Expose @ConfigOption(name = "Enabled", desc = "Enable the Experimentation Table helper.")
        @ConfigEditorBoolean public boolean enabled = true;
        @Expose @ConfigOption(name = "Next Click Highlight", desc = "Highlight the next Chronomatron/Ultrasequencer click.")
        @ConfigEditorBoolean public boolean highlight = true;
        @Expose @ConfigOption(name = "Prevent Misclicks", desc = "Block clicks that do not match the detected sequence.")
        @ConfigEditorBoolean public boolean preventMisclicks = true;
    }

    public static final class MouseLock {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Reduce camera sensitivity while using supported farming tools.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Ground Only", desc = "Only apply Mouse Lock while the player is on the ground.")
        @ConfigEditorBoolean
        public boolean groundOnly = true;
    }

    public static final class Misc {
        @Expose
        @Accordion
        @ConfigOption(name = "Nickname", desc = "Click to expand nickname settings.")
        public Nickname nickname = new Nickname();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Reset", desc = "Reset the mouse cursor when selected SkyBlock menus open.")
        public MouseReset mouseReset = new MouseReset();
    }

    public static final class MouseReset {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable automatic mouse reset for selected menus.")
        @ConfigEditorBoolean
        public boolean enabled = false;

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
        @ConfigOption(name = "Nickname Enabled", desc = "Use your SkyJew nickname in SkyJew global chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

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
        Path dir = mc.gameDirectory.toPath().resolve("config");
        mc.execute(() -> mc.gui.setScreen(new SkyJewCommandKeysScreen(dir)));
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

    public static SkyJewConfig load(Path path) {
        try {
            migrateLegacy(path);
        } catch (Exception e) {
            System.err.println("[SkyJew] Legacy config migration failed: " + e.getMessage());
        }

        FileHolder holder = new FileHolder(path);
        boolean configExisted = Files.exists(path);

        managed = new ManagedConfig<>(new io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder<>(
            holder.file, SkyJewConfig.class
        ));

        // ManagedConfig may keep a new/default config entirely in memory until
        // the config screen is opened. Save it immediately so SkyJew always
        // has a real config file on first launch.
        if (!configExisted) {
            try {
                Files.createDirectories(path.getParent());
                managed.saveToFile();
            } catch (Exception e) {
                System.err.println("[SkyJew] Failed to create initial config: " + e.getMessage());
            }
        }

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

        if (old.has("experimentHelperEnabled")) migrated.experiments.enabled = old.get("experimentHelperEnabled").getAsBoolean();
        if (old.has("experimentHelperHighlight")) migrated.experiments.highlight = old.get("experimentHelperHighlight").getAsBoolean();
        if (old.has("experimentHelperPreventMisclicks")) migrated.experiments.preventMisclicks = old.get("experimentHelperPreventMisclicks").getAsBoolean();
        if (old.has("experimentHelperDebug")) 
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
