package com.epic60869.skyballs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import org.lwjgl.glfw.GLFW;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
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
 * SkyBalls's configuration model.
 *
 * The configuration screen is powered directly by MoulConfig, the same
 * configuration engine used by SkyHanni/NotEnoughUpdates.
 */
public final class SkyBallsConfig extends Config {
    public static final class General {
        @Expose
        @Accordion
        @ConfigOption(name = "Item Custom", desc = "Item and armor customization settings.")
        public ItemCustom itemCustom = new ItemCustom();

        @ConfigOption(name = "Notes", desc = "Open your SkyBalls notes.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable notes = () -> openNotes();

        @ConfigOption(name = "Command Keys", desc = "Configure SkyBalls command shortcuts.")
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
        @Accordion
        @ConfigOption(name = "Custom Chat", desc = "Control how SkyBalls command output from other players appears in chat.")
        public CustomChat customChat = new CustomChat();

        @Expose
        @Accordion
        @ConfigOption(name = "Copy Chat", desc = "Right-click chat messages to copy them, like NoFrills' Chat Tweaks.")
        public CopyChat copyChat = new CopyChat();

        @Expose
        @ConfigOption(name = "Compact Chat", desc = "Compact repeated chat messages into one message with an occurrence counter.")
        @ConfigEditorBoolean
        public boolean compactChat = true;

        @Expose
        @ConfigOption(name = "Chat Emoji", desc = "Replace :emoji: shortcodes with SkyBalls emoji sprites and provide emoji autocomplete while typing chat.")
        @ConfigEditorBoolean
        public boolean chatEmoji = true;

        @Expose
        @ConfigOption(name = "Current Chat Display", desc = "Show which chat you are typing in (All, Party, Guild, Officer, Co-op, a private conversation or SkyBalls chat) just above the chat box while it is open.")
        @ConfigEditorBoolean
        public boolean currentChatDisplay = true;
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
        public com.epic60869.skyballs.features.FeatureConfigs.Garden garden = new com.epic60869.skyballs.features.FeatureConfigs.Garden();

    }

    public static final class Mining {
        @Expose
        @Accordion
        @ConfigOption(name = "Mining Commissions", desc = "Show and configure the Mining Commission HUD.")
        public MiningCommissions commissions = new MiningCommissions();

        @Expose
        @Accordion
        @ConfigOption(name = "Mining Features", desc = "Crystal Hollows map, Divan tools alert and mineshaft timer.")
        public com.epic60869.skyballs.features.FeatureConfigs.MiningFeatures features = new com.epic60869.skyballs.features.FeatureConfigs.MiningFeatures();
    }

    public static final class Slayers {
        @Expose
        @Accordion
        @ConfigOption(name = "Slayer HUDs", desc = "Slayer tracker and boss phase HUDs.")
        public com.epic60869.skyballs.features.FeatureConfigs.Slayer huds = new com.epic60869.skyballs.features.FeatureConfigs.Slayer();

        @Expose
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

    public static final class Misc {
        @Expose
        @Accordion
        @ConfigOption(name = "Party Commands", desc = "Let party members use !warp, !allinvite and !pt when you are leader.")
        public com.epic60869.skyballs.features.FeatureConfigs.PartyCommands partyCommands = new com.epic60869.skyballs.features.FeatureConfigs.PartyCommands();

