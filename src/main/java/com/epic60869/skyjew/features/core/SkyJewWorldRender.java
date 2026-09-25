package com.epic60869.skyjew.features.core;

import com.epic60869.skyjew.sb.utils.render.primitive.GizmoPrimitiveCollector;
import com.epic60869.skyjew.sb.utils.render.primitive.PrimitiveCollector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-world boxes, lines and text. Renderers are called once per client tick inside
 * Minecraft's per-tick gizmo collection, the same mechanism vanilla's debug overlays use.
 */
public final class SkyJewWorldRender {
    private static final List<Consumer<PrimitiveCollector>> RENDERERS = new CopyOnWriteArrayList<>();
    private static final PrimitiveCollector COLLECTOR = new GizmoPrimitiveCollector();

    private SkyJewWorldRender() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(SkyJewWorldRender::tick);
    }

    public static void register(Consumer<PrimitiveCollector> renderer) {
        RENDERERS.add(renderer);
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || RENDERERS.isEmpty()) return;
        try (Gizmos.TemporaryCollection ignored = mc.collectPerTickGizmos()) {
            for (Consumer<PrimitiveCollector> renderer : RENDERERS) {
                try {
                    renderer.accept(COLLECTOR);
                } catch (Exception e) {
                    System.err.println("[SkyJew] World renderer failed: " + e);
                }
            }
        }
    }
}
