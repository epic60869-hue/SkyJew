package com.epic60869.skyjew;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.nio.file.Path;

public final class SkyJewMod implements ClientModInitializer {

    private SkyJewConfig config;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = SkyJewConfig.load(configDir.resolve("skyjew-mod.json"));
        // Register key mappings during client initialization, before GameOptions is initialized.
        SkyJewKeyMappings.init();
        SkyJewRecipeCommand.init();
        SkyJewCraftHelper.init(configDir);

        FarmingRngTracker.get().register();
        SkyJewRngHud.register(config);
        SkyJewCommissionHud.register(config);
        // CommandKeys port: /sj keys. Key mappings must be registered during client init.
        com.epic60869.skyjew.commandkeys.CommandKeys.init();
        com.epic60869.skyjew.commandkeys.SkyJewCommandKeysMigration.migrate(configDir);
        com.epic60869.skyjew.commandkeys.CommandKeys.getKeybinds().forEach(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper::registerKeyMapping);
        ClientCommandRegistrationCallback.EVENT.register(com.epic60869.skyjew.commandkeys.command.Commands::register);
        ClientTickEvents.END_CLIENT_TICK.register(com.epic60869.skyjew.commandkeys.CommandKeys::afterClientTick);
        SkyJewStorageSearch.init(configDir);
        SkyJewCustom.init(configDir);
        SkyJewDateCalculator.init();

        // Shared infrastructure for the skill features.
        com.epic60869.skyjew.features.core.SkyJewLocation.init();
        com.epic60869.skyjew.features.core.SkyJewChat.init();
        com.epic60869.skyjew.features.core.SkyJewAlerts.init();
        com.epic60869.skyjew.features.core.SkyJewHuds.init(configDir);
        com.epic60869.skyjew.features.core.SkyJewWorldRender.init();

        com.epic60869.skyjew.features.combat.CombatFeatures.init();
        com.epic60869.skyjew.features.combat.ZealotCounter.init(configDir);
        com.epic60869.skyjew.features.slayer.SlayerFeatures.init();
        com.epic60869.skyjew.features.garden.GardenFeatures.init();
        com.epic60869.skyjew.features.fishing.FishingFeatures.init();
        com.epic60869.skyjew.features.mining.MiningFeatures.init();
        com.epic60869.skyjew.features.mining.CrystalHollowsWaypoints.init();
        com.epic60869.skyjew.features.mining.PickaxeAbility.init();
        com.epic60869.skyjew.features.mining.PristineRecord.init(configDir);
        com.epic60869.skyjew.features.portfolio.Portfolio.init(configDir);
        com.epic60869.skyjew.features.skills.SkillFeatures.init();
        com.epic60869.skyjew.features.dungeons.SkyJewDungeons.init();
        com.epic60869.skyjew.features.dungeons.DungeonFeatures.init(configDir);
        com.epic60869.skyjew.features.misc.PartyCommands.init();
        com.epic60869.skyjew.features.misc.AutoWelcome.init();
        com.epic60869.skyjew.features.misc.ItemNotification.init();
        com.epic60869.skyjew.features.misc.HeldItemModel.init(configDir);
        com.epic60869.skyjew.features.misc.ScrollableTooltips.init();
        com.epic60869.skyjew.features.misc.ToggleSprint.init();
        com.epic60869.skyjew.features.misc.WarpShortcuts.init();
        com.epic60869.skyjew.features.misc.PricePaid.init(configDir);
        com.epic60869.skyjew.features.misc.CollectionTracker.init(configDir);
        SkyJewStaff.init();
        SkyJewUpdateChecker.init();
        com.epic60869.skyjew.reports.Reports.init();
        SkyJewToggleCommands.init();
        com.epic60869.skyjew.features.misc.CopyChat.init();
        com.epic60869.skyjew.features.misc.SlotLocking.init();
        com.epic60869.skyjew.features.dungeons.CaseOpening.init();
        SkyJewChangelog.init();
        SkyJewNopoFeatures.init(configDir);
        SkyJewNick.init(config);
        SkyJewMouseLock.init(config);
        SkyJewGlobalChat.init();
        SkyJewCurrentChat.init(configDir);
        SkyJewPriceTooltip.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        registerCommands();

