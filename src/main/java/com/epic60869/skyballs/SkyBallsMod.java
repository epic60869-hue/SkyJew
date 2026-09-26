package com.epic60869.skyballs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.nio.file.Path;

public final class SkyBallsMod implements ClientModInitializer {

    private SkyBallsConfig config;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = SkyBallsConfig.load(configDir.resolve("skyballs-mod.json"));
        // Register key mappings during client initialization, before GameOptions is initialized.
        SkyBallsKeyMappings.init();
        SkyBallsRecipeCommand.init();
        SkyBallsCraftHelper.init(configDir);

        FarmingRngTracker.get().register();
        SkyBallsRngHud.register(config);
        SkyBallsCommissionHud.register(config);
        // CommandKeys port: /sj keys. Key mappings must be registered during client init.
        com.epic60869.skyballs.commandkeys.CommandKeys.init();
        com.epic60869.skyballs.commandkeys.SkyBallsCommandKeysMigration.migrate(configDir);
        com.epic60869.skyballs.commandkeys.CommandKeys.getKeybinds().forEach(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper::registerKeyMapping);
        ClientCommandRegistrationCallback.EVENT.register(com.epic60869.skyballs.commandkeys.command.Commands::register);
        ClientTickEvents.END_CLIENT_TICK.register(com.epic60869.skyballs.commandkeys.CommandKeys::afterClientTick);
        SkyBallsStorageSearch.init(configDir);
        SkyBallsCustom.init(configDir);
        SkyBallsDateCalculator.init();

        // Shared infrastructure for the skill features.
        com.epic60869.skyballs.features.core.SkyBallsLocation.init();
        com.epic60869.skyballs.features.core.SkyBallsChat.init();
        com.epic60869.skyballs.features.core.SkyBallsAlerts.init();
        com.epic60869.skyballs.features.core.SkyBallsHuds.init(configDir);
        com.epic60869.skyballs.features.core.SkyBallsWorldRender.init();

        com.epic60869.skyballs.features.combat.CombatFeatures.init();
        com.epic60869.skyballs.features.combat.ZealotCounter.init(configDir);
        com.epic60869.skyballs.features.slayer.SlayerFeatures.init();
        com.epic60869.skyballs.features.garden.GardenFeatures.init();
        com.epic60869.skyballs.features.fishing.FishingFeatures.init();
        com.epic60869.skyballs.features.mining.MiningFeatures.init();
        com.epic60869.skyballs.features.mining.CrystalHollowsWaypoints.init();
        com.epic60869.skyballs.features.mining.PickaxeAbility.init();
        com.epic60869.skyballs.features.mining.PristineRecord.init(configDir);
        com.epic60869.skyballs.features.portfolio.Portfolio.init(configDir);
        com.epic60869.skyballs.features.skills.SkillFeatures.init();
        com.epic60869.skyballs.features.dungeons.SkyBallsDungeons.init();
        com.epic60869.skyballs.features.dungeons.DungeonFeatures.init(configDir);
        com.epic60869.skyballs.features.misc.PartyCommands.init();
        com.epic60869.skyballs.features.misc.AutoWelcome.init();
        com.epic60869.skyballs.features.misc.ItemNotification.init();
        com.epic60869.skyballs.features.misc.HeldItemModel.init(configDir);
        com.epic60869.skyballs.features.misc.ScrollableTooltips.init();
        com.epic60869.skyballs.features.misc.ToggleSprint.init();
        com.epic60869.skyballs.features.misc.WarpShortcuts.init();
        com.epic60869.skyballs.features.misc.PricePaid.init(configDir);
        com.epic60869.skyballs.features.misc.CollectionTracker.init(configDir);
        SkyBallsStaff.init();
        SkyBallsUpdateChecker.init();
        com.epic60869.skyballs.reports.Reports.init();
        SkyBallsToggleCommands.init();
        com.epic60869.skyballs.features.misc.CopyChat.init();
        com.epic60869.skyballs.features.misc.SlotLocking.init();
        com.epic60869.skyballs.features.misc.StorageOverlay.init();
        com.epic60869.skyballs.features.misc.ScreenshotShare.init();
        com.epic60869.skyballs.features.dungeons.CaseOpening.init();
        SkyBallsChangelog.init();
        SkyBallsNopoFeatures.init(configDir);
        SkyBallsNick.init(config);
        SkyBallsMouseLock.init(config);
        SkyBallsGlobalChat.init();
        SkyBallsCurrentChat.init(configDir);
        SkyBallsPriceTooltip.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        registerCommands();

