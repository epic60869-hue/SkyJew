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
        @ConfigOption(name = "Mines of Divan Tools Alert", desc = "Alert when you are holding all four scavenged tools for the Jade Crystal.")
        @ConfigEditorBoolean
        public boolean divanToolsAlert = true;

        @Expose
        @ConfigOption(name = "Mineshaft Timer", desc = "HUD with your time in the mineshaft and time until you freeze.")
        @ConfigEditorBoolean
        public boolean mineshaftTimer = true;

        @Expose
        @ConfigOption(name = "Pristine Record", desc = "Keep your highest pristine proc, overall and per gemstone, and alert on a new PB. /sj pristine to see them.")
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
        @ConfigOption(name = "Positional Messages", desc = "Party messages sent when you reach a spot (/sj posmsg), plus built-in waypoints like Py Stand Here.")
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
        @ConfigOption(name = "Background", desc = "Draw a blurred background behind the dungeon map.")
        @ConfigEditorBoolean
        public boolean background = false;

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
        @ConfigOption(name = "Show Routes", desc = "Show a secret route for the current room: yours if you recorded one, otherwise Stella's. Record with /sj route start and /sj route stop; share with /sj export; import a Stella (or SecretRoutes) export with /sj route import (clipboard) or /sj route import <file>.")
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
    }

    public static final class Terminals {
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
        @ConfigOption(name = "Tick Timers", desc = "HUD for Storm's pillars (20 ticks) and Goldor's death tick (60 ticks).")
        @ConfigEditorBoolean
        public boolean tickTimers = true;

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
        @ConfigOption(name = "Send 270 to Party", desc = "Also send \"[SJ] 270 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party270 = false;

        @Expose
        @ConfigOption(name = "Send 300 to Party", desc = "Also send \"[SJ] 300 Score Reached!\" to party chat.")
        @ConfigEditorBoolean
        public boolean party300 = false;

        @Expose
        @ConfigOption(name = "270 Message", desc = "Text for the 270 title and party message. [score] is replaced with the score. Party messages start with [SJ].")
        @ConfigEditorText
        public String message270 = "270 Score Reached!";

        @Expose
        @ConfigOption(name = "300 Message", desc = "Text for the 300 title and party message. [score] is replaced with the score. Party messages start with [SJ].")
        @ConfigEditorText
        public String message300 = "300 Score Reached!";

        @Expose
        @ConfigOption(name = "Score Display", desc = "HUD with the estimated score, secrets, crypts, deaths and mimic/prince status. Hidden in boss.")
        @ConfigEditorBoolean
        public boolean display = true;
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
        @ConfigOption(name = "Mob Kill Timers", desc = "Box where each blood mob will land, with a countdown until it spawns (green > 1.5s, gold, red, then aqua once spawned).")
        @ConfigEditorBoolean
        public boolean killTimers = true;
    }

    public static final class PositionalMessages {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Send your positional messages to party chat when you reach them. Add them with /sj posmsg add here <radius> <delay ticks> <message>.")
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
        @ConfigOption(name = "Built-in Waypoints", desc = "Show SkyJew's hard-coded waypoints on floor 7: Py Stand Here (95, 165.5, 94.4) in Storm when you are Mage, Mage Stop (34, 169, 65) in Storm when you are Mage, Arch Stand Here (102-104, 168, 49) in Storm when you are Archer, Tank Stand Here (109, 170, 93) in Storm when you are Tank, Healer Stand Here After Lighting (58, 169, 66) in Storm when you are Healer, and SS during Goldor (until Necron) when you are Healer: the block at 109, 120, 93 is highlighted and standing at 108, 120, 93 sends \"At SS\" to party chat once.")
        @ConfigEditorBoolean
        public boolean builtInWaypoints = true;
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
}
