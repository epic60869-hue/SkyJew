package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewWorldRender;
import com.epic60869.skyjew.sb.config.SkyblockerConfig;
import com.epic60869.skyjew.sb.config.SkyblockerConfigManager;
import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonMap;
import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonMapLabels;
import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonMapTexture;
import com.epic60869.skyjew.sb.skyblock.dungeon.DungeonScore;
import com.epic60869.skyjew.sb.skyblock.dungeon.device.ArrowAlign;
import com.epic60869.skyjew.sb.skyblock.dungeon.device.LightsOn;
import com.epic60869.skyjew.sb.skyblock.dungeon.device.SimonSays;
import com.epic60869.skyjew.sb.skyblock.dungeon.device.TargetPractice;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.CreeperBeams;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.DungeonBlaze;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.IceFill;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.IcePath;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.ThreeWeirdos;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.TicTacToe;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.Trivia;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.boulder.Boulder;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.waterboard.WaterboardOneFlow;
import com.epic60869.skyjew.sb.skyblock.dungeon.puzzle.waterboard.WaterboardPreviewer;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonManager;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonPlayerManager;
import com.epic60869.skyjew.sb.skyblock.waypoint.FairySouls;
import com.epic60869.skyjew.sb.utils.container.ContainerSolverManager;
import com.epic60869.skyjew.sb.utils.render.LevelRenderExtractionCallback;
import com.epic60869.skyjew.sb.utils.scheduler.MessageScheduler;
import com.epic60869.skyjew.sb.utils.scheduler.Scheduler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Starts SkyJew's port of Skyblocker's dungeon and experimentation features (map, puzzle
 * solvers, secret waypoints, terminals, devices, experiments) and keeps the port's config in
 * sync with SkyJew's own settings.
 */
public final class SkyJewDungeons {
    private static int ticks;

    private SkyJewDungeons() {}

