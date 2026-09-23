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
    private boolean firstBootScreenShown;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = SkyJewConfig.load(configDir.resolve("skyjew-mod.json"));
        SkyJewSounds.initialize();

        FarmingRngTracker.get().register();
        SkyJewRngHud.register(config);
        SkyJewCommandKeys.init(configDir);
        SkyJewStorageSearch.init(configDir);
        SkyJewCustom.init(configDir);
        SkyJewNopoFeatures.init(configDir);
        SkyJewNick.init(config);
        SkyJewMouseLock.init(config);
        SkyJewGlobalChat.init();
        SkyJewExperimentHelper.init(config);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        SkyJewVersionChecker.check(minecraft);
        registerCommands();

        System.out.println("[SkyJew] Core mod loaded.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("sj")
                .executes(context -> openMenu())
                .then(ClientCommands.literal("notes").executes(context -> openNotes()))
                .then(ClientCommands.literal("keys").executes(context -> openCommandKeys()))
                .then(ClientCommands.literal("search").executes(context -> openStorageSearch()))
                .then(ClientCommands.literal("chat")
                    .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendGlobalChat(StringArgumentType.getString(context, "message")))))
                .then(ClientCommands.literal("nick")
                    .executes(context -> openNick())
                    .then(ClientCommands.argument("value", StringArgumentType.greedyString())
                        .executes(context -> setNick(StringArgumentType.getString(context, "value")))))
                .then(ClientCommands.literal("discord")
                    .executes(context -> openDiscord()))
                .then(ClientCommands.literal("dm")
                    .then(ClientCommands.argument("user", StringArgumentType.word())
                        .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                            .executes(context -> sendDiscordDm(
                                StringArgumentType.getString(context, "user"),
                                StringArgumentType.getString(context, "message"))))))
                .then(customCommand())));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> customCommand() {
        var custom = ClientCommands.literal("custom")
            .executes(context -> openCustom());

        custom.then(ClientCommands.literal("renameItem")
            .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                .executes(context -> customRename(StringArgumentType.getString(context, "name")))));

        custom.then(ClientCommands.literal("dyeColor")
            .executes(context -> customDye(""))
            .then(ClientCommands.argument("hex", StringArgumentType.word())
                .executes(context -> customDye(StringArgumentType.getString(context, "hex")))));

        custom.then(ClientCommands.literal("armorTrim")
            .executes(context -> customTrim("", ""))
            .then(ClientCommands.argument("material", StringArgumentType.word())
                .then(ClientCommands.argument("pattern", StringArgumentType.word())
                    .executes(context -> customTrim(
                        StringArgumentType.getString(context, "material"),
                        StringArgumentType.getString(context, "pattern"))))));

        custom.then(ClientCommands.literal("animatedDye")
            .then(ClientCommands.argument("hex1", StringArgumentType.word())
                .then(ClientCommands.argument("hex2", StringArgumentType.word())
                    .then(ClientCommands.argument("duration", StringArgumentType.word())
                        .then(ClientCommands.argument("cycleBack", StringArgumentType.word())
                            .executes(context -> customAnimated(
                                StringArgumentType.getString(context, "hex1"),
                                StringArgumentType.getString(context, "hex2"),
                                StringArgumentType.getString(context, "duration"),
                                StringArgumentType.getString(context, "cycleBack"), "0"))
                            .then(ClientCommands.argument("delay", StringArgumentType.word())
                                .executes(context -> customAnimated(
                                    StringArgumentType.getString(context, "hex1"),
                                    StringArgumentType.getString(context, "hex2"),
                                    StringArgumentType.getString(context, "duration"),
                                    StringArgumentType.getString(context, "cycleBack"),
                                    StringArgumentType.getString(context, "delay")))))))));

        return custom;
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

    private int openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SkyJewCustom.open(mc, mc.gui.screen()));
        return 1;
    }

    private int customRename(String value) {
        Minecraft mc = Minecraft.getInstance();
        SkyJewCustom.setName(SkyJewCustom.held(mc), value);
        return 1;
    }

    private int customDye(String value) {
        Minecraft mc = Minecraft.getInstance();
        try {
            SkyJewCustom.setDye(SkyJewCustom.held(mc), value.isBlank() ? null : SkyJewCustom.parseHex(value));
        } catch (Exception ignored) {}
        return 1;
    }

    private int customTrim(String material, String pattern) {
        Minecraft mc = Minecraft.getInstance();
        SkyJewCustom.setTrim(SkyJewCustom.held(mc), material, pattern);
        return 1;
    }

    private int customAnimated(String a, String b, String duration, String cycleBack, String delay) {
        Minecraft mc = Minecraft.getInstance();
        try {
            SkyJewCustom.setAnimatedDye(SkyJewCustom.held(mc),
                SkyJewCustom.parseHex(a), SkyJewCustom.parseHex(b),
                Float.parseFloat(duration), Boolean.parseBoolean(cycleBack), Float.parseFloat(delay));
        } catch (Exception ignored) {}
        return 1;
    }

    private int sendGlobalChat(String message) {
        SkyJewGlobalChat.send(message);
        return 1;
    }

    private int openNick() {
        Minecraft.getInstance().execute(SkyJewConfig::openGui);
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

    private int sendDiscordDm(String user, String message) {
        SkyJewGlobalChat.sendDiscordDm(user, message);
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
        if (!config.general.firstBootAcknowledged && !firstBootScreenShown) {
            firstBootScreenShown = true;
            Screen currentScreen = minecraft.gui.screen();
            minecraft.execute(() -> minecraft.gui.setScreen(new SkyJewFirstBootScreen(config, currentScreen)));
            return;
        }
        SkyJewCommandKeys.tick(minecraft);
        SkyJewStorageSearch.tick(minecraft);
        SkyJewCustom.tick(minecraft);
        SkyJewNopoFeatures.tick(minecraft);
        SkyJewMouseLock.tick(minecraft);
        SkyJewGlobalChat.tick();
        SkyJewExperimentHelper.tick(minecraft);
        SkyJewFoxy.tick(minecraft);
    }
}
