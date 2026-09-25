package com.epic60869.skyjew.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
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
        @ConfigOption(name = "Arrow Counter", desc = "HUD showing the selected arrow type and how many arrows are left in your quiver.")
        @ConfigEditorBoolean
        public boolean arrowCounter = true;

        @Expose
        @ConfigOption(name = "Zealot Counter", desc = "HUD counting Zealots you kill in the End, kills since your last Summoning Eye, and eyes dropped.")
        @ConfigEditorBoolean
        public boolean zealotCounter = true;

        @Expose
        @ConfigOption(name = "Legion Display", desc = "HUD showing how many players are within Legion range (30 blocks).")
        @ConfigEditorBoolean
        public boolean legionDisplay = false;

        @Expose
        @Accordion
        @ConfigOption(name = "Cocoon Alert", desc = "Alert when you cocoon a mob.")
        public CocoonAlert cocoonAlert = new CocoonAlert();

        @Expose
        @Accordion
        @ConfigOption(name = "Rare Drops", desc = "Copy rare drops and animate big ones.")
        public RareDrops rareDrops = new RareDrops();
    }

    public static final class CocoonAlert {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show an on-screen alert when you cocoon a mob or your slayer boss.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Mobs", desc = "Comma-separated mob names to alert for, e.g. \"Minos Inquisitor, Slayer Boss\". Leave empty to alert for every cocoon.")
        @ConfigEditorText
        public String mobs = "";
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

        @Expose
        @ConfigOption(name = "Profit Tracker", desc = "HUD with slayer profit and profit per hour this session.")
        @ConfigEditorBoolean
        public boolean profitTracker = true;
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
    }

    public static final class Alchemy {
        @Expose
        @ConfigOption(name = "Alchemy 50 Estimate", desc = "HUD estimating how many potions you need to brew to reach Alchemy 50.")
        @ConfigEditorBoolean
        public boolean progressEstimate = false;

        @Expose
        @ConfigOption(name = "XP Per Potion", desc = "Alchemy XP you get per potion brewed with Enchanted Gold Blocks. Check it once in game and set it here.")
        @ConfigEditorSlider(minValue = 100, maxValue = 20000, minStep = 50)
        public float xpPerPotion = 2000;
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

    public static final class Hunting {
        @Expose
        @ConfigOption(name = "Safari Critter Tracker", desc = "HUD showing the critters you have captured this session in the Safari.")
        @ConfigEditorBoolean
        public boolean safariTracker = true;

        @Expose
        @ConfigOption(name = "Hunting Box Value", desc = "Show the total value of the shards in your Hunting Box.")
        @ConfigEditorBoolean
        public boolean huntingBoxValue = true;
    }

    public static final class Dungeons {
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
        @ConfigOption(name = "Timers and Alerts", desc = "Splits, tick timers, mask timers and debuff alerts.")
        public Timers timers = new Timers();
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

        @Expose public int x = 2;
        @Expose public int y = 2;
        @Expose public float scale = 1f;
    }

    public static final class Puzzles {
        @Expose @ConfigOption(name = "Tic Tac Toe", desc = "Show the best move.") @ConfigEditorBoolean public boolean ticTacToe = true;
        @Expose @ConfigOption(name = "Three Weirdos", desc = "Highlight the chest to open.") @ConfigEditorBoolean public boolean threeWeirdos = true;
        @Expose @ConfigOption(name = "Creeper Beams", desc = "Show which lanterns to connect.") @ConfigEditorBoolean public boolean creeperBeams = true;
        @Expose @ConfigOption(name = "Water Board", desc = "Show the lever order.") @ConfigEditorBoolean public boolean waterBoard = true;
        @Expose @ConfigOption(name = "Blaze", desc = "Highlight the next blaze to shoot.") @ConfigEditorBoolean public boolean blaze = true;
        @Expose @ConfigOption(name = "Boulder", desc = "Show which boulders to push.") @ConfigEditorBoolean public boolean boulder = true;
        @Expose @ConfigOption(name = "Ice Fill", desc = "Show the path.") @ConfigEditorBoolean public boolean iceFill = true;
        @Expose @ConfigOption(name = "Silverfish", desc = "Show the path.") @ConfigEditorBoolean public boolean silverfish = true;
        @Expose @ConfigOption(name = "Trivia", desc = "Highlight the right answer.") @ConfigEditorBoolean public boolean trivia = true;
        @Expose @ConfigOption(name = "Teleport Maze", desc = "Mark visited pads and the right one.") @ConfigEditorBoolean public boolean teleportMaze = true;
    }

    public static final class Secrets {
        @Expose
        @ConfigOption(name = "Secret Waypoints", desc = "Show waypoints for each room's secrets.")
        @ConfigEditorBoolean
        public boolean secretWaypoints = true;

        @Expose
        @ConfigOption(name = "Show Routes", desc = "Show your recorded route for the current room. Record with /sj route start, /sj route point, /sj route save.")
        @ConfigEditorBoolean
        public boolean routes = true;
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
        @ConfigOption(name = "Splits", desc = "HUD with dungeon split times and a run summary at the end.")
        @ConfigEditorBoolean
        public boolean splits = true;

        @Expose
        @ConfigOption(name = "Tick Timers", desc = "HUD for Storm's pillars (20 ticks) and Goldor's death tick (60 ticks).")
        @ConfigEditorBoolean
        public boolean tickTimers = true;

        @Expose
        @ConfigOption(name = "Mask Timers", desc = "HUD with your Bonzo mask, Spirit mask and Phoenix cooldowns, and an alert when they proc.")
        @ConfigEditorBoolean
        public boolean maskTimers = true;

        @Expose
        @ConfigOption(name = "Max Debuff Alert", desc = "Alert when your own Last Breath shots (5), Ice Spray uses (1) and Lethality hits (5) reach the max debuff on an M7 dragon. Counts reset when a new dragon spawns.")
        @ConfigEditorBoolean
        public boolean debuffAlert = true;
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
