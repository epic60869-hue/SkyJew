package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in Command Keys style macro system for TastyFish.
 *
 * This is client-side and intentionally stores only the macro configuration.
 */
public final class TastyFishCommandKeys {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "tastyfish-command-keys.json";
    private static final Random RANDOM = new Random();

    public enum Mode { SEND, TYPE, CYCLE, RANDOM, REPEAT }

    public enum Conflict { SUBMIT, ASSERT, VETO, AVOID }

    public static final class Macro {
        public String name = "New Macro";
        public int keyCode = GLFW.GLFW_KEY_UNKNOWN;
        public int modifier = 0;
        public Mode mode = Mode.SEND;
        public int delayMs = 250;
        public Conflict conflict = Conflict.SUBMIT;
        public List<String> commands = new ArrayList<>(List.of("/help"));
        public int cycleIndex = 0;
        public boolean repeating = false;
        public long nextRepeatAt = 0L;
    }

    public static final class Profile {
        public String name = "Default";
        public String match = "";
        public List<Macro> macros = new ArrayList<>();
    }

    public static final class Data {
        public List<Profile> profiles = new ArrayList<>();
        public int editKeyCode = GLFW.GLFW_KEY_K;
    }

    private static Data data = new Data();
    private static Path configDir;
    private static boolean initialized;
    private static final java.util.Set<Integer> previousKeys = new java.util.HashSet<>();

    private TastyFishCommandKeys() {}

    public static void init(Path dir) {
        configDir = dir;
        load();
        initialized = true;
    }

    public static Data data() {
        return data;
    }

