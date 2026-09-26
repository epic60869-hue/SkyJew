package com.epic60869.skyjew.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/**
 * Config sections for SkyJew's skill features. They are referenced as categories or
 * accordions from {@link com.epic60869.skyjew.SkyJewConfig}. HUD positions and scales are
 * set in /sj gui and stored in skyjew-huds.json.
 */
public final class FeatureConfigs {
    private FeatureConfigs() {}

    public static final class Combat {
        @Expose
        @Accordion
        @ConfigOption(name = "Cocoon Alert", desc = "Alert when you cocoon a mob.")
        public CocoonAlert cocoonAlert = new CocoonAlert();

        @Expose
        @Accordion
        @ConfigOption(name = "Rare Drops", desc = "Copy rare drops and animate big ones.")
        public RareDrops rareDrops = new RareDrops();

        @Expose
        @ConfigOption(name = "Arrow Counter", desc = "HUD showing the selected arrow type and how many arrows are left in your quiver.")
        @ConfigEditorBoolean
        public boolean arrowCounter = true;

        @Expose
        @ConfigOption(name = "Zealot Tracker", desc = "Tracker for the Zealots you kill in the End and the Summoning Eyes you drop, for this session or in total.")
        @ConfigEditorBoolean
        public boolean zealotCounter = true;

        @Expose
        @ConfigOption(name = "Legion Display", desc = "HUD showing how many players are within Legion range (30 blocks).")
        @ConfigEditorBoolean
        public boolean legionDisplay = false;
    }

    public static final class CocoonAlert {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show an on-screen alert when you cocoon a slayer boss, slayer miniboss, elusive mob or important boss.")
        @ConfigEditorBoolean
        public boolean enabled = true;
    }

    public static final class RareDrops {
        @Expose
        @ConfigOption(name = "Copy Rare Drops", desc = "Copy rare drop messages to your clipboard.")
        @ConfigEditorBoolean
        public boolean copy = true;

        @Expose
        @ConfigOption(name = "Big Drop Animation", desc = "Play an animation when a rare drop is worth more than the threshold.")
        @ConfigEditorBoolean
        public boolean animation = true;

        @Expose
        @ConfigOption(name = "Animation Threshold (millions)", desc = "Minimum drop value, in millions of coins, for the animation.")
        @ConfigEditorSlider(minValue = 1, maxValue = 500, minStep = 1)
        public float thresholdMillions = 10;
    }

    public static final class Slayer {
        @Expose
        @ConfigOption(name = "Slayer Tracker", desc = "HUD with slayer XP earned, bosses to the next level and progress to spawning the boss.")
        @ConfigEditorBoolean
        public boolean tracker = true;

        @Expose
        @ConfigOption(name = "Boss Phase Display", desc = "HUD showing your slayer boss's nametag lines, including Voidgloom hits and Inferno attunement.")
        @ConfigEditorBoolean
        public boolean phaseDisplay = true;
    }

    public static final class Garden {
        @Expose
        @ConfigOption(name = "Yaw and Pitch", desc = "HUD showing your yaw, pitch and facing direction.")
        @ConfigEditorBoolean
        public boolean yawPitch = false;

        @Expose
        @ConfigOption(name = "Pest Cooldown", desc = "HUD counting down from the last pest spawn.")
        @ConfigEditorBoolean
        public boolean pestCooldown = true;

        @Expose
        @ConfigOption(name = "Pest Cooldown (seconds)", desc = "Your pest spawn cooldown in seconds.")
        @ConfigEditorSlider(minValue = 60, maxValue = 900, minStep = 5)
        public float pestCooldownSeconds = 300;

        @Expose
        @ConfigOption(name = "Blocks Per Second", desc = "HUD showing how many blocks per second you are breaking.")
        @ConfigEditorBoolean
        public boolean blocksPerSecond = true;

        @Expose
        @ConfigOption(name = "Special Drop Animation", desc = "Play an animation when you drop a farming dye or a Ray of Helios.")
        @ConfigEditorBoolean
        public boolean specialDropAnimation = true;
    }

    public static final class Fishing {
        @Expose
        @ConfigOption(name = "Stat Display", desc = "HUD with fishing speed, sea creature, trophy fish, double hook and treasure chance from your tab list stats.")
        @ConfigEditorBoolean
        public boolean statDisplay = true;

        @Expose
        @ConfigOption(name = "Hook Timer", desc = "HUD showing the time until your fish bites.")
        @ConfigEditorBoolean
        public boolean hookTimer = true;

        @Expose
        @ConfigOption(name = "Bait Display", desc = "HUD showing your bait and how much is left.")
        @ConfigEditorBoolean
        public boolean baitDisplay = true;

        @Expose
        @ConfigOption(name = "Rare Sea Creature Alert", desc = "Alert when you catch a rare sea or lava creature.")
        @ConfigEditorBoolean
        public boolean rareCreatureAlert = true;

        @Expose
        @ConfigOption(name = "Alert Rarity", desc = "Minimum sea creature rarity to alert for.")
        @ConfigEditorDropdown
        public CreatureRarity alertRarity = CreatureRarity.LEGENDARY;
    }

