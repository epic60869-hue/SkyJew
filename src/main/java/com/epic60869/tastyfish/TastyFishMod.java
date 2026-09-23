package com.epic60869.tastyfish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.nio.file.Path;

public final class TastyFishMod implements ClientModInitializer {

    private TastyFishConfig config;
    private boolean firstBootScreenShown;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Path configDir = minecraft.gameDirectory.toPath().resolve("config");
        config = TastyFishConfig.load(configDir.resolve("tastyfish-mod.json"));

        FarmingRngTracker.get().register();
        TastyFishRngHud.register(config);
        TastyFishCommandKeys.init(configDir);
        TastyFishStorageSearch.init(configDir);
        TastyFishCustom.init(configDir);
        TastyFishNopoFeatures.init(configDir);
        TastyFishMouseLock.init(config);
        TastyFishGlobalChat.init();
        TastyFishExperimentHelper.init(config);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        TastyFishVersionChecker.check(minecraft);
        registerCommands();

        System.out.println("[TastyFish] Core mod loaded.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("tf")
                .executes(context -> openMenu())
                .then(ClientCommands.literal("notes").executes(context -> openNotes()))
                .then(ClientCommands.literal("keys").executes(context -> openCommandKeys()))
                .then(ClientCommands.literal("search").executes(context -> openStorageSearch()))
                .then(ClientCommands.literal("chat")
                    .then(ClientCommands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendGlobalChat(StringArgumentType.getString(context, "message")))))
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
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishScreen(config)));
        return 1;
    }

    private int openCommandKeys() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishCommandKeysScreen(configDir)));
        return 1;
    }

    private int openCustom() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> TastyFishCustom.open(mc, mc.gui.screen()));
        return 1;
    }

    private int customRename(String value) {
        Minecraft mc = Minecraft.getInstance();
        TastyFishCustom.setName(TastyFishCustom.held(mc), value);
        return 1;
    }

    private int customDye(String value) {
        Minecraft mc = Minecraft.getInstance();
        try {
            TastyFishCustom.setDye(TastyFishCustom.held(mc), value.isBlank() ? null : TastyFishCustom.parseHex(value));
        } catch (Exception ignored) {}
        return 1;
    }

    private int customTrim(String material, String pattern) {
        Minecraft mc = Minecraft.getInstance();
        TastyFishCustom.setTrim(TastyFishCustom.held(mc), material, pattern);
        return 1;
    }

    private int customAnimated(String a, String b, String duration, String cycleBack, String delay) {
        Minecraft mc = Minecraft.getInstance();
        try {
            TastyFishCustom.setAnimatedDye(TastyFishCustom.held(mc),
                TastyFishCustom.parseHex(a), TastyFishCustom.parseHex(b),
                Float.parseFloat(duration), Boolean.parseBoolean(cycleBack), Float.parseFloat(delay));
        } catch (Exception ignored) {}
        return 1;
    }

    private int sendGlobalChat(String message) {
        TastyFishGlobalChat.send(message);
        return 1;
    }

    private int sendDiscordDm(String user, String message) {
        TastyFishGlobalChat.sendDiscordDm(user, message);
        return 1;
    }

    private int openStorageSearch() {
        Minecraft.getInstance().execute(() ->
            TastyFishStorageSearch.open(Minecraft.getInstance(), ""));
        return 1;
    }

    private int openNotes() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().gui.setScreen(new TastyFishNotesScreen(configDir)));
        return 1;
    }

    private void tick(Minecraft minecraft) {
        if (!config.firstBootAcknowledged && !firstBootScreenShown && minecraft.gui.screen() != null) {
            firstBootScreenShown = true;
            minecraft.gui.setScreen(new TastyFishFirstBootScreen(config, minecraft.gui.screen()));
            return;
        }
        TastyFishCommandKeys.tick(minecraft);
        TastyFishStorageSearch.tick(minecraft);
        TastyFishCustom.tick(minecraft);
        TastyFishNopoFeatures.tick(minecraft);
        TastyFishMouseLock.tick(minecraft);
        TastyFishGlobalChat.tick();
        TastyFishExperimentHelper.tick(minecraft);
        TastyFishFoxy.tick(minecraft);
    }
}
