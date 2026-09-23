package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
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
    @Category(name = "Farming", desc = "Farming overlays and RNG tools.")
    public Farming farming = new Farming();

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
    @Category(name = "Visual", desc = "Visual helpers and camera behaviour.")
    public Visual visual = new Visual();

    @Expose
    @Category(name = "Nickname", desc = "Your SkyJew global-chat nickname and colour.")
    public Nickname nickname = new Nickname();

    @Expose
    @Category(name = "Discord", desc = "Link and use your own Discord account.")
    public Discord discord = new Discord();

    public static final class General {
        @Expose
        @ConfigOption(name = "Enable SkyJew", desc = "Master switch for SkyJew features.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "Custom Item Editor", desc = "Open the SkyJew custom item editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable customItemEditor = () -> openCustom();

        @ConfigOption(name = "Notes", desc = "Open your SkyJew notes.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable notes = () -> openNotes();

        @ConfigOption(name = "Command Keys", desc = "Configure SkyJew command shortcuts.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable commandKeys = () -> openCommandKeys();

        @Expose
        public boolean firstBootAcknowledged = false;
    }

    public static final class Farming {
        @Expose
        @ConfigOption(name = "Farming RNG HUD", desc = "Show the farming RNG/progress overlay.")
        @ConfigEditorBoolean
        public boolean rngEnabled = true;

        @Expose
        @ConfigOption(name = "RNG HUD Background", desc = "Draw a background behind the farming RNG HUD.")
        @ConfigEditorBoolean
        public boolean rngBackground = false;

        @Expose
        @ConfigOption(name = "RNG HUD Scale", desc = "Scale the farming RNG HUD.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 3.0f, minStep = 0.1f)
        public float rngScale = 1.0f;

        @Expose
        @ConfigOption(name = "RNG HUD X", desc = "Horizontal position of the farming RNG HUD.")
        @ConfigEditorSlider(minValue = 0, maxValue = 2000, minStep = 1)
        public int rngX = 8;

        @Expose
        @ConfigOption(name = "RNG HUD Y", desc = "Vertical position of the farming RNG HUD.")
        @ConfigEditorSlider(minValue = 0, maxValue = 1200, minStep = 1)
        public int rngY = 8;
    }

    public static final class Slayers {
        @ConfigOption(name = "Kills Since Rare Drop", desc = "SkyJew's Slayer drop counter will appear here.")
        @ConfigEditorBoolean
        public boolean killsSinceDrop = true;

        @ConfigOption(name = "Rare Drop Alerts", desc = "Reserved for future configurable Slayer alerts.")
        @ConfigEditorBoolean
        public boolean rareDropAlerts = true;

        @ConfigOption(name = "Slayer HUD", desc = "Reserved for the future Slayer HUD.")
        @ConfigEditorBoolean
        public boolean hud = true;
    }

    public static final class Pets {
        @ConfigOption(name = "Pet Display", desc = "Reserved for the SkyJew pet display.")
        @ConfigEditorBoolean
        public boolean display = true;

        @ConfigOption(name = "Overflow Pet Levels", desc = "Reserved for displaying pet XP beyond level 100.")
        @ConfigEditorBoolean
        public boolean overflowLevels = true;

        @ConfigOption(name = "Auto-Pet Display", desc = "Keep the pet display synced with the active pet.")
        @ConfigEditorBoolean
        public boolean autoDisplay = true;
    }

    public static final class Experiments {
        @Expose
        @ConfigOption(name = "Experiment Solver", desc = "Enable the Experimentation Table helper.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Next Click Highlight", desc = "Highlight the next Chronomatron/Ultrasequencer click.")
        @ConfigEditorBoolean
        public boolean highlight = true;

        @Expose
        @ConfigOption(name = "Prevent Misclicks", desc = "Block clicks that do not match the detected sequence.")
        @ConfigEditorBoolean
        public boolean preventMisclicks = true;

        @Expose
        @ConfigOption(name = "Debug Mode", desc = "Show experiment solver debug information.")
        @ConfigEditorBoolean
        public boolean debug = false;
    }

    public static final class Visual {
        @Expose
        @ConfigOption(name = "Mouse Lock", desc = "Reduce camera sensitivity while aiming at supported farming tools.")
        @ConfigEditorBoolean
        public boolean mouseLockEnabled = false;

        @Expose
        @ConfigOption(name = "Ground Only", desc = "Only apply Mouse Lock while the player is on the ground.")
        @ConfigEditorBoolean
        public boolean mouseLockGroundOnly = true;
    }

    public static final class Nickname {
        @Expose
        @ConfigOption(name = "Nickname Enabled", desc = "Use your SkyJew nickname in SkyJew global chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Name", desc = "The nickname shown by SkyJew. Maximum 32 characters.")
        @ConfigEditorText
        public String name = "";

        @Expose
        @ConfigOption(name = "Style", desc = "Choose a classic Minecraft colour or rainbow.")
        @ConfigEditorDropdown(values = {
            "Plain", "Black", "Dark Blue", "Dark Green", "Dark Aqua",
            "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray",
            "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow",
            "White", "Rainbow"
        })
        public String style = "Plain";

        @Expose
        @ConfigOption(name = "Custom Hex Colour", desc = "Optional #RRGGBB colour. Used when Style is Plain.")
        @ConfigEditorText
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
        if (old.has("enabled")) migrated.general.enabled = old.get("enabled").getAsBoolean();
        if (old.has("firstBootAcknowledged")) migrated.general.firstBootAcknowledged = old.get("firstBootAcknowledged").getAsBoolean();

        if (old.has("farmingRngEnabled")) migrated.farming.rngEnabled = old.get("farmingRngEnabled").getAsBoolean();
        if (old.has("farmingRngBackground")) migrated.farming.rngBackground = old.get("farmingRngBackground").getAsBoolean();
        if (old.has("farmingRngScale")) migrated.farming.rngScale = old.get("farmingRngScale").getAsFloat();
        if (old.has("farmingRngX")) migrated.farming.rngX = old.get("farmingRngX").getAsInt();
        if (old.has("farmingRngY")) migrated.farming.rngY = old.get("farmingRngY").getAsInt();

        if (old.has("mouseLockEnabled")) migrated.visual.mouseLockEnabled = old.get("mouseLockEnabled").getAsBoolean();
        if (old.has("mouseLockGroundOnly")) migrated.visual.mouseLockGroundOnly = old.get("mouseLockGroundOnly").getAsBoolean();

        if (old.has("experimentHelperEnabled")) migrated.experiments.enabled = old.get("experimentHelperEnabled").getAsBoolean();
        if (old.has("experimentHelperHighlight")) migrated.experiments.highlight = old.get("experimentHelperHighlight").getAsBoolean();
        if (old.has("experimentHelperPreventMisclicks")) migrated.experiments.preventMisclicks = old.get("experimentHelperPreventMisclicks").getAsBoolean();
        if (old.has("experimentHelperDebug")) migrated.experiments.debug = old.get("experimentHelperDebug").getAsBoolean();

        if (old.has("nickEnabled")) migrated.nickname.enabled = old.get("nickEnabled").getAsBoolean();
        if (old.has("nickName")) migrated.nickname.name = old.get("nickName").getAsString();
        if (old.has("nickMode")) migrated.nickname.style = legacyStyle(old.get("nickMode").getAsString());
        if (old.has("nickColor")) migrated.nickname.customHex = old.get("nickColor").getAsString();

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
