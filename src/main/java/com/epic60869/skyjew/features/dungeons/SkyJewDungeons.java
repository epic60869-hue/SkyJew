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
        ContainerSolverManager.init();
        DungeonManager.init();
        DungeonPlayerManager.init();
        DungeonScore.init();
        DungeonMap.init();
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
        sb.dungeons.dungeonMap.mapX = d.map.x;
        sb.dungeons.dungeonMap.mapY = d.map.y;
        sb.dungeons.dungeonMap.mapScaling = d.map.scale;

        sb.dungeons.puzzleSolvers.solveTicTacToe = d.puzzles.ticTacToe;
        sb.dungeons.puzzleSolvers.solveThreeWeirdos = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.creeperSolver = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveWaterboard = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.previewWaterPath = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.blazeSolver = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveBoulder = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveIceFill = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveSilverfish = d.puzzles.silverfish;
        sb.dungeons.puzzleSolvers.solveTrivia = false; // Odin solver in OdinPuzzleSolvers
        sb.dungeons.puzzleSolvers.solveTeleportMaze = false; // Odin solver in OdinPuzzleSolvers

        sb.dungeons.secretWaypoints.enableSecretWaypoints = d.secrets.secretWaypoints;
        // SkyJew's own door highlight replaces Skyblocker's blood-rush door box.
        sb.dungeons.doorHighlight.enableDoorHighlight = !d.secrets.doorHighlight;

        sb.dungeons.terminals.solveColor = d.terminals.color;
        sb.dungeons.terminals.solveOrder = d.terminals.order;
        sb.dungeons.terminals.solveStartsWith = d.terminals.startsWith;
        sb.dungeons.terminals.solveSameColor = d.terminals.sameColor;
        sb.dungeons.devices.solveSimonSays = d.terminals.simonSays;
        sb.dungeons.devices.solveLightsOn = d.terminals.lightsOn;
        sb.dungeons.devices.solveArrowAlign = d.terminals.arrowAlign;
        sb.dungeons.devices.solveTargetPractice = d.terminals.targetPractice;

        sb.helpers.experiments.enableChronomatronSolver = skyjew.misc.experimentalTable.chronomatron;
        sb.helpers.experiments.enableSuperpairsSolver = skyjew.misc.experimentalTable.superpairs;
        sb.helpers.experiments.enableUltrasequencerSolver = skyjew.misc.experimentalTable.ultrasequencer;

        // Display only: never block clicks, and never post to party chat automatically.
        sb.dungeons.terminals.blockIncorrectClicks = false;
        sb.helpers.experiments.blockIncorrectClicks = false;
        sb.dungeons.mimicMessage.sendMimicMessage = false;
        sb.dungeons.princeMessage.sendPrinceMessage = false;
        sb.dungeons.batMessage.sendBatMessage = false;
        FeatureConfigs.Score score = d.score;
        sb.dungeons.dungeonScore.enableDungeonScore270Title = score.alert270;
        sb.dungeons.dungeonScore.enableDungeonScore270Sound = score.alert270;
        sb.dungeons.dungeonScore.enableDungeonScore270Message = score.party270;
        sb.dungeons.dungeonScore.enableDungeonScore300Title = score.alert300;
        sb.dungeons.dungeonScore.enableDungeonScore300Sound = score.alert300;
        sb.dungeons.dungeonScore.enableDungeonScore300Message = score.party300;
        sb.dungeons.dungeonScore.dungeonScore270Message = score.message270.isBlank() ? "270 Score Reached!" : score.message270;
        sb.dungeons.dungeonScore.dungeonScore300Message = score.message300.isBlank() ? "300 Score Reached!" : score.message300;
        sb.dungeons.dungeonScore.enableDungeonCryptsMessage = false;
    }
}
