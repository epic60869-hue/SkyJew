package com.epic60869.skyballs;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Carries settings over from before the rename: every config/skyjew... file and the config/skyjew folder are copied
 * to their config/skyballs... names, once, before anything reads them. The old files are left as a backup.
 */
public final class SkyBallsMigration implements PreLaunchEntrypoint {
    private static final String OLD = "skyjew";
    private static final String NEW = "skyballs";

    @Override
    public void onPreLaunch() {
        Path config = FabricLoader.getInstance().getConfigDir();
        try (Stream<Path> entries = Files.list(config)) {
            for (Path old : entries.toList()) {
                String name = old.getFileName().toString();
                if (!name.startsWith(OLD)) continue;
                Path target = config.resolve(NEW + name.substring(OLD.length()));
                if (Files.exists(target)) continue;
                copy(old, target);
            }
        } catch (IOException e) {
            System.err.println("[SkyBalls] Could not carry over old settings: " + e.getMessage());
        }
    }

    private static void copy(Path from, Path to) throws IOException {
        if (Files.isDirectory(from)) {
            try (Stream<Path> walk = Files.walk(from)) {
                for (Path source : walk.toList()) {
                    Path dest = to.resolve(from.relativize(source).toString());
                    if (Files.isDirectory(source)) Files.createDirectories(dest);
                    else Files.copy(source, dest);
                }
            }
        } else {
            Files.copy(from, to);
        }
    }
}