        System.out.println("[SkyBalls] Core mod loaded.");
    }

    private void registerCommands() {
        // /chat sj enters the SkyBalls channel; switching to any other channel with /chat leaves it.
        // Done by intercepting the command rather than registering /chat, so Hypixel still gets /chat a, /chat g, ...
        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String lower = command.trim().toLowerCase(java.util.Locale.ROOT);
            if (lower.equals("chat sj") || lower.equals("chat skyballs")) {
                Minecraft.getInstance().execute(this::enterSkyBallsChat);
                return false;
            }
            if (lower.startsWith("chat ") && SkyBallsGlobalChat.isInSkyBallsChannel()) {
                SkyBallsGlobalChat.leaveSkyBallsChannel();
            }
            return true;
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /sb and /skyballs.
            for (String root : com.epic60869.skyballs.custom.util.Compat.COMMAND_ROOTS) dispatcher.register(commandTree(root));
            // /sbc: shortcut for /sb chat.
            for (String chat : new String[]{"sbc"}) {
                dispatcher.register(ClientCommands.literal(chat)
                    .executes(context -> enterSkyBallsChat())
                    .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendGlobalChat(StringArgumentType.getString(context, "message")))));
            }
        });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> commandTree(String name) {
        var root = ClientCommands.literal(name)
            .executes(context -> openMenu())
            .then(ClientCommands.literal("notes").executes(context -> openNotes()))
            .then(ClientCommands.literal("search").executes(context -> openStorageSearch()))
            .then(SkyBallsRecipeCommand.command())
            .then(ClientCommands.literal("calc")
                .then(ClientCommands.argument("calculation", StringArgumentType.greedyString())
                    .executes(context -> calculate(StringArgumentType.getString(context, "calculation")))))
            .then(ClientCommands.literal("chat")
                .executes(context -> enterSkyBallsChat())
                .then(ClientCommands.literal("leave").executes(context -> leaveSkyBallsChat()))
                .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> sendGlobalChat(StringArgumentType.getString(context, "message")))))
            .then(ClientCommands.literal("nick")
                .executes(context -> openNick())
                .then(ClientCommands.argument("value", StringArgumentType.greedyString())
                    .executes(context -> setNick(StringArgumentType.getString(context, "value")))))
            .then(ClientCommands.literal("gui").executes(context -> openHudEditor()))
            .then(ClientCommands.literal("debug").executes(context -> SkyBallsDebug.run()));

        return root;
    }


    private int openMenu() {
        Minecraft.getInstance().execute(SkyBallsConfig::openGui);
        return 1;
    }








    private int enterSkyBallsChat() {
        SkyBallsGlobalChat.enterSkyBallsChannel();
        return 1;
    }

    private int leaveSkyBallsChat() {
        SkyBallsGlobalChat.leaveSkyBallsChannel();
        return 1;
    }

    private int sendGlobalChat(String message) {
        if (message != null && message.trim().startsWith("!")) {
            SkyBallsGlobalChat.sendBotCommand(message);
        } else {
            SkyBallsGlobalChat.send(message);
        }
        return 1;
    }

    private int openNick() {
        return com.epic60869.skyballs.custom.util.Compat.queueOpenScreen(new SkyBallsNickScreen(null));
    }

    private int setNick(String value) {
        SkyBallsNick.set(value);
        return 1;
    }

    private int openDiscord() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsDiscordScreen(mc.gui.screen())));
        return 1;
    }

    private int openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyBallsHudEditorScreen(mc.gui.screen())));
        return 1;
    }

    private int sendDiscordDm(String user, String message) {
        SkyBallsGlobalChat.sendDiscordDm(user, message);
        return 1;
    }

    private int calculate(String expression) {
        Minecraft mc = Minecraft.getInstance();
        try {
            String result = SkyBallsCalculator.calculate(expression);
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                    net.minecraft.network.chat.Component.literal("§6[SB] §f" + expression + " §7= §a" + result)
                );
            }
        } catch (IllegalArgumentException e) {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c[SB] Calc error: §f" + e.getMessage())
                );
            }
        }
        return 1;
    }

    private int openStorageSearch() {
        Minecraft.getInstance().execute(() ->
            SkyBallsStorageSearch.open(Minecraft.getInstance(), ""));
        return 1;
    }

    private int openNotes() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new SkyBallsNotesScreen(configDir)));
        return 1;
    }

    private void tick(Minecraft minecraft) {
        while (SkyBallsKeyMappings.SEARCH.consumeClick()) {
            openStorageSearch();
        }
        SkyBallsStorageSearch.tick(minecraft);
        SkyBallsNopoFeatures.tick(minecraft);
        SkyBallsTabWidgetManager.tick(minecraft);
        SkyBallsMouseLock.tick(minecraft);
        SkyBallsMouseReset.tick(minecraft);
        SkyBallsGlobalChat.tick();
        SkyBallsFoxy.tick(minecraft);
    }
}
