package com.epic60869.skyjew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent in-game SkyJew notes. */
public final class SkyJewNotes {
    private static final String FILE_NAME = "skyjew-notes.txt";

    private SkyJewNotes() {}

    public static String load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) return "";
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyBalls] Failed to load notes: " + e.getMessage());
            return "";
        }
    }

    public static void save(Path configDir, String text) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            Files.writeString(file, text == null ? "" : text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyBalls] Failed to save notes: " + e.getMessage());
        }
    }
}