    public static void load() {
        if (configDir == null) return;
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(file)) {
                Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeToken<Data>() {}.getType());
                if (loaded != null) data = loaded;
            }
        } catch (Exception e) {
            System.err.println("[TastyFish] Failed to load command keys: " + e.getMessage());
        }
        normalize();
    }

    public static void save() {
        if (configDir == null) return;
        try {
            Files.createDirectories(configDir);
            Files.writeString(configDir.resolve(FILE_NAME), GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[TastyFish] Failed to save command keys: " + e.getMessage());
        }
    }

    private static void normalize() {
        if (data.profiles == null) data.profiles = new ArrayList<>();
        if (data.profiles.isEmpty()) data.profiles.add(new Profile());
        for (Profile p : data.profiles) {
            if (p.macros == null) p.macros = new ArrayList<>();
            for (Macro m : p.macros) {
                if (m.commands == null || m.commands.isEmpty()) m.commands = new ArrayList<>(List.of("/help"));
                if (m.mode == null) m.mode = Mode.SEND;
                if (m.conflict == null) m.conflict = Conflict.SUBMIT;
                if (m.delayMs < 0) m.delayMs = 0;
            }
        }
    }

    public static Profile activeProfile() {
        normalize();
        Minecraft mc = Minecraft.getInstance();
        String target = currentTarget(mc).toLowerCase(Locale.ROOT);
        Profile defaultProfile = data.profiles.get(0);
        for (Profile p : data.profiles) {
            if (p.match != null && !p.match.isBlank() &&
                target.contains(p.match.trim().toLowerCase(Locale.ROOT))) {
                return p;
            }
        }
        return defaultProfile;
    }

    private static String currentTarget(Minecraft mc) {
        try {
            if (mc.hasSingleplayerServer()) return "singleplayer";
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
                return mc.getCurrentServer().ip;
            }
        } catch (Throwable ignored) {}
        return "";
    }

    public static void tick(Minecraft mc) {
        if (!initialized || mc.player == null || mc.screen != null) return;

        long window = mc.getWindow().getWindow();
        Profile profile = activeProfile();

        for (Macro macro : profile.macros) {
            if (macro.mode == Mode.REPEAT && macro.repeating && System.currentTimeMillis() >= macro.nextRepeatAt) {
                run(macro, mc);
                macro.nextRepeatAt = System.currentTimeMillis() + Math.max(10, macro.delayMs);
            }
        }

        for (Macro macro : profile.macros) {
            if (macro.keyCode == GLFW.GLFW_KEY_UNKNOWN || !modifierDown(window, macro.modifier)) continue;
            boolean down = isDown(window, macro.keyCode);
            boolean wasDown = previousKeys.contains(macro.keyCode);
            if (down && !wasDown) {
                if (macro.mode == Mode.REPEAT) {
                    macro.repeating = !macro.repeating;
                    if (macro.repeating) {
                        run(macro, mc);
                        macro.nextRepeatAt = System.currentTimeMillis() + Math.max(10, macro.delayMs);
                    }
                } else {
                    run(macro, mc);
                }
            }
        }

        for (Macro macro : profile.macros) {
            if (isDown(window, macro.keyCode)) previousKeys.add(macro.keyCode);
            else previousKeys.remove(macro.keyCode);
        }

        if (data.editKeyCode != GLFW.GLFW_KEY_UNKNOWN && isDown(window, data.editKeyCode)
            && !previousKeys.contains(data.editKeyCode)) {
            mc.gui.setScreen(new TastyFishCommandKeysScreen(configDir));
            previousKeys.add(data.editKeyCode);
        }
    }

    private static boolean isDown(long window, int key) {
        return key != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    private static boolean modifierDown(long window, int modifier) {
        if (modifier == 0) return true;
        return switch (modifier) {
            case GLFW.GLFW_MOD_SHIFT -> isDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || isDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            case GLFW.GLFW_MOD_CONTROL -> isDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || isDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
            case GLFW.GLFW_MOD_ALT -> isDown(window, GLFW.GLFW_KEY_LEFT_ALT) || isDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
            case GLFW.GLFW_MOD_SUPER -> isDown(window, GLFW.GLFW_KEY_LEFT_SUPER) || isDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
            default -> true;
        };
    }

    private static void run(Macro macro, Minecraft mc) {
        if (macro.commands == null || macro.commands.isEmpty()) return;

        if (macro.mode == Mode.TYPE) {
            String text = expand(macro.commands.get(0), mc);
            mc.gui.setScreen(new ChatScreen(text));
            return;
        }

        List<String> selected = switch (macro.mode) {
            case CYCLE -> List.of(macro.commands.get(Math.floorMod(macro.cycleIndex++, macro.commands.size())));
            case RANDOM -> List.of(macro.commands.get(RANDOM.nextInt(macro.commands.size())));
            default -> macro.commands;
        };

        if (selected.size() == 1 && selected.get(0).contains(",,") && macro.mode == Mode.CYCLE) {
            selected = List.of(selected.get(0).replace(",,", "\u0000"));
        }

        long delay = 0;
        for (String raw : selected) {
            String command = expand(raw, mc).trim();
            if (command.isEmpty()) continue;
            final String toSend = command.startsWith("/") ? command.substring(1) : command;
            final long wait = delay;
            if (wait == 0) send(mc, toSend);
            else {
                Thread.startVirtualThread(() -> {
                    try { Thread.sleep(wait); } catch (InterruptedException ignored) { return; }
                    mc.execute(() -> send(mc, toSend));
                });
            }
            delay += Math.max(0, macro.delayMs);
        }
    }

    private static void send(Minecraft mc, String text) {
        if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
        else mc.player.connection.sendChat(text);
    }

    public static String expand(String input, Minecraft mc) {
        if (input == null) return "";
        String s = input.replace("%myname%", mc.getUser().getName());
        if (mc.player != null) {
            int x = (int)Math.floor(mc.player.getX());
            int y = (int)Math.floor(mc.player.getY());
            int z = (int)Math.floor(mc.player.getZ());
            s = s.replace("%x%", String.valueOf(x)).replace("%y%", String.valueOf(y)).replace("%z%", String.valueOf(z))
                .replace("%pos%", x + " " + y + " " + z);
            var hit = mc.hitResult;
            if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                s = s.replace("%lx%", String.valueOf(pos.getX()))
                    .replace("%ly%", String.valueOf(pos.getY()))
                    .replace("%lz%", String.valueOf(pos.getZ()))
                    .replace("%lpos%", pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
        }
        try {
            String clipboard = mc.keyboardHandler.getClipboard();
            s = s.replace("%clipboard%", clipboard == null ? "" : clipboard);
        } catch (Throwable ignored) {}
        return s;
    }

    public static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "Unbound";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            default -> "KEY " + key;
        };
    }
}