        @Expose
        @Accordion
        @ConfigOption(name = "Slot Locking & Binding", desc = "Lock inventory slots (L) and bind hotbar slots to inventory slots (B).")
        public SlotLocking slotLocking = new SlotLocking();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Notification", desc = "Show items from your list on a HUD when they go into your sacks or inventory (SkyOcean's Sack Notification as a HUD).")
        public com.epic60869.skyballs.features.FeatureConfigs.ItemNotification itemNotification = new com.epic60869.skyballs.features.FeatureConfigs.ItemNotification();

        @Expose
        @Accordion
        @ConfigOption(name = "Auto Welcome", desc = "Welcome players on your list in guild chat or with /msg when they come online.")
        public com.epic60869.skyballs.features.FeatureConfigs.AutoWelcome autoWelcome = new com.epic60869.skyballs.features.FeatureConfigs.AutoWelcome();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Rarity", desc = "Rarity-coloured backgrounds behind SkyBlock items.")
        public ItemRarity itemRarity = new ItemRarity();

        @Expose
        @Accordion
        @ConfigOption(name = "Experimental Table", desc = "Experimentation table solvers: Chronomatron, Superpairs and Ultrasequencer.")
        public com.epic60869.skyballs.features.FeatureConfigs.Enchanting experimentalTable = new com.epic60869.skyballs.features.FeatureConfigs.Enchanting();

        @Expose
        @Accordion
        @ConfigOption(name = "Item Price Tooltip", desc = "Add prices to SkyBlock item tooltips, like Skyblocker.")
        public PriceTooltip priceTooltip = new PriceTooltip();

        @Expose
        @Accordion
        @ConfigOption(name = "Random", desc = "Low fire overlay, hidden explosions and other small visual tweaks.")
        public Random random = new Random();

        @Expose
        @Accordion
        @ConfigOption(name = "Player Size", desc = "Make yourself, other players, or both bigger or smaller (client side only), like Odin.")
        public PlayerSize playerSize = new PlayerSize();

        @Expose
        @Accordion
        @ConfigOption(name = "Held Item Model", desc = "Move, rotate and scale the item in your hand and change your swing speed, like Skysoft. /sb helditem save stores the settings for the held item only.")
        public HeldItemModel heldItemModel = new HeldItemModel();

        @Expose
        @Accordion
        @ConfigOption(name = "Nickname", desc = "Click to expand nickname settings.")
        public Nickname nickname = new Nickname();

        @Expose
        @Accordion
        @ConfigOption(name = "Mouse Reset", desc = "Reset the mouse cursor when selected SkyBlock menus open.")
        public MouseReset mouseReset = new MouseReset();

        @Expose
        @Accordion
        @ConfigOption(name = "Tooltip Scroll", desc = "Move tooltips with the mouse wheel and keys so long tooltips can be read (Skysoft's Tooltip Scroll). Off automatically when Skysoft is installed.")
        public TooltipScroll tooltipScroll = new TooltipScroll();

        @Expose
        @ConfigOption(name = "Screenshot Sharing", desc = "After F2, the screenshot message gets an [Upload] button that gives you a link to post in /sbc, like Skysoft. Uploads are public to anyone with the link.")
        @ConfigEditorBoolean
        public boolean screenshotSharing = true;

        @Expose
        @ConfigOption(name = "Screenshot Upload Host", desc = "Where screenshots are uploaded. Litterbox deletes them after the chosen time; Catbox keeps them.")
        @ConfigEditorDropdown
        public com.epic60869.skyballs.features.misc.ScreenshotShare.Host screenshotHost = com.epic60869.skyballs.features.misc.ScreenshotShare.Host.LITTERBOX_72H;

        @Expose
        @ConfigOption(name = "Storage Overlay", desc = "Show every Ender Chest page and backpack at once in /storage and in any page, like Firmament. Click a page's name to open it; the open page and your inventory can be clicked as normal.")
        @ConfigEditorBoolean
        public boolean storageOverlay = true;

        @Expose
        @ConfigOption(name = "Recipe HUD", desc = "While a /sb recipe is selected, show a movable HUD with the item and the base ingredients you still need (like SkyOcean's craft helper overlay). Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean recipeHud = true;

        @Expose
        @ConfigOption(name = "Recipe HUD Hide Completed", desc = "Hide ingredients you already have enough of in the Recipe HUD.")
        @ConfigEditorBoolean
        public boolean recipeHudHideCompleted = false;

        @Expose
        @ConfigOption(name = "Calendar Time to Real Time", desc = "When enabled, hovering a SkyBlock calendar date adds the equivalent real-world date and time in your computer's local time zone.")
        @ConfigEditorBoolean
        public boolean calendarTimeToRealTime = true;

        @Expose
        @ConfigOption(name = "Price Paid", desc = "Remember what you paid for items you buy on the auction house and show it in their tooltip, like NoFrills.")
        @ConfigEditorBoolean
        public boolean pricePaid = true;

        @Expose
        @ConfigOption(name = "Update Notifications", desc = "Tell you in chat when a newer SkyBalls version is out (\"New SkyBalls Mod Version 1.2.3 --> 1.2.5\"), with a download link.")
        @ConfigEditorBoolean
        public boolean updateNotifications = true;

        @Expose
        @ConfigOption(name = "Collection Tracker", desc = "While you mine, farm, forage or fish, show the collection you're gathering, what you've gained this session and per hour, like SkyHanni's farming display. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean collectionTracker = true;

        @Expose
        @ConfigOption(name = "Collection Tracker Elite Rank", desc = "Also show your rank on the Elite (elitebot.dev) collection leaderboard and how much you need to pass the next player.")
        @ConfigEditorBoolean
        public boolean collectionTrackerRank = true;

        /** Collection pinned with /sj trackcollection (a Hypixel item id), or "" to follow what you gather. */
        @Expose
        public String collectionTrackerItem = "";

        /** Goal set with /sj trackcollection &lt;item&gt; &lt;goal&gt;, or 0. */
        @Expose
        public long collectionTrackerGoal = 0;

        @Expose
        @ConfigOption(name = "Warp Shortcuts", desc = "Type /dhub instead of /warp dhub (and the same for every name in the list below). Applies next time you join a server.")
        @ConfigEditorBoolean
        public boolean warpShortcuts = true;

        @Expose
        @ConfigOption(name = "Warp Shortcut List", desc = "Warps that get their own command, separated by commas.")
        @ConfigEditorText
        public String warpShortcutList = "dhub, dungeon_hub, garden, barn, desert, trapper, park, howl, jungle, gold, deep, mines, forge, crystals, nucleus, base, camp, tunnels, end, drag, void, spider, nest, arachne, crimson, isle, kuudra, smold, museum, da, castle, wiz, jerry, rift, galatea, murkwater";

        @Expose
        @ConfigOption(name = "Toggle Sprint", desc = "Always sprint, like Odin's Auto Sprint. Set a \"Toggle Sprint\" key in Controls to switch it on and off.")
        @ConfigEditorBoolean
        public boolean toggleSprint = false;

        @Expose
        @ConfigOption(name = "Toggle Sprint HUD", desc = "Show [Sprinting (Toggled)] while toggle sprint is on. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean toggleSprintHud = true;
    }

    private static final Gson LEGACY_GSON = new Gson();

    private static ManagedConfig<SkyBallsConfig> managed;

    @Expose
    @Category(name = "General", desc = "Core SkyBalls settings and utilities.")
    public General general = new General();

    @Expose
    @Category(name = "Chat", desc = "Chat quality-of-life features.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "Combat", desc = "Arrow counter, legion display, cocoon alerts and rare drops.")
    public com.epic60869.skyballs.features.FeatureConfigs.Combat combat = new com.epic60869.skyballs.features.FeatureConfigs.Combat();

    @Expose
    @Category(name = "Slayers", desc = "Slayer tracker, boss phases and drop tracking.")
    public Slayers slayers = new Slayers();

    @Expose
    @Category(name = "Farming", desc = "Garden HUDs, mouse lock and farming RNG tools.")
    public Farming farming = new Farming();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stats, hook timer, bait and rare creature alerts.")
    public com.epic60869.skyballs.features.FeatureConfigs.Fishing fishing = new com.epic60869.skyballs.features.FeatureConfigs.Fishing();

    @Expose
    @Category(name = "Mining", desc = "Commissions, Crystal Hollows map, Divan tools and mineshaft timer.")
    public Mining mining = new Mining();

    @Expose
    @Category(name = "Foraging", desc = "Sweep display.")
    public com.epic60869.skyballs.features.FeatureConfigs.Foraging foraging = new com.epic60869.skyballs.features.FeatureConfigs.Foraging();

    @Expose
    @Category(name = "Runecrafting", desc = "Valuable rune alerts.")
    public com.epic60869.skyballs.features.FeatureConfigs.Runecrafting runecrafting = new com.epic60869.skyballs.features.FeatureConfigs.Runecrafting();

    @Expose
    @Category(name = "Dungeons", desc = "Map, puzzle solvers, secrets, terminals, splits and timers.")
    public com.epic60869.skyballs.features.FeatureConfigs.Dungeons dungeons = new com.epic60869.skyballs.features.FeatureConfigs.Dungeons();

    @Expose
    @Category(name = "Pets", desc = "Pet displays and overflow XP tools.")
    public Pets pets = new Pets();

    @Expose
    @Category(name = "Misc", desc = "Nickname and small quality-of-life options.")
    public Misc misc = new Misc();

    public static final class TooltipScroll {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Allow tooltips to be moved with the mouse wheel and movement keys.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Enable Scroll Wheel", desc = "Move tooltips with the mouse wheel.")
        @ConfigEditorBoolean
        public boolean enableScrollWheel = true;

        @Expose
        @ConfigOption(name = "Enable in Chat", desc = "Allow tooltip movement while chat is open.")
        @ConfigEditorBoolean
        public boolean enabledInChat = false;

        @Expose
        @ConfigOption(name = "Enable WASD", desc = "Use WASD to move the hovered tooltip.")
        @ConfigEditorBoolean
        public boolean enableWASD = false;

        @Expose
        @ConfigOption(name = "Mouse Scrolling Speed", desc = "Pixels moved per mouse-wheel step.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
        public int mouseScrollingSpeed = 10;

        @Expose
        @ConfigOption(name = "Keyboard Scrolling Speed", desc = "Pixels moved per tick while a tooltip movement key is held.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 40f, minStep = 1f)
        public int keyboardScrollingSpeed = 5;

        @Expose
        @ConfigOption(name = "Move Up Key", desc = "Move the hovered tooltip up.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_PAGE_UP)
        public int moveUpKey = GLFW.GLFW_KEY_PAGE_UP;

        @Expose
        @ConfigOption(name = "Move Down Key", desc = "Move the hovered tooltip down.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_PAGE_DOWN)
        public int moveDownKey = GLFW.GLFW_KEY_PAGE_DOWN;

        @Expose
        @ConfigOption(name = "Horizontal Movement Key", desc = "Hold this key to make up and down movement horizontal.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int horizontalMovementKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Reset Tooltip Key", desc = "Reset the hovered tooltip's moved position.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int resetTooltipKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Start On Top", desc = "Show the top of oversized tooltips when they first appear.")
        @ConfigEditorBoolean
        public boolean startOnTop = false;

        @Expose
        @ConfigOption(name = "Reset Position When Not Hovered", desc = "Reset tooltip movement after the tooltip disappears.")
        @ConfigEditorBoolean
        public boolean resetWhenNotHovered = true;

        @Expose
        @ConfigOption(name = "Use Left Shift", desc = "Hold left shift to move tooltips horizontally with the mouse wheel.")
        @ConfigEditorBoolean
        public boolean useLeftShift = true;

        @Expose
        @ConfigOption(name = "Invert Horizontal Movement", desc = "Invert horizontal tooltip movement.")
        @ConfigEditorBoolean
        public boolean invertHorizontal = false;

        @Expose
        @ConfigOption(name = "Invert Vertical Movement", desc = "Invert vertical tooltip movement.")
        @ConfigEditorBoolean
        public boolean invertVertical = false;

        @Expose
        @ConfigOption(name = "Scroll Smoothness", desc = "How quickly tooltips slide toward the moved position. 100 is instant.")
        @ConfigEditorSlider(minValue = 5f, maxValue = 100f, minStep = 5f)
        public int scrollSmoothness = 25;
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

    public static final class SlotLocking {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Slot locking and slot binding. Locked slots can't be clicked, moved or dropped; bound slots swap with a shift-click (Odin's Slot Binds).")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Lock Key", desc = "Press over a slot in your inventory to lock or unlock it.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_L)
        public int lockKey = GLFW.GLFW_KEY_L;

        @Expose
        @ConfigOption(name = "Bind Key", desc = "In your inventory: press over a slot, then over another (one in the hotbar), to bind them. Press on a bound slot to unbind.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_B)
        public int bindKey = GLFW.GLFW_KEY_B;

        @Expose
        @ConfigOption(name = "Bind Line Only With Shift", desc = "Only show the line between bound slots while holding Shift.")
        @ConfigEditorBoolean
        public boolean lineOnlyWithShift = false;

        /** Locked player inventory slots (0-8 hotbar, 9-35 inventory). */
        @Expose
        public java.util.List<Integer> locked = new java.util.ArrayList<>();

        /** Slot binds, by inventory screen slot (36-44 is the hotbar). */
        @Expose
        public java.util.Map<Integer, Integer> binds = new java.util.HashMap<>();
    }

    public static final class CopyChat {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Copy chat messages, like NoFrills' Chat Tweaks: with chat open, right-click a message to copy it, Shift+right-click to copy one line. SkyBalls rank prefixes aren't copied.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Right-Click To Copy", desc = "Right-click a message to copy it (Shift+right-click for one line).")
        @ConfigEditorBoolean
        public boolean rightClick = true;

        @Expose
        @ConfigOption(name = "Copy Message Key", desc = "Key that copies the message under the mouse while chat is open.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int copyKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Copy Line Key", desc = "Key that copies just the line under the mouse while chat is open.")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        public int copyLineKey = GLFW.GLFW_KEY_UNKNOWN;

        @Expose
        @ConfigOption(name = "Copy Preview", desc = "Show what was copied in chat.")
        @ConfigEditorBoolean
        public boolean preview = true;

        @Expose
        @ConfigOption(name = "Preview Length", desc = "How many characters of the copied text to show (0 just says it was copied).")
        @ConfigEditorSlider(minValue = 0, maxValue = 200, minStep = 10)
        public int previewLength = 50;

        @Expose
        @ConfigOption(name = "Trim On Copy", desc = "Remove spaces at the start and end of what's copied.")
        @ConfigEditorBoolean
        public boolean trim = false;
    }

    public static final class CustomChat {
        @Expose
        @ConfigOption(name = "Show SB Chat", desc = "Show SkyBalls chat (/sbc) messages from other players. Off hides them; you can still send with /sbc.")
        @ConfigEditorBoolean
        public boolean showSjChat = true;

        @Expose
        @ConfigOption(name = "Show Ranks", desc = "Show SkyBalls ranks ([OWNER], [TESTER], ...) in front of names in /sbc. Turn off to hide them.")
        @ConfigEditorBoolean
        public boolean showRanks = true;

        @Expose
        @ConfigOption(name = "SB Chat Ping", desc = "Play a little ping when someone sends a message in /sbc.")
        @ConfigEditorBoolean
        public boolean pingSound = false;

        @Expose
        @ConfigOption(name = "Hide Other Players' Commands", desc = "Hide SkyBalls command result messages when they belong to another player. Your own command results remain visible.")
        @ConfigEditorBoolean
        public boolean hideOtherCommands = true;
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

        @ConfigOption(name = "Edit Position", desc = "Open the SkyBalls HUD editor and drag the Farming RNG HUD.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable editPosition = () -> openRngEditor();
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
        @ConfigOption(name = "Bazaar Prices", desc = "For items sold on the bazaar instead of the auction house, show the bazaar insta-buy and insta-sell price where the lowest BIN and 3 day average would be (for the whole stack, or the whole sack in the Sacks menu).")
        @ConfigEditorBoolean
        public boolean bazaar = true;

        @Expose
        @ConfigOption(name = "NPC Sell Price", desc = "How much an NPC buys the item for.")
        @ConfigEditorBoolean
        public boolean npcPrice = true;

        @Expose
        @ConfigOption(name = "Lowest BIN Price", desc = "The item's current lowest Buy It Now price on the auction house.")
        @ConfigEditorBoolean
        public boolean lowestBin = true;

        @Expose
        @ConfigOption(name = "3 Day Avg. Price", desc = "The item's average lowest BIN price over the last 3 days.")
        @ConfigEditorBoolean
        public boolean threeDayAverage = true;
    }

    public static final class ItemRarity {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show a background behind SkyBlock items in your inventory, containers and hotbar using the item's rarity color.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Style", desc = "The shape of the item rarity background.")
        @ConfigEditorDropdown
        public SkyBallsItemBackgrounds.Style style = SkyBallsItemBackgrounds.Style.SQUARE;

        @Expose
        @ConfigOption(name = "Opacity", desc = "How opaque the item rarity background is.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
        public float opacity = 0.5f;
    }

    public static final class PlayerSize {
        @Expose
        @ConfigOption(name = "Scale Yourself", desc = "Change your own player's size (third person, inventory preview).")
        @ConfigEditorBoolean
        public boolean self = false;

        @Expose @ConfigOption(name = "Your Width", desc = "X scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float selfX = 0.6f;
        @Expose @ConfigOption(name = "Your Height", desc = "Y scale (1 = normal, negative = upside down).") @ConfigEditorSlider(minValue = -1f, maxValue = 3f, minStep = 0.05f) public float selfY = 0.6f;
        @Expose @ConfigOption(name = "Your Depth", desc = "Z scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float selfZ = 0.6f;

        @Expose
        @ConfigOption(name = "Scale Others", desc = "Change the size of every other real player (NPCs are left alone).")
        @ConfigEditorBoolean
        public boolean others = false;

        @Expose @ConfigOption(name = "Others Width", desc = "X scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float othersX = 0.6f;
        @Expose @ConfigOption(name = "Others Height", desc = "Y scale (1 = normal, negative = upside down).") @ConfigEditorSlider(minValue = -1f, maxValue = 3f, minStep = 0.05f) public float othersY = 0.6f;
        @Expose @ConfigOption(name = "Others Depth", desc = "Z scale (1 = normal).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float othersZ = 0.6f;
    }

    public static final class HeldItemModel {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Apply these settings to the item in your hand (first person).")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose @ConfigOption(name = "X", desc = "Left / right.") @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f) public float x = 0f;
        @Expose @ConfigOption(name = "Y", desc = "Down / up.") @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f) public float y = 0f;
        @Expose @ConfigOption(name = "Z", desc = "Towards / away from you.") @ConfigEditorSlider(minValue = -1.5f, maxValue = 1.5f, minStep = 0.01f) public float z = 0f;
        @Expose @ConfigOption(name = "Scale", desc = "Item size (1 = vanilla).") @ConfigEditorSlider(minValue = 0.05f, maxValue = 2f, minStep = 0.01f) public float scale = 1f;
        @Expose @ConfigOption(name = "Rotation X", desc = "Pitch in degrees.") @ConfigEditorSlider(minValue = -180, maxValue = 180, minStep = 1) public float rotationX = 0f;
        @Expose @ConfigOption(name = "Rotation Y", desc = "Yaw in degrees.") @ConfigEditorSlider(minValue = -180, maxValue = 180, minStep = 1) public float rotationY = 0f;
        @Expose @ConfigOption(name = "Rotation Z", desc = "Roll in degrees.") @ConfigEditorSlider(minValue = -180, maxValue = 180, minStep = 1) public float rotationZ = 0f;
        @Expose @ConfigOption(name = "Swing Speed", desc = "Arm swing speed (1 = vanilla, 2 = twice as fast, 0.5 = half speed).") @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f) public float swingSpeed = 1f;
        @Expose @ConfigOption(name = "No Swing Animation", desc = "Your hand and held item don't swing when you click, like NoFrills. Works even with Held Item Model off. Off by default.") @ConfigEditorBoolean public boolean noSwing = false;
        @Expose @ConfigOption(name = "Swing X", desc = "How far the item moves sideways when you swing (1 = vanilla, 0 = none). With X, Y and Z at 0 the item still rotates but stays in place, like NoammAddons and NoFrills.") @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 0.05f) public float swingX = 1f;
        @Expose @ConfigOption(name = "Swing Y", desc = "How far the item moves up and down when you swing (1 = vanilla, 0 = none).") @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 0.05f) public float swingY = 1f;
        @Expose @ConfigOption(name = "Swing Z", desc = "How far the item moves forward when you swing (1 = vanilla, 0 = none).") @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 0.05f) public float swingZ = 1f;

        @Expose
        @ConfigOption(name = "Ignore Mining Effects", desc = "Swing at the normal speed even with Haste or Mining Fatigue.")
        @ConfigEditorBoolean
        public boolean ignoreMiningEffects = false;
    }

    public static final class Random {
        @Expose
        @ConfigOption(name = "Low Fire", desc = "Lower the burning overlay on your screen so it covers less of the view.")
        @ConfigEditorBoolean
        public boolean lowFire = false;

        @Expose
        @ConfigOption(name = "Fire Height", desc = "How far to lower the fire overlay (0 = vanilla, 1 = off the screen).")
        @ConfigEditorSlider(minValue = 0, maxValue = 1, minStep = 0.05f)
        public float fireOffset = 0.3f;

        @Expose
        @ConfigOption(name = "Hide Explosions", desc = "Hide explosion particles (TNT, Bonzo staff, Wither impact and other server explosions).")
        @ConfigEditorBoolean
        public boolean hideExplosions = false;
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
        @ConfigOption(name = "Nickname Enabled", desc = "Use your SkyBalls nickname in SkyBlock TAB and chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "See Other Nicks", desc = "Replace the real usernames of other SkyBalls users with their synced nicknames in Hypixel chat, including normal, guild and private messages.")
        @ConfigEditorBoolean
        public boolean seeOtherNicks = true;

        @ConfigOption(name = "Open Nickname Menu", desc = "Open the dedicated /sb nick editor.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable openMenu = () -> openNick();

        @Expose
        public String name = "";

        @Expose
        public String style = "Plain";

        @Expose
        public String customHex = "";

        @Expose
        public String font = "Default";

        /** Players whose nickname you turned off with /sb togglenick. */
        @Expose
        public java.util.List<String> hiddenNicks = new java.util.ArrayList<>();
    }

    public static final class Discord {
        @ConfigOption(name = "Open Discord", desc = "Open SkyBalls's personal Discord linking screen.")
        @ConfigEditorButton(buttonText = "OPEN")
        public Runnable open = () -> openDiscord();
    }

    private static void openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SkyBallsCustom.open(mc, mc.gui.screen()));
    }

    private static void openNotes() {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.gameDirectory.toPath().resolve("config");
        mc.execute(() -> mc.gui.setScreen(new SkyBallsNotesScreen(dir)));
    }

    private static void openCommandKeys() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(com.epic60869.skyballs.commandkeys.CommandKeys.getConfigScreen(mc.gui.screen())));
    }

    private static void openRngEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsRngHudScreen(mc.gui.screen())));
    }

    private static void openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsHudEditorScreen(mc.gui.screen())));
    }

    private static void openNick() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsNickScreen(mc.gui.screen())));
    }