    public static void init() {
        syncConfig();

        // Skyblocker runs these through its @Init annotation processor.
        FairySouls.init();
        com.epic60869.skyjew.sb.skyblock.waypoint.Waypoints.init();
        ContainerSolverManager.init();
        DungeonManager.init();
        DungeonPlayerManager.init();
        DungeonScore.init();
        ScoreCalculator.init();
        SelfClass.init();
        OdinTerminals.init();
        OdinDevices.init();
        DungeonMap.init();
        NoammMap.init();
        DungeonMapLabels.init();
        DungeonMapTexture.init();
        Boulder.init();
        CreeperBeams.init();
        DungeonBlaze.init();
        IceFill.init();
        IcePath.init();
        ThreeWeirdos.init();
        TicTacToe.init();
        Trivia.init();
        WaterboardOneFlow.init();
        WaterboardPreviewer.init();
        ArrowAlign.init();
        LightsOn.init();
        SimonSays.init();
        TargetPractice.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Scheduler.INSTANCE.tick();
            MessageScheduler.INSTANCE.tick();
            if (++ticks % 20 == 0) syncConfig();
        });
        SkyJewWorldRender.register(collector -> LevelRenderExtractionCallback.EVENT.invoker().onExtract(collector));
    }

    /** Copies SkyJew's toggles into the Skyblocker config sections the ported code reads. */
    public static void syncConfig() {
        SkyJewConfig skyjew = SkyJewConfig.current();
        if (skyjew == null) return;
        FeatureConfigs.Dungeons d = skyjew.dungeons;
        SkyblockerConfig sb = SkyblockerConfigManager.get();

        sb.dungeons.dungeonMap.enableMap = d.map.enabled;
        sb.dungeons.dungeonMap.fancyMap = d.map.fancy;
        sb.dungeons.dungeonMap.showRoomLabels = d.map.roomLabels;
        sb.dungeons.dungeonMap.backgroundBlur = d.map.background;
        int mapSize = Math.round(128 * d.map.scale);
        sb.dungeons.dungeonMap.mapX = com.epic60869.skyjew.features.core.SkyJewHuds.mapX(d.map.x, mapSize);
        sb.dungeons.dungeonMap.mapY = com.epic60869.skyjew.features.core.SkyJewHuds.mapY(d.map.y, mapSize);
        sb.dungeons.dungeonMap.mapScaling = d.map.scale;

        sb.dungeons.puzzleSolvers.solveTicTacToe = d.puzzles.ticTacToe;
        sb.dungeons.puzzleSolvers.solveThreeWeirdos = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.creeperSolver = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveWaterboard = false; // Odin solver in OdinPuzzleSolvers
        // Only Odin's Water Board solver draws the lever line; Skyblocker's one-flow solver drew a second one.
        sb.dungeons.puzzleSolvers.waterboardOneFlow = false;
        // Skyblocker's previews still run next to Odin's solver.
        sb.dungeons.puzzleSolvers.previewWaterPath = d.puzzles.waterPreviewPath;
        sb.dungeons.puzzleSolvers.previewLeverEffects = d.puzzles.waterPreviewLevers;
        sb.dungeons.puzzleSolvers.blazeSolver = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveBoulder = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveIceFill = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveSilverfish = d.puzzles.silverfish;
        sb.dungeons.puzzleSolvers.solveTrivia = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveTeleportMaze = false; // Odin solver in OdinPuzzleSolvers

        sb.dungeons.secretWaypoints.enableSecretWaypoints = d.secrets.secretWaypoints;
        // SkyJew's own door highlight replaces Skyblocker's blood-rush door box.
        sb.dungeons.doorHighlight.enableDoorHighlight = !d.secrets.doorHighlight;

        boolean odinTerms = d.terminals.odinSolver;
        sb.dungeons.terminals.solveColor = d.terminals.color && !odinTerms;
        sb.dungeons.terminals.solveOrder = d.terminals.order && !odinTerms;
        sb.dungeons.terminals.solveStartsWith = d.terminals.startsWith && !odinTerms;
        sb.dungeons.terminals.solveSameColor = d.terminals.sameColor && !odinTerms;
        boolean odinDevices = d.terminals.odinDevices;
        sb.dungeons.devices.solveSimonSays = d.terminals.simonSays && !odinDevices;
        sb.dungeons.devices.solveLightsOn = d.terminals.lightsOn;
        sb.dungeons.devices.solveArrowAlign = d.terminals.arrowAlign && !odinDevices;
        sb.dungeons.devices.solveTargetPractice = d.terminals.targetPractice && !odinDevices;

        sb.helpers.experiments.enableChronomatronSolver = skyjew.misc.experimentalTable.chronomatron;
        sb.helpers.experiments.enableSuperpairsSolver = skyjew.misc.experimentalTable.superpairs;
        sb.helpers.experiments.enableUltrasequencerSolver = skyjew.misc.experimentalTable.ultrasequencer;

        // Display only: never block clicks, and never post to party chat automatically.
        sb.dungeons.terminals.blockIncorrectClicks = false;
        sb.helpers.experiments.blockIncorrectClicks = false;
        sb.dungeons.mimicMessage.sendMimicMessage = false;
        sb.dungeons.princeMessage.sendPrinceMessage = false;
        sb.dungeons.batMessage.sendBatMessage = false;
        // Score alerts come from SkyJew's ScoreCalculator (NoammAddons' calculation) instead.
        sb.dungeons.dungeonScore.enableDungeonScore270Title = false;
        sb.dungeons.dungeonScore.enableDungeonScore270Sound = false;
        sb.dungeons.dungeonScore.enableDungeonScore270Message = false;
        sb.dungeons.dungeonScore.enableDungeonScore300Title = false;
        sb.dungeons.dungeonScore.enableDungeonScore300Sound = false;
        sb.dungeons.dungeonScore.enableDungeonScore300Message = false;
        sb.dungeons.dungeonScore.enableDungeonCryptsMessage = false;
    }
}
