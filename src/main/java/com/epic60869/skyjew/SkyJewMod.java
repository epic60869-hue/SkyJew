package com.epic60869.skyjew;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.nio.file.Path;

public final class SkyJewMod implements ClientModInitializer {

    private SkyJewConfig config;
    private boolean firstBootScreenShown;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = SkyJewConfig.load(configDir.resolve("skyjew-mod.json"));
        SkyJewSounds.initialize();
        // Register key mappings during client initialization, before GameOptions is initialized.
        SkyJewKeyMappings.init();
        SkyJewRecipeCommand.init();

        FarmingRngTracker.get().register();
        SkyJewRngHud.register(config);
        SkyJewCommissionHud.register(config);
        SkyJewCommandKeys.init(configDir);
        SkyJewStorageSearch.init(configDir);
        SkyJewCustom.init(configDir);
        SkyJewNopoFeatures.init(configDir);
        SkyJewNick.init(config);
        SkyJewMouseLock.init(config);
        SkyJewGlobalChat.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        SkyJewVersionChecker.check(minecraft);
        registerCommands();

        System.out.println("[SkyJew] Core mod loaded.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(commandTree("sj"));
            dispatcher.register(commandTree("skyjew"));
        });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> commandTree(String name) {
        var root = ClientCommands.literal(name)
            .executes(context -> openMenu())
            .then(ClientCommands.literal("notes").executes(context -> openNotes()))
            .then(ClientCommands.literal("keys").executes(context -> openCommandKeys()))
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
            .then(ClientCommands.literal("gui").executes(context -> openHudEditor()));

        return root;
    }


    private int openMenu() {
        Minecraft.getInstance().execute(SkyJewConfig::openGui);
        return 1;
    }

    private int openCommandKeys() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new SkyJewCommandKeysScreen(configDir)));
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
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new SkyJewNickScreen(mc.gui.screen())));
        return 1;
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
                    net.minecraft.network.chat.Component.literal("§6[SkyJew] §f" + expression + " §7= §a" + result)
                );
            }
        } catch (IllegalArgumentException e) {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c[SkyJew] Calc error: §f" + e.getMessage())
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
        if (!config.general.firstBootAcknowledged
            && !firstBootScreenShown
            && minecraft.gui.screen() instanceof TitleScreen) {
            firstBootScreenShown = true;
            Screen currentScreen = minecraft.gui.screen();
            minecraft.gui.setScreen(new SkyJewFirstBootScreen(config, currentScreen));
            return;
        }

        if (!config.general.firstBootAcknowledged
            && minecraft.gui.screen() instanceof SkyJewFirstBootScreen) {
            SkyJewSounds.tickFirstBoot();
            return;
        }

        while (SkyJewKeyMappings.SEARCH.consumeClick()) {
            openStorageSearch();
        }
        SkyJewCommandKeys.tick(minecraft);
        SkyJewStorageSearch.tick(minecraft);
        SkyJewNopoFeatures.tick(minecraft);
        SkyJewTabWidgetManager.tick(minecraft);
        if (minecraft.getConnection() != null) {
            for (var info : minecraft.getConnection().getOnlinePlayers()) {
                SkyJewNick.applyToTab(info);
            }
        }
        SkyJewMouseLock.tick(minecraft);
        SkyJewMouseReset.tick(minecraft);
        SkyJewGlobalChat.tick();
        SkyJewFoxy.tick(minecraft);
    }
}
