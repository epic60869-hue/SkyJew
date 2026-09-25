package com.epic60869.skyjew.commandkeys;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import com.epic60869.skyjew.commandkeys.config.Config;
import com.epic60869.skyjew.commandkeys.config.Macro;
import com.epic60869.skyjew.commandkeys.config.Message;
import com.epic60869.skyjew.commandkeys.config.Profile;

/**
 * One-time import of macros from SkyJew's previous command key system
 * ({@code config/skyjew-command-keys.json}) into the CommandKeys port.
 */
public final class SkyJewCommandKeysMigration {
    private static final String OLD_FILE_NAME = "skyjew-command-keys.json";

    private SkyJewCommandKeysMigration() {}

    public static void migrate(Path configDir) {
        Path oldFile = configDir.resolve(OLD_FILE_NAME);
        if (Files.notExists(oldFile)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(oldFile, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray profiles = root.has("profiles") ? root.getAsJsonArray("profiles") : new JsonArray();

            boolean first = true;
            for (JsonElement element : profiles) {
                JsonObject oldProfile = element.getAsJsonObject();
                Profile profile;
                if (first) {
                    // The first old profile merges into the active CommandKeys profile.
                    profile = Config.get().activeProfile();
                    first = false;
                } else {
                    profile = Config.get().addNewProfile();
                    profile.name = string(oldProfile, "name", "Profile");
                }
                String match = string(oldProfile, "match", "");
                if (!match.isBlank()) profile.forceAddLink(match);

                if (oldProfile.has("macros")) {
                    for (JsonElement macroElement : oldProfile.getAsJsonArray("macros")) {
                        importMacro(profile, macroElement.getAsJsonObject());
                    }
                }
            }

            Config.save();
            Files.move(oldFile, oldFile.resolveSibling(OLD_FILE_NAME + ".migrated"), StandardCopyOption.REPLACE_EXISTING);
            CommandKeys.LOG.info("Imported SkyJew command keys from {}", OLD_FILE_NAME);
        } catch (Exception e) {
            CommandKeys.LOG.error("Failed to import SkyJew command keys", e);
        }
    }

    private static void importMacro(Profile profile, JsonObject old) {
        Macro macro = new Macro();
        macro.name = string(old, "name", macro.name);
        profile.addMacro(macro);

        String mode = string(old, "mode", "SEND");
        if (mode.equals("RELEASE")) {
            profile.setActivationType(macro, Macro.ActivationType.RELEASE);
        } else {
            try {
                profile.setSendMode(macro, Macro.SendMode.valueOf(mode));
            } catch (IllegalArgumentException ignored) {}
        }
        try {
            profile.setConflictStrategy(macro, Macro.ConflictStrategy.valueOf(string(old, "conflict", "ASSERT")));
        } catch (IllegalArgumentException ignored) {}

        int keyCode = old.has("keyCode") ? old.get("keyCode").getAsInt() : GLFW.GLFW_KEY_UNKNOWN;
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            boolean mouse = old.has("mouseButton") && old.get("mouseButton").getAsBoolean();
            InputConstants.Key key = (mouse ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM).getOrCreate(keyCode);
            profile.setKey(macro, macro.getKeybind(), key);
        }

        // The old system stored a GLFW modifier bitmask; CommandKeys uses a limit key instead.
        int modifierKey = switch (old.has("modifier") ? old.get("modifier").getAsInt() : 0) {
            case GLFW.GLFW_MOD_SHIFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case GLFW.GLFW_MOD_CONTROL -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case GLFW.GLFW_MOD_ALT -> GLFW.GLFW_KEY_LEFT_ALT;
            case GLFW.GLFW_MOD_SUPER -> GLFW.GLFW_KEY_LEFT_SUPER;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
        if (modifierKey != GLFW.GLFW_KEY_UNKNOWN) {
            profile.setLimitKey(macro, macro.getKeybind(), InputConstants.Type.KEYSYM.getOrCreate(modifierKey));
        }

        while (!macro.getMessages().isEmpty()) macro.removeMessage(0);
        int delayTicks = Math.max(0, (old.has("delayMs") ? old.get("delayMs").getAsInt() : 0) / 50);
        if (old.has("commands")) {
            boolean firstMessage = true;
            for (JsonElement command : old.getAsJsonArray("commands")) {
                Message message = new Message();
                message.string = command.getAsString();
                message.delayTicks = firstMessage ? 0 : delayTicks;
                macro.addMessage(message);
                firstMessage = false;
            }
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }
}