    public enum CreatureRarity {
        RARE, EPIC, LEGENDARY, MYTHIC;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public static final class MiningFeatures {
        @Expose
        @ConfigOption(name = "Commissions For Current Area Only", desc = "Only show the commission HUD in the Dwarven Mines, Crystal Hollows, Glacite Tunnels and Mineshafts.")
        @ConfigEditorBoolean
        public boolean commissionsAreaOnly = true;

        @Expose
        @ConfigOption(name = "Crystal Hollows Map", desc = "HUD map of the Crystal Hollows with your position and the structures you have found.")
        @ConfigEditorBoolean
        public boolean crystalHollowsMap = true;

        @Expose
        @ConfigOption(name = "Crystal Hollows Waypoints", desc = "Mark Mines of Divan, Jungle Temple, Goblin Queen's Den and other places when you find them, like Skyblocker. /sb crystalwaypoints add|share|remove|clear.")
        @ConfigEditorBoolean
        public boolean crystalWaypoints = true;

        @Expose
        @ConfigOption(name = "Waypoints From Chat", desc = "Turn Crystal Hollows coordinates in chat into waypoints.")
        @ConfigEditorBoolean
        public boolean crystalWaypointsFromChat = true;

        @Expose
        @ConfigOption(name = "Pickaxe Ability HUD", desc = "Cooldown of your pickaxe ability (Mining Speed Boost, Pickobulus, ...) while mining. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean pickaxeAbilityHud = true;

        @Expose
        @ConfigOption(name = "Pickaxe Ability Ready Alert", desc = "Show a title when your pickaxe ability is ready again.")
        @ConfigEditorBoolean
        public boolean pickaxeAbilityAlert = true;

        @Expose
        @ConfigOption(name = "Mines of Divan Tools Alert", desc = "Alert when you are holding all four scavenged tools for the Jade Crystal.")
        @ConfigEditorBoolean
        public boolean divanToolsAlert = true;

        @Expose
        @ConfigOption(name = "Mineshaft Timer", desc = "HUD with your time in the mineshaft and time until you freeze.")
        @ConfigEditorBoolean
        public boolean mineshaftTimer = true;

        @Expose
        @ConfigOption(name = "Pristine Record", desc = "Keep your highest pristine proc, overall and per gemstone, and alert on a new PB. /sb pristine to see them.")
        @ConfigEditorBoolean
        public boolean pristineRecord = true;
    }

    public static final class Foraging {
        @Expose
        @ConfigOption(name = "Sweep Display", desc = "HUD showing your Sweep stat and how many logs you will cut.")
        @ConfigEditorBoolean
        public boolean sweepDisplay = true;
    }

    public static final class Enchanting {
        @Expose
        @ConfigOption(name = "Chronomatron Solver", desc = "Highlight the Chronomatron pattern.")
        @ConfigEditorBoolean
        public boolean chronomatron = true;

        @Expose
        @ConfigOption(name = "Superpairs Solver", desc = "Show revealed Superpairs cards.")
        @ConfigEditorBoolean
        public boolean superpairs = true;

        @Expose
        @ConfigOption(name = "Ultrasequencer Solver", desc = "Highlight the Ultrasequencer order.")
        @ConfigEditorBoolean
        public boolean ultrasequencer = true;

        @Expose
        @ConfigOption(name = "Ultrasequencer Numbers", desc = "Show the click order as numbers on every Ultrasequencer slot, instead of only highlighting the next one.")
        @ConfigEditorBoolean
        public boolean ultrasequencerNumbers = true;
    }

    public static final class Runecrafting {
        @Expose
        @ConfigOption(name = "Valuable Rune Alert", desc = "Alert when you get a rune from the list below.")
        @ConfigEditorBoolean
        public boolean valuableRuneAlert = true;

        @Expose
        @ConfigOption(name = "Runes", desc = "Comma-separated rune names to alert for.")
        @ConfigEditorText
        public String runes = "Music, Enchant, Grand Searing, Rainbow, Spellbound, Grand Freezing, Primal Fear, Golden Carpet";
    }

    public static final class Dungeons {
        @Expose
        @Accordion
        @ConfigOption(name = "Platform Highlight (3x3)", desc = "One big box over the floor 7 3x3 platform (53-55, 63, 113-115), from when Goldor starts, like NoFrills.")
        public PlatformHighlight platformHighlight = new PlatformHighlight();

        @Expose
        @Accordion
        @ConfigOption(name = "Dungeon Map", desc = "Dungeon map HUD.")
        public DungeonMap map = new DungeonMap();

        @Expose
        @Accordion
        @ConfigOption(name = "Puzzle Solvers", desc = "Solutions for dungeon puzzles.")
        public Puzzles puzzles = new Puzzles();

        @Expose
        @Accordion
        @ConfigOption(name = "Secrets and Routes", desc = "Secret waypoints and your recorded routes.")
        public Secrets secrets = new Secrets();

        @Expose
        @Accordion
        @ConfigOption(name = "Terminals and Devices", desc = "Floor 7 terminal and device solvers.")
        public Terminals terminals = new Terminals();

        @Expose
        @Accordion
        @ConfigOption(name = "Mobs", desc = "Dungeon mob highlighting.")
        public DungeonMobs mobs = new DungeonMobs();

        @Expose
        @Accordion
        @ConfigOption(name = "Timers and Alerts", desc = "Splits, tick timers, mask timers and debuff alerts.")
        public Timers timers = new Timers();

        @Expose
        @Accordion
        @ConfigOption(name = "Score", desc = "270/300 score alerts and the score display.")
        public Score score = new Score();

        @Expose
        @Accordion
        @ConfigOption(name = "Leap Menu", desc = "Odin-style Spirit Leap menu with a box per teammate, coloured by class.")
        public LeapMenu leapMenu = new LeapMenu();

        @Expose
        @Accordion
        @ConfigOption(name = "Positional Messages", desc = "Party messages sent when you reach a spot (/sb posmsg), plus built-in waypoints like Py Stand Here.")
        public PositionalMessages positionalMessages = new PositionalMessages();

        @Expose
        @Accordion
        @ConfigOption(name = "Blood Camp", desc = "Watcher move prediction and blood mob kill timers.")
        public BloodCamp bloodCamp = new BloodCamp();
    }

    public static final class DungeonMap {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the dungeon map.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Fancy Map", desc = "Show player heads and room colours on the map.")
        @ConfigEditorBoolean
        public boolean fancy = true;

        @Expose
        @ConfigOption(name = "Room Labels", desc = "Show room names and secret counts on the map.")
        @ConfigEditorBoolean
        public boolean roomLabels = true;

        @Expose
        @ConfigOption(name = "Background", desc = "Skyblocker style: draw a blurred background behind the dungeon map.")
        @ConfigEditorBoolean
        public boolean background = false;

        @Expose
        @ConfigOption(name = "Map Style", desc = "NoammAddons (legit): rooms redrawn in clean colours with checkmarks, names and bordered heads, only showing what the vanilla map shows. Skyblocker: the vanilla map image.")
        @ConfigEditorDropdown
        public MapStyle style = MapStyle.NOAMM;

        @Expose
        @ConfigOption(name = "Checkmark Style", desc = "NoammAddons style: what to draw on each room.")
        @ConfigEditorDropdown
        public CheckmarkStyle checkmarkStyle = CheckmarkStyle.CHECKMARKS;

        @Expose
        @ConfigOption(name = "Center Checkmark", desc = "Put the checkmark/name in the middle of big rooms instead of the top-left square.")
        @ConfigEditorBoolean
        public boolean centerCheckmark = true;

        @Expose
        @ConfigOption(name = "Hide Unknown Room Checkmark", desc = "Don't draw the ? on rooms you haven't opened.")
        @ConfigEditorBoolean
        public boolean hideQuestionCheckmarks = false;

        @Expose
        @ConfigOption(name = "Limit Room Name Size", desc = "Shrink room names so they fit inside the room.")
        @ConfigEditorBoolean
        public boolean limitRoomNameSize = true;

        @Expose
        @ConfigOption(name = "Show Extra Info Under Map", desc = "Secrets, crypts, score, deaths, mimic and prince under the map.")
        @ConfigEditorBoolean
        public boolean extraInfo = false;

        @Expose
        @ConfigOption(name = "Show Player Names", desc = "Names under the player heads.")
        @ConfigEditorDropdown
        public PlayerNames playerNames = PlayerNames.HOLDING_LEAP;

        @Expose
        @ConfigOption(name = "Class Coloured Head Border", desc = "Colour each head's border by class instead of the Head Border colour.")
        @ConfigEditorBoolean
        public boolean classHeadBorder = false;

        @Expose
        @ConfigOption(name = "Class Coloured Names", desc = "Colour player names by class.")
        @ConfigEditorBoolean
        public boolean classNames = false;

        @Expose
        @ConfigOption(name = "Text Scale", desc = "Scale of room names and secret counts.")
        @ConfigEditorSlider(minValue = 0.4f, maxValue = 1.5f, minStep = 0.1f)
        public float textScale = 1f;

        @Expose
        @ConfigOption(name = "Checkmark Scale", desc = "Scale of the room checkmarks.")
        @ConfigEditorSlider(minValue = 0.3f, maxValue = 1.5f, minStep = 0.1f)
        public float checkmarkScale = 1f;

        @Expose
        @ConfigOption(name = "Player Head Scale", desc = "Scale of the player heads.")
        @ConfigEditorSlider(minValue = 0.3f, maxValue = 1.5f, minStep = 0.1f)
        public float headScale = 1f;

        @Expose
        @ConfigOption(name = "Player Name Scale", desc = "Scale of the player names.")
        @ConfigEditorSlider(minValue = 0.3f, maxValue = 1.5f, minStep = 0.1f)
        public float nameScale = 0.5f;

        @Expose
        @ConfigOption(name = "Border Thickness", desc = "Thickness of the map border.")
        @ConfigEditorSlider(minValue = 0, maxValue = 5, minStep = 1)
        public int borderWidth = 1;

        @Expose @ConfigOption(name = "Map Background Colour", desc = "") @ConfigEditorColour public String backgroundColor = "0:50:255:255:255";
        @Expose @ConfigOption(name = "Map Border Colour", desc = "") @ConfigEditorColour public String borderColor = "0:255:255:255:255";
        @Expose @ConfigOption(name = "Head Border Colour", desc = "") @ConfigEditorColour public String headBorderColor = "0:255:0:0:0";
        @Expose @ConfigOption(name = "Blood Room", desc = "") @ConfigEditorColour public String colorBlood = "0:255:178:0:0";
        @Expose @ConfigOption(name = "Entrance Room", desc = "") @ConfigEditorColour public String colorEntrance = "0:255:0:255:0";
        @Expose @ConfigOption(name = "Fairy Room", desc = "") @ConfigEditorColour public String colorFairy = "0:255:227:155:226";
        @Expose @ConfigOption(name = "Miniboss Room", desc = "") @ConfigEditorColour public String colorMiniboss = "0:255:255:200:0";
        @Expose @ConfigOption(name = "Normal Room", desc = "") @ConfigEditorColour public String colorRoom = "0:255:121:70:0";
        @Expose @ConfigOption(name = "Puzzle Room", desc = "") @ConfigEditorColour public String colorPuzzle = "0:255:123:0:123";
        @Expose @ConfigOption(name = "Rare Room", desc = "") @ConfigEditorColour public String colorRare = "0:255:178:178:178";
        @Expose @ConfigOption(name = "Trap Room", desc = "") @ConfigEditorColour public String colorTrap = "0:255:255:130:0";
        @Expose @ConfigOption(name = "Unopened Room", desc = "") @ConfigEditorColour public String colorUnopened = "0:255:65:65:65";
        @Expose @ConfigOption(name = "Blood Door", desc = "") @ConfigEditorColour public String colorBloodDoor = "0:255:178:0:0";
        @Expose @ConfigOption(name = "Entrance Door", desc = "") @ConfigEditorColour public String colorEntranceDoor = "0:255:0:255:0";
        @Expose @ConfigOption(name = "Normal Door", desc = "") @ConfigEditorColour public String colorRoomDoor = "0:255:121:70:0";
        @Expose @ConfigOption(name = "Wither Door", desc = "") @ConfigEditorColour public String colorWitherDoor = "0:255:16:16:16";
        @Expose @ConfigOption(name = "Opened Wither Door", desc = "") @ConfigEditorColour public String colorOpenWitherDoor = "0:255:121:70:0";
        @Expose @ConfigOption(name = "Unopened Door", desc = "") @ConfigEditorColour public String colorUnopenedDoor = "0:255:65:65:65";

        @Expose public int x = 2;
        @Expose public int y = 2;
        @Expose public float scale = 1f;
    }

    /** Ice Fill, Boulder, Creeper Beams, Three Weirdos, Quiz, Teleport Maze, Water Board and Blaze use Odin's solvers. */
    public static final class Puzzles {
        @Expose @ConfigOption(name = "Tic Tac Toe", desc = "Show the best move.") @ConfigEditorBoolean public boolean ticTacToe = true;
        @Expose @ConfigOption(name = "Silverfish", desc = "Show the path.") @ConfigEditorBoolean public boolean silverfish = true;
        @Expose @ConfigOption(name = "Three Weirdos", desc = "Odin: the chest with the reward in green, wrong chests in red.") @ConfigEditorBoolean public boolean threeWeirdos = true;
        @Expose @ConfigOption(name = "Creeper Beams", desc = "Odin: each pair of lanterns to connect in its own colour, with a line between them.") @ConfigEditorBoolean public boolean creeperBeams = true;
        @Expose @ConfigOption(name = "Water Board", desc = "Odin: when to flip each lever, with CLICK ME! and countdowns, and a line to the next lever.") @ConfigEditorBoolean public boolean waterBoard = true;
        @Expose @ConfigOption(name = "Water Board Optimized", desc = "Use Odin's faster Water Board solutions.") @ConfigEditorBoolean public boolean waterOptimized = false;
        @Expose @ConfigOption(name = "Water Board Path Preview", desc = "Skyblocker: show where the water will flow on the board right now.") @ConfigEditorBoolean public boolean waterPreviewPath = true;
        @Expose @ConfigOption(name = "Water Board Lever Preview", desc = "Skyblocker: while looking at a lever, show which blocks flipping it adds (green) and removes (red).") @ConfigEditorBoolean public boolean waterPreviewLevers = true;
        @Expose @ConfigOption(name = "Blaze", desc = "Odin: the next three blazes to shoot (green, orange, white) with lines between them.") @ConfigEditorBoolean public boolean blaze = true;
        @Expose @ConfigOption(name = "Show All Blazes", desc = "Also box every other blaze.") @ConfigEditorBoolean public boolean blazeShowAll = false;
        @Expose @ConfigOption(name = "Boulder", desc = "Odin: the boulder to push next. The box clears when you click its button.") @ConfigEditorBoolean public boolean boulder = true;
        @Expose @ConfigOption(name = "Show All Boulder Clicks", desc = "Show every boulder to push instead of only the next one.") @ConfigEditorBoolean public boolean boulderShowAll = false;
        @Expose @ConfigOption(name = "Ice Fill", desc = "Odin: the path over each Ice Fill floor.") @ConfigEditorBoolean public boolean iceFill = true;
        @Expose @ConfigOption(name = "Ice Fill Optimized Patterns", desc = "Use Odin's shorter (harder) Ice Fill paths.") @ConfigEditorBoolean public boolean iceFillOptimized = false;
        @Expose @ConfigOption(name = "Quiz", desc = "Odin: a box and beam on the right answer.") @ConfigEditorBoolean public boolean trivia = true;
        @Expose @ConfigOption(name = "Teleport Maze", desc = "Odin: visited pads in red, the right pad in green (orange while there are several), and a line to the best next pad.") @ConfigEditorBoolean public boolean teleportMaze = true;
    }

    public static final class Secrets {
        @Expose
        @ConfigOption(name = "Secret Waypoints", desc = "Show waypoints for each room's secrets.")
        @ConfigEditorBoolean
        public boolean secretWaypoints = true;

        @Expose
        @ConfigOption(name = "Show Routes", desc = "Show a secret route for the current room: yours if you recorded one, otherwise Stella's. Record with /sb route start and /sb route stop; share with /sb export; import a Stella (or SecretRoutes) export with /sb route import (clipboard) or /sb route import <file>.")
        @ConfigEditorBoolean
        public boolean routes = true;

        @Expose
        @ConfigOption(name = "Door Highlight", desc = "Outline wither and blood doors: green when your team has the key, red when locked.")
        @ConfigEditorBoolean
        public boolean doorHighlight = true;

        @Expose
        @ConfigOption(name = "Key Highlight", desc = "Outline dropped Wither and Blood keys, visible through walls.")
        @ConfigEditorBoolean
        public boolean keyHighlight = true;

        @Expose
        @ConfigOption(name = "Announce Key Spawn", desc = "Show a title when a Wither or Blood key spawns.")
        @ConfigEditorBoolean
        public boolean announceKeySpawn = true;
    }

    public static final class DungeonMobs {
        @Expose
        @ConfigOption(name = "Highlight Starred Mobs", desc = "Draw a box around starred (✯) dungeon mobs you can see. Hidden behind walls.")
        @ConfigEditorBoolean
        public boolean starredMobs = true;

        @Expose
        @ConfigOption(name = "Starred Mob Colour", desc = "Colour of the box around starred mobs.")
        @ConfigEditorColour
        public String starredColor = "0:255:255:217:51";

        @Expose
        @ConfigOption(name = "Fill Opacity", desc = "How see-through the fill inside the box is. 0 draws only the outline.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
        public float starredFill = 0f;

        @Expose
        @ConfigOption(name = "Line Width", desc = "Thickness of the box outline.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 0.5f)
        public float starredLineWidth = 2f;
    }

    public static final class Terminals {
        @Expose @ConfigOption(name = "Odin Terminal Solver", desc = "Odin's terminal solvers for all six terminals (panes, rubix, numbers, starts with, select, melody). Covers the terminal and shows only what to click. Off: Skyblocker's highlights below.") @ConfigEditorBoolean public boolean odinSolver = true;
        @Expose @ConfigOption(name = "Block Misclicks", desc = "In terminals, ignore clicks on slots that aren't part of the solution (and wrong-button rubix clicks).") @ConfigEditorBoolean public boolean blockMisclicks = true;
        @Expose @ConfigOption(name = "Client Prediction", desc = "Update the solution as soon as you click instead of waiting for the server.") @ConfigEditorBoolean public boolean clickPrediction = true;
        @Expose @ConfigOption(name = "Resolve Timeout (ms)", desc = "How long a predicted click waits for the server before the terminal is re-read.") @ConfigEditorSlider(minValue = 300, maxValue = 1200, minStep = 10) public int resolveTimeout = 600;
        @Expose @ConfigOption(name = "First Click Protection (ms)", desc = "Clicks this soon after a terminal opens are ignored (Odin recommends 500 minus your ping).") @ConfigEditorSlider(minValue = 350, maxValue = 800, minStep = 10) public int firstClickProt = 500;
        @Expose @ConfigOption(name = "Account For Server Lag", desc = "Also block clicks until the terminal has been open for the ticks below.") @ConfigEditorBoolean public boolean lagProtection = false;
        @Expose @ConfigOption(name = "Lag Protection Ticks", desc = "Server ticks (50ms each) before clicks go through.") @ConfigEditorSlider(minValue = 7, maxValue = 16, minStep = 1) public int lagProtectionTicks = 8;
        @Expose @ConfigOption(name = "Rubix Left Clicks Only", desc = "Only use left clicks for the rubix terminal instead of the fewest clicks.") @ConfigEditorBoolean public boolean rubixLeftClicksOnly = false;
        @Expose @ConfigOption(name = "Melody Solver", desc = "Show the melody solver.") @ConfigEditorBoolean public boolean melodySolver = true;
        @Expose @ConfigOption(name = "Odin Device Solvers", desc = "Odin's Simon Says, Arrow Align and Sharp Shooter (i4) solvers. Off: Skyblocker's.") @ConfigEditorBoolean public boolean odinDevices = true;
        @Expose @ConfigOption(name = "SS Block Wrong Clicks", desc = "Simon Says: ignore clicks on the wrong button. Hold shift to click anyway.") @ConfigEditorBoolean public boolean ssBlockWrong = true;
        @Expose @ConfigOption(name = "SS Skip Helper", desc = "Simon Says skip: count your start button clicks (shown above the button) and block clicks past the limit below, like Odin's Block Wrong on Start. Hold shift to click anyway.") @ConfigEditorBoolean public boolean ssLimitStartClicks = true;
        @Expose @ConfigOption(name = "SS Skip Start Clicks", desc = "How many start button clicks to allow for SS skip.") @ConfigEditorSlider(minValue = 1, maxValue = 10, minStep = 1) public int ssMaxStartClicks = 4;
        @Expose @ConfigOption(name = "SS Announce Progress", desc = "Send \"SS n/5\" to party chat when you click the last button of a round.") @ConfigEditorBoolean public boolean ssAnnounce = false;
        @Expose @ConfigOption(name = "SS First", desc = "") @ConfigEditorColour public String ssFirstColor = "0:128:85:255:85";
        @Expose @ConfigOption(name = "SS Second", desc = "") @ConfigEditorColour public String ssSecondColor = "0:128:255:170:0";
        @Expose @ConfigOption(name = "SS Rest", desc = "") @ConfigEditorColour public String ssThirdColor = "0:128:255:85:85";
        @Expose @ConfigOption(name = "Arrow Align Block Wrong Clicks", desc = "Arrow Align: ignore clicks on frames that are already right. Hold shift to click anyway.") @ConfigEditorBoolean public boolean arrowAlignBlockWrong = true;
        @Expose @ConfigOption(name = "i4 Aim Positions", desc = "Sharp Shooter: also show the three best places to aim to hit two blocks at once.") @ConfigEditorBoolean public boolean i4AimPositions = false;
        @Expose @ConfigOption(name = "i4 Complete Alert", desc = "Sharp Shooter: show a title when you complete the device.") @ConfigEditorBoolean public boolean i4CompleteAlert = true;
        @Expose @ConfigOption(name = "Background", desc = "") @ConfigEditorColour public String backgroundColor = "0:128:38:38:38";
        @Expose @ConfigOption(name = "Panes", desc = "") @ConfigEditorColour public String panesColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Rubix 1", desc = "") @ConfigEditorColour public String rubix1Color = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Rubix 2", desc = "") @ConfigEditorColour public String rubix2Color = "0:255:42:127:42";
        @Expose @ConfigOption(name = "Rubix -1", desc = "") @ConfigEditorColour public String rubixMinus1Color = "0:255:170:0:0";
        @Expose @ConfigOption(name = "Rubix -2", desc = "") @ConfigEditorColour public String rubixMinus2Color = "0:255:85:0:0";
        @Expose @ConfigOption(name = "Numbers 1", desc = "") @ConfigEditorColour public String numbers1Color = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Numbers 2", desc = "") @ConfigEditorColour public String numbers2Color = "0:255:42:127:42";
        @Expose @ConfigOption(name = "Numbers 3", desc = "") @ConfigEditorColour public String numbers3Color = "0:255:21:63:21";
        @Expose @ConfigOption(name = "Starts With", desc = "") @ConfigEditorColour public String startsWithColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Select", desc = "") @ConfigEditorColour public String selectColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Melody Column", desc = "") @ConfigEditorColour public String melodyColumnColor = "0:255:170:0:170";
        @Expose @ConfigOption(name = "Melody Pointer", desc = "") @ConfigEditorColour public String melodyPointerColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Color Terminal", desc = "Highlight the right panes.") @ConfigEditorBoolean public boolean color = true;
        @Expose @ConfigOption(name = "Order Terminal", desc = "Highlight the order.") @ConfigEditorBoolean public boolean order = true;
        @Expose @ConfigOption(name = "Starts With Terminal", desc = "Highlight the matching items.") @ConfigEditorBoolean public boolean startsWith = true;
        @Expose @ConfigOption(name = "Same Color Terminal", desc = "Show how many clicks each pane needs.") @ConfigEditorBoolean public boolean sameColor = true;
        @Expose @ConfigOption(name = "Simon Says", desc = "Highlight the buttons to press.") @ConfigEditorBoolean public boolean simonSays = true;
        @Expose @ConfigOption(name = "Lights On", desc = "Highlight the levers to flip.") @ConfigEditorBoolean public boolean lightsOn = true;
        @Expose @ConfigOption(name = "Arrow Align", desc = "Show how many clicks each frame needs.") @ConfigEditorBoolean public boolean arrowAlign = true;
        @Expose @ConfigOption(name = "Target Practice (i4)", desc = "Highlight the targets to shoot.") @ConfigEditorBoolean public boolean targetPractice = true;
    }

    public static final class Timers {
        @Expose
        @ConfigOption(name = "Splits", desc = "Odin-style split HUD (Blood Open, Blood Clear, Portal Entry, each boss phase, Total) with personal bests per floor and a chat message after each split.")
        @ConfigEditorBoolean
        public boolean splits = true;

        @Expose
        @ConfigOption(name = "Split Messages", desc = "Send \"<split> took <time>\" with your PB to chat when a split finishes.")
        @ConfigEditorBoolean
        public boolean splitMessages = true;

        @Expose
        @ConfigOption(name = "Boss Entry Split", desc = "Add a Boss Entry row (Blood Open + Blood Clear + Portal Entry) to the split HUD.")
        @ConfigEditorBoolean
        public boolean bossEntrySplit = true;

        @Expose
        @ConfigOption(name = "Show Tick Time", desc = "Show the split time counted in server ticks next to the real time.")
        @ConfigEditorBoolean
        public boolean splitTickTime = true;

        @Expose
        @ConfigOption(name = "Tick Timers", desc = "HUD for Storm's pillars (20 ticks) and Goldor's death tick (50 ticks by default).")
        @ConfigEditorBoolean
        public boolean tickTimers = true;

        @Expose
        @ConfigOption(name = "Goldor Tick Period", desc = "Server ticks between Goldor's death ticks.")
        @ConfigEditorSlider(minValue = 20, maxValue = 100, minStep = 1)
        public int goldorTickPeriod = 50;

        @Expose
        @ConfigOption(name = "Mask Timers", desc = "HUD with the Spirit Mask, Bonzo's Mask and Phoenix pet: invincibility time (gold), cooldown (red) or ready (green), counted in server ticks. Your worn mask is marked with a purple bar.")
        @ConfigEditorBoolean
        public boolean maskTimers = true;

        @Expose
        @ConfigOption(name = "Mask Proc Alert", desc = "Show a title and play a sound when a mask or Phoenix procs.")
        @ConfigEditorBoolean
        public boolean maskAlert = true;

        @Expose
        @ConfigOption(name = "Announce Mask Procs", desc = "Send \"<Mask> Procced! (n/3)\" to party chat when one of your masks or Phoenix procs.")
        @ConfigEditorBoolean
        public boolean maskAnnounce = false;

        @Expose
        @ConfigOption(name = "Max Debuff Alert", desc = "Alert when your own Last Breath shots (5), Ice Spray uses (1) and Lethality hits (5) reach the max debuff on an M7 dragon. Counts reset when a new dragon spawns.")
        @ConfigEditorBoolean
        public boolean debuffAlert = true;

        @Expose
        @ConfigOption(name = "Last Breath Release", desc = "Play a sound and show RELEASE once you have charged Last Breath for the set number of server ticks.")
        @ConfigEditorBoolean
        public boolean lastBreathRelease = true;

        @Expose
        @ConfigOption(name = "Last Breath Ticks", desc = "Server ticks of charging before the release cue.")
        @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
        public float lastBreathTicks = 5;

        @Expose
        @ConfigOption(name = "Last Breath Sound", desc = "Sound played with RELEASE.")
        @ConfigEditorDropdown
        public ReleaseSound lastBreathSound = ReleaseSound.BELL;

        @Expose
        @ConfigOption(name = "Last Breath Volume", desc = "Volume of the RELEASE sound.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 1f, minStep = 0.05f)
        public float lastBreathVolume = 1f;
    }

    public enum ReleaseSound {
        BELL("Bell"), NOTE_BELL("Note Block Bell"), DING("Ding"), ORB("XP Orb"), NONE("None");

        private final String label;

        ReleaseSound(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class Score {
        @Expose
        @ConfigOption(name = "270 Score Alert", desc = "Show a title, play a sound and print a chat message when the run reaches 270 score (S).")
        @ConfigEditorBoolean
        public boolean alert270 = true;

        @Expose
        @ConfigOption(name = "300 Score Alert", desc = "Show a title, play a sound and print a chat message with the run time when the run reaches 300 score (S+).")
        @ConfigEditorBoolean
        public boolean alert300 = true;

        @Expose
        @ConfigOption(name = "Send 270 to Party", desc = "Also send \"[SB] 270 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party270 = false;

        @Expose
        @ConfigOption(name = "Send 300 to Party", desc = "Also send \"[SB] 300 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party300 = false;

        @Expose
        @ConfigOption(name = "270 Message", desc = "Text for the 270 title and party message. [score] is replaced with the score. Party messages start with [SB].")
        @ConfigEditorText
        public String message270 = "270 Score Reached!";

        @Expose
        @ConfigOption(name = "300 Message", desc = "Text for the 300 title and party message. [score] is replaced with the score. Party messages start with [SB].")
        @ConfigEditorText
        public String message300 = "300 Score Reached!";

        @Expose
        @ConfigOption(name = "Score Display", desc = "NoammAddons' score HUD: the estimated score, coloured red below 270, yellow below 300 and green at 300.")
        @ConfigEditorBoolean
        public boolean display = true;

        @Expose
        @ConfigOption(name = "Detailed Score Display", desc = "Also show secrets, crypts, deaths and mimic/prince under the score, like the info under NoammAddons' map.")
        @ConfigEditorBoolean
        public boolean detailed = false;

        @Expose
        @ConfigOption(name = "Force Paul", desc = "Count Paul's +10 bonus score even when the EZPZ perk isn't detected.")
        @ConfigEditorBoolean
        public boolean forcePaul = false;
    }

    public enum ClassOverride {
        AUTO("Auto"), ARCHER("Archer"), BERSERK("Berserk"), HEALER("Healer"), MAGE("Mage"), TANK("Tank");

        private final String label;

        ClassOverride(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum MapStyle {
        NOAMM("NoammAddons (Legit)"), SKYBLOCKER("Skyblocker");

        private final String label;

        MapStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum CheckmarkStyle {
        CHECKMARKS("Checkmarks"), SECRETS("Secrets"), ROOM_NAME("Room Name"), ROOM_NAME_SECRETS("Room Name + Secrets");

        private final String label;

        CheckmarkStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum PlayerNames {
        OFF("Off"), HOLDING_LEAP("Holding Leap"), ALWAYS("Always");

        private final String label;

        PlayerNames(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum LeapCorner {
        TOP_LEFT("Top Left"), TOP_RIGHT("Top Right"), BOTTOM_LEFT("Bottom Left"), BOTTOM_RIGHT("Bottom Right");

        private final String label;

        LeapCorner(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Defaults follow Odin's leap menu: class colours and quadrants. */
    public static final class LeapMenu {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Replace the Spirit Leap / Infinileap menu with four large boxes. Click a box to leap to that teammate.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Colored Boxes", desc = "Fill each box with the class colour. Off: dark boxes with class-coloured names.")
        @ConfigEditorBoolean
        public boolean coloredBoxes = false;

        @Expose
        @ConfigOption(name = "Leap Announce", desc = "Send \"Leaped to <name>!\" to party chat after you leap.")
        @ConfigEditorBoolean
        public boolean announce = false;

        @Expose @ConfigOption(name = "Archer Colour", desc = "Archer box colour.") @ConfigEditorColour public String archerColor = "0:255:255:170:0";
        @Expose @ConfigOption(name = "Archer Position", desc = "Where the Archer goes. If two classes want the same corner, the extra one takes a free corner.") @ConfigEditorDropdown public LeapCorner archerCorner = LeapCorner.TOP_LEFT;
        @Expose @ConfigOption(name = "Berserk Colour", desc = "Berserk box colour.") @ConfigEditorColour public String berserkColor = "0:255:170:0:0";
        @Expose @ConfigOption(name = "Berserk Position", desc = "Where the Berserk goes.") @ConfigEditorDropdown public LeapCorner berserkCorner = LeapCorner.TOP_RIGHT;
        @Expose @ConfigOption(name = "Healer Colour", desc = "Healer box colour.") @ConfigEditorColour public String healerColor = "0:255:170:0:170";
        @Expose @ConfigOption(name = "Healer Position", desc = "Where the Healer goes.") @ConfigEditorDropdown public LeapCorner healerCorner = LeapCorner.BOTTOM_LEFT;
        @Expose @ConfigOption(name = "Mage Colour", desc = "Mage box colour.") @ConfigEditorColour public String mageColor = "0:255:85:170:255";
        @Expose @ConfigOption(name = "Mage Position", desc = "Where the Mage goes.") @ConfigEditorDropdown public LeapCorner mageCorner = LeapCorner.BOTTOM_RIGHT;
        @Expose @ConfigOption(name = "Tank Colour", desc = "Tank box colour.") @ConfigEditorColour public String tankColor = "0:255:0:170:0";
        @Expose @ConfigOption(name = "Tank Position", desc = "Where the Tank goes.") @ConfigEditorDropdown public LeapCorner tankCorner = LeapCorner.BOTTOM_RIGHT;
    }

    public static final class BloodCamp {
        @Expose
        @ConfigOption(name = "Move Prediction", desc = "Predict when the Watcher moves after its first spawns and show a Move Timer HUD.")
        @ConfigEditorBoolean
        public boolean movePrediction = true;

        @Expose
        @ConfigOption(name = "Move Message", desc = "Print \"Watcher will move in Xs.\" in chat.")
        @ConfigEditorBoolean
        public boolean moveMessage = true;

        @Expose
        @ConfigOption(name = "Party Move Message", desc = "Send \"Watcher will move in Xs.\" to party chat.")
        @ConfigEditorBoolean
        public boolean partyMoveMessage = false;

        @Expose
        @ConfigOption(name = "Kill Title", desc = "Show a \"Kill Mobs\" title when it is time to kill the first spawns.")
        @ConfigEditorBoolean
        public boolean killTitle = true;

        @Expose
        @ConfigOption(name = "Mob Kill Timers", desc = "Box where each blood mob will land, with a countdown until it spawns (green > 1.5s, gold, red, then aqua once spawned). Like Odin, only heads the Watcher throws are tracked, not the heads on the walls.")
        @ConfigEditorBoolean
        public boolean killTimers = true;

        @Expose
        @ConfigOption(name = "Spawn Colour", desc = "Box where the mob will land.")
        @ConfigEditorColour
        public String spawnColor = "0:255:255:85:85";

        @Expose
        @ConfigOption(name = "Final Colour", desc = "Box once the head has reached where the mob spawns.")
        @ConfigEditorColour
        public String finalColor = "0:255:0:170:170";

        @Expose
        @ConfigOption(name = "Position Colour", desc = "Box on the flying head (moved ahead by your ping).")
        @ConfigEditorColour
        public String positionColor = "0:255:85:255:85";

        @Expose
        @ConfigOption(name = "Box Size", desc = "Size of the boxes.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 1f, minStep = 0.1f)
        public float boxSize = 1f;

        @Expose
        @ConfigOption(name = "Line", desc = "Draw a line from the head to where it lands.")
        @ConfigEditorBoolean
        public boolean line = true;

        @Expose
        @ConfigOption(name = "Time Left", desc = "Show the time until the mob spawns.")
        @ConfigEditorBoolean
        public boolean timeLeft = true;

        @Expose
        @ConfigOption(name = "Offset (ms)", desc = "Shifts the countdown to match when mobs really spawn.")
        @ConfigEditorSlider(minValue = -100, maxValue = 100, minStep = 1)
        public int offset = 40;

        @Expose
        @ConfigOption(name = "Spawn Tick", desc = "Tick the mob is assumed to spawn on (mobs spawn 37-41 ticks after the throw).")
        @ConfigEditorSlider(minValue = 35, maxValue = 41, minStep = 1)
        public int tick = 38;

        @Expose
        @ConfigOption(name = "Interpolation", desc = "Smooth the landing box between ticks.")
        @ConfigEditorBoolean
        public boolean interpolation = true;

        @Expose
        @ConfigOption(name = "Ping Offset", desc = "Move the position box ahead by your ping.")
        @ConfigEditorBoolean
        public boolean pingOffset = true;

        @Expose
        @ConfigOption(name = "Watcher Bar", desc = "Show how many blood mobs are left in the Watcher's boss bar.")
        @ConfigEditorBoolean
        public boolean watcherBar = true;
    }

    public static final class PositionalMessages {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Send your positional messages to party chat when you reach them. Add them with /sb posmsg add here <radius> <delay ticks> <message>.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Only in Boss", desc = "Only send and show positional messages in a dungeon boss fight.")
        @ConfigEditorBoolean
        public boolean onlyInBoss = true;

        @Expose
        @ConfigOption(name = "Show Positions", desc = "Draw each positional message's circle or box and text in the world.")
        @ConfigEditorBoolean
        public boolean showPositions = true;

        @Expose
        @ConfigOption(name = "Ring Height", desc = "Height of the ring drawn around radius messages.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5f, minStep = 0.1f)
        public float ringHeight = 0.2f;

        @Expose
        @ConfigOption(name = "Show Message", desc = "Show each message's text above its spot.")
        @ConfigEditorBoolean
        public boolean showMessage = true;

        @Expose
        @ConfigOption(name = "Message Size", desc = "Size of the text above each spot.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 4f, minStep = 0.1f)
        public float messageSize = 1f;

        @Expose
        @ConfigOption(name = "Built-in Waypoints", desc = "Show SkyBalls's hard-coded waypoints on floor 7: Py Stand Here (95, 165.5, 94.4) in Storm when you are Mage, Mage Stop (34, 169, 65) in Storm when you are Mage, Arch Stand Here (102-104, 168, 49) in Storm when you are Archer, Tank Stand Here (109, 170, 93) in Storm when you are Tank, Healer Stand Here After Lighting (58, 169, 66) in Storm when you are Healer, and SS during Goldor (until Necron) when you are Healer: the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends \"At SS\" to party chat once.")
        @ConfigEditorBoolean
        public boolean builtInWaypoints = true;

        @Expose
        @ConfigOption(name = "Your Class", desc = "Which class's built-in waypoints to show. Auto reads it from the tab list; pick one if it isn't detected.")
        @ConfigEditorDropdown
        public ClassOverride classOverride = ClassOverride.AUTO;
    }

    public enum BoxStyle {
        OUTLINE("Outline"), FILLED("Filled"), BOTH("Outline and Fill");

        private final String label;

        BoxStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class PlatformHighlight {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Highlight the 3x3 platform on floor 7 once Goldor starts (after Storm).")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Healer Only", desc = "Only show it while you are Healer.")
        @ConfigEditorBoolean
        public boolean healerOnly = false;

        @Expose
        @ConfigOption(name = "Style", desc = "Outline, fill, or both.")
        @ConfigEditorDropdown
        public BoxStyle style = BoxStyle.OUTLINE;

        @Expose @ConfigOption(name = "Outline Colour", desc = "Colour of the outline.") @ConfigEditorColour public String outlineColor = "0:255:85:255:85";
        @Expose @ConfigOption(name = "Fill Colour", desc = "Colour of the fill.") @ConfigEditorColour public String fillColor = "0:127:85:255:85";
    }

    public static final class PartyCommands {
        @Expose
        @ConfigOption(name = "Enabled", desc = "When you are party leader, run commands that party members type in party chat.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose @ConfigOption(name = "!warp", desc = "Runs /party warp.") @ConfigEditorBoolean public boolean warp = true;
        @Expose @ConfigOption(name = "!allinvite", desc = "Runs /party settings allinvite.") @ConfigEditorBoolean public boolean allInvite = true;
        @Expose @ConfigOption(name = "!pt / !transfer", desc = "Transfers the party to the player who asked.") @ConfigEditorBoolean public boolean transfer = true;
        @Expose @ConfigOption(name = "!promote", desc = "Promotes the player who asked.") @ConfigEditorBoolean public boolean promote = false;
    }

    public static final class ItemNotification {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show items from your list on a HUD when you get them (in your sacks or your inventory), like the farming RNG HUD: amount, name and total price. Move it in /sb gui.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @ConfigOption(name = "Items", desc = "Open the list of items to watch for: one per line, with item name suggestions as you type. Also /sb itemnotify.")
        @io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton(buttonText = "EDIT")
        public Runnable editItems = com.epic60869.skyjew.features.misc.ItemNotification::openEditor;

        /** The items, one per line (edited in the Items window). */
        @Expose
        public String items = "";

        @Expose
        @ConfigOption(name = "Check Sacks", desc = "Watch the [Sacks] messages for items on the list.")
        @ConfigEditorBoolean
        public boolean checkSacks = true;

        @Expose
        @ConfigOption(name = "Check Inventory", desc = "Watch your inventory for items on the list.")
        @ConfigEditorBoolean
        public boolean checkInventory = true;

        @Expose
        @ConfigOption(name = "Show For (seconds)", desc = "How long an item stays on the HUD after you get it. Getting more of it keeps it there and adds to the amount.")
        @ConfigEditorSlider(minValue = 2, maxValue = 30, minStep = 1)
        public int seconds = 5;

        @Expose
        @ConfigOption(name = "Sound", desc = "Play a sound when an item on the list comes in.")
        @ConfigEditorBoolean
        public boolean sound = true;
    }

    public static final class AutoWelcome {
        public enum Destination {
            GUILD("Guild Chat"), MESSAGE("Private Message (/msg)");

            private final String label;

            Destination(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        @Expose
        @ConfigOption(name = "Enabled", desc = "Welcome the players on your list when they come online (from the \"Guild > Name joined.\" and \"Friend > Name joined.\" messages).")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Players", desc = "Names to welcome, separated by commas. You can also use /sb welcome add <name> and /sb welcome remove <name>.")
        @ConfigEditorText
        public String names = "";

        @Expose
        @ConfigOption(name = "Welcome In", desc = "Send the welcome in guild chat (/gc) or as a private message to the player (/msg).")
        @ConfigEditorDropdown
        public Destination destination = Destination.GUILD;

        @Expose
        @ConfigOption(name = "Message", desc = "What to send. {name} is replaced with the player's name.")
        @ConfigEditorText
        public String message = "Welcome back {name}!";

        @Expose
        @ConfigOption(name = "Welcome New Guild Members", desc = "Also welcome anyone who joins the guild, in guild chat.")
        @ConfigEditorBoolean
        public boolean welcomeNewMembers = false;

        @Expose
        @ConfigOption(name = "New Member Message", desc = "What to send when someone joins the guild. {name} is replaced with their name.")
        @ConfigEditorText
        public String newMemberMessage = "Welcome to the guild {name}!";

        @Expose
        @ConfigOption(name = "Cooldown (minutes)", desc = "Don't welcome the same player again within this many minutes, so relogging doesn't spam chat.")
        @ConfigEditorSlider(minValue = 0, maxValue = 120, minStep = 5)
        public int cooldownMinutes = 30;
    }
}
