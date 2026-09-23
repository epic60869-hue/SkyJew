package com.epic60869.skyjew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SkyJewConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean farmingRngEnabled = true;
    public boolean farmingRngBackground = false;
    public int farmingRngX = 8;
    public int farmingRngY = 8;
    public float farmingRngScale = 1.0f;
    public boolean mouseLockEnabled = false;
    public boolean mouseLockGroundOnly = true;
    public boolean experimentHelperEnabled = true;
    public boolean experimentHelperHighlight = true;
    public boolean experimentHelperPreventMisclicks = true;
    public boolean experimentHelperDebug = false;
    public boolean firstBootAcknowledged = false;\n    public boolean nickEnabled = false;\n    public String nickName = "";\n    public String nickMode = "plain";\n    public String nickColor = "";

    public static SkyJewConfig load(Path path) {
        try {
            if (Files.notExists(path)) {
                SkyJewConfig c = new SkyJewConfig();
                c.save(path);
                return c;
            }

            SkyJewConfig c = GSON.fromJson(
                Files.readString(path, StandardCharsets.UTF_8),
                SkyJewConfig.class
            );

            if (c == null) c = new SkyJewConfig();
            c.farmingRngScale = Math.max(0.5f, Math.min(3.0f, c.farmingRngScale));
            return c;
        } catch (Exception e) {
            System.err.println("[SkyJew] Failed to load config: " + e.getMessage());
            return new SkyJewConfig();
        }
    }

    public static void saveCurrent(SkyJewConfig config) {
        Path path = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("skyjew-mod.json");
        config.save(path);
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[SkyJew] Failed to save config: " + e.getMessage());
        }
    }
}