        System.out.println("[SkyBalls] Core mod loaded.");
    }

    private void registerCommands() {
        // /chat sj enters the SkyJew channel; switching to any other channel with /chat leaves it.
        // Done by intercepting the command rather than registering /chat, so Hypixel still gets /chat a, /chat g, ...
        net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            String lower = command.trim().toLowerCase(java.util.Locale.ROOT);
            if (lower.equals("chat sj") || lower.equals("chat skyjew")) {
                Minecraft.getInstance().execute(this::enterSkyJewChat);
                return false;
            }
            if (lower.startsWith("chat ") && SkyJewGlobalChat.isInSkyJewChannel()) {
                SkyJewGlobalChat.leaveSkyJewChannel();
            }
            return true;
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /sb and /skyballs; /sj and /skyjew from before the rename still work.
            for (String root : com.epic60869.skyjew.custom.util.Compat.COMMAND_ROOTS) dispatcher.register(commandTree(root));
            // /sbc (and the old /sjc): shortcut for /sb chat.
            for (String chat : new String[]{"sbc", "sjc"}) {
                dispatcher.register(ClientCommands.literal(chat)
                    .executes(context -> enterSkyJewChat())
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
            .then(SkyJewRecipeCommand.command())
            .then(ClientCommands.literal("calc")
                .then(ClientCommands.argument("calculation", StringArgumentType.greedyString())
                    .executes(context -> calculate(StringArgumentType.getString(context, "calculation")))))
            .then(ClientCommands.literal("chat")
                .executes(context -> enterSkyJewChat())
                .then(ClientCommands.literal("leave").executes(context -> leaveSkyJewChat()))
                .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> sendGlobalChat(StringArgumentType.getString(context, "message")))))
            .then(ClientCommands.literal("nick")
                .executes(context -> openNick())
                .then(ClientCommands.argument("value", StringArgumentType.greedyString())
                    .executes(context -> setNick(StringArgumentType.getString(context, "value")))))
            .then(ClientCommands.literal("gui").executes(context -> openHudEditor()))
            .then(ClientCommands.literal("debug").executes(context -> SkyJewDebug.run()));

        return root;
    }


    private int openMenu() {
        Minecraft.getInstance().execute(SkyJewConfig::openGui);
        return 1;
    }








    private int enterSkyJewChat() {
        SkyJewGlobalChat.enterSkyJewChannel();
        return 1;
    }

    private int leaveSkyJewChat() {
        SkyJewGlobalChat.leaveSkyJewChannel();
        return 1;
    }

    private int sendGlobalChat(String message) {
        if (message != null && message.trim().startsWith("!")) {
            SkyJewGlobalChat.sendBotCommand(message);
        } else {
            SkyJewGlobalChat.send(message);
        }
        return 1;
    }

    private int openNick() {
        return com.epic60869.skyjew.custom.util.Compat.queueOpenScreen(new SkyJewNickScreen(null));
    }

    private int setNick(String value) {
        SkyJewNick.set(value);
        return 1;
    }

    private int openDiscord() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewDiscordScreen(mc.gui.screen())));
        return 1;
    }

    private int openHudEditor() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewHudEditorScreen(mc.gui.screen())));
        return 1;
    }

    private int sendDiscordDm(String user, String message) {
        SkyJewGlobalChat.sendDiscordDm(user, message);
        return 1;
    }

    private int calculate(String expression) {
        Minecraft mc = Minecraft.getInstance();
        try {
            String result = SkyJewCalculator.calculate(expression);
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
            SkyJewStorageSearch.open(Minecraft.getInstance(), ""));
        return 1;
    }

    private int openNotes() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new SkyJewNotesScreen(configDir)));
        return 1;
    }

    private void tick(Minecraft minecraft) {
        while (SkyJewKeyMappings.SEARCH.consumeClick()) {
            openStorageSearch();
        }
        SkyJewStorageSearch.tick(minecraft);
        SkyJewNopoFeatures.tick(minecraft);
        SkyJewTabWidgetManager.tick(minecraft);
        SkyJewMouseLock.tick(minecraft);
        SkyJewMouseReset.tick(minecraft);
        SkyJewGlobalChat.tick();
        SkyJewFoxy.tick(minecraft);
    }
}