    private static void openDiscord() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsDiscordScreen(mc.gui.screen())));
    }

    @Override
    public StructuredText getTitle() {
        // "SkyBalls Mod v1.2.3": the installed version, from fabric.mod.json.
        String version = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyballs")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
        return StructuredText.of("§dSkyBalls Mod" + (version.isEmpty() ? "" : " §7v" + version));
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

    public static SkyBallsConfig load(Path path) {
        try {
            migrateLegacy(path);
            migrateConfigShape(path);
        } catch (Exception e) {
            System.err.println("[SkyBalls] Legacy config migration failed: " + e.getMessage());
        }

        FileHolder holder = new FileHolder(path);
        boolean configExisted = Files.exists(path);

        managed = new ManagedConfig<>(new io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder<>(
            holder.file, SkyBallsConfig.class
        ));

        // Always materialize the current config after loading. This is important
        // for newly-added settings such as Mouse Reset: older SkyBalls config files
        // may not contain the new nested section yet, and leaving defaults only
        // in memory makes them appear to reset after a restart.
        try {
            Files.createDirectories(path.getParent());
            managed.saveToFile();
        } catch (Exception e) {
            System.err.println("[SkyBalls] Failed to persist config after load: " + e.getMessage());
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
                    System.err.println("[SkyBalls] Failed to save config: " + e.getMessage());
                }
            }
        });

        return managed.getInstance();
    }

    public static SkyBallsConfig current() {
        return managed == null ? null : managed.getInstance();
    }

    public static void openGui() {
        if (managed != null) {
            managed.openConfigGui();
        }
    }

    public static void saveCurrent(SkyBallsConfig config) {
        if (managed != null && managed.getInstance() == config) {
            managed.saveToFile();
            return;
        }

        Path path = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyballs-mod.json");
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
            System.err.println("[SkyBalls] Failed to save config: " + e.getMessage());
        }
    }

    private static void migrateLegacy(Path path) throws IOException {
        if (Files.notExists(path)) return;

        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject old = LEGACY_GSON.fromJson(raw, JsonObject.class);
        if (old == null || old.has("general")) return;

        SkyBallsConfig migrated = new SkyBallsConfig();
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
