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

        // The old system stored GLFW key codes; Minecraft 26.3 uses SDL, so they are carried over by key name.
        int keyCode = old.has("keyCode") ? old.get("keyCode").getAsInt() : -1;
        if (keyCode != -1) {
            boolean mouse = old.has("mouseButton") && old.get("mouseButton").getAsBoolean();
            InputConstants.Key key = mouse ? glfwMouseButton(keyCode) : glfwKey(keyCode);
            if (key != null) profile.setKey(macro, macro.getKeybind(), key);
        }

        // The old system stored a GLFW modifier bitmask; CommandKeys uses a limit key instead.
        InputConstants.Key modifierKey = switch (old.has("modifier") ? old.get("modifier").getAsInt() : 0) {
            case 0x0001 -> InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LSHIFT);
            case 0x0002 -> InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LCONTROL);
            case 0x0004 -> InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LALT);
            case 0x0008 -> InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LGUI);
            default -> null;
        };
        if (modifierKey != null) {
            profile.setLimitKey(macro, macro.getKeybind(), modifierKey);
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

    /** A GLFW mouse button (0 left, 1 right, 2 middle, 3+ side) as a key, by name. */
    private static InputConstants.Key glfwMouseButton(int button) {
        String name = switch (button) {
            case 0 -> "key.mouse.left";
            case 1 -> "key.mouse.right";
            case 2 -> "key.mouse.middle";
            default -> "key.mouse." + (button + 1);
        };
        return keyByName(name);
    }

    /** A GLFW key code from the old SkyJew macro format, as a key, by name. */
    private static InputConstants.Key glfwKey(int code) {
        String name;
        if (code >= 65 && code <= 90) name = "key.keyboard." + (char) ('a' + code - 65);
        else if (code >= 48 && code <= 57) name = "key.keyboard." + (char) code;
        else if (code >= 290 && code <= 314) name = "key.keyboard.f" + (code - 289);
        else if (code >= 320 && code <= 329) name = "key.keyboard.keypad." + (code - 320);
        else name = switch (code) {
            case 32 -> "key.keyboard.space";
            case 39 -> "key.keyboard.apostrophe";
            case 44 -> "key.keyboard.comma";
            case 45 -> "key.keyboard.minus";
            case 46 -> "key.keyboard.period";
            case 47 -> "key.keyboard.slash";
            case 59 -> "key.keyboard.semicolon";
            case 61 -> "key.keyboard.equal";
            case 91 -> "key.keyboard.left.bracket";
            case 92 -> "key.keyboard.backslash";
            case 93 -> "key.keyboard.right.bracket";
            case 96 -> "key.keyboard.grave.accent";
            case 257 -> "key.keyboard.enter";
            case 258 -> "key.keyboard.tab";
            case 259 -> "key.keyboard.backspace";
            case 260 -> "key.keyboard.insert";
            case 261 -> "key.keyboard.delete";
            case 262 -> "key.keyboard.right";
            case 263 -> "key.keyboard.left";
            case 264 -> "key.keyboard.down";
            case 265 -> "key.keyboard.up";
            case 266 -> "key.keyboard.page.up";
            case 267 -> "key.keyboard.page.down";
            case 268 -> "key.keyboard.home";
            case 269 -> "key.keyboard.end";
            case 280 -> "key.keyboard.caps.lock";
            case 340 -> "key.keyboard.left.shift";
            case 341 -> "key.keyboard.left.control";
            case 342 -> "key.keyboard.left.alt";
            case 343 -> "key.keyboard.left.win";
            case 344 -> "key.keyboard.right.shift";
            case 345 -> "key.keyboard.right.control";
            case 346 -> "key.keyboard.right.alt";
            case 347 -> "key.keyboard.right.win";
            default -> null;
        };
        return name == null ? null : keyByName(name);
    }

    private static InputConstants.Key keyByName(String name) {
        try {
            return InputConstants.getKey(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
