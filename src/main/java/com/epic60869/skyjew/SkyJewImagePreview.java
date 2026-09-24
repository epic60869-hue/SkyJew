package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class SkyJewImagePreview {
    private static final Pattern IMAGE_URL = Pattern.compile(
        "(?i)^https?://[^\\s]+\\.(?:png|jpe?g|gif|webp)(?:\\?[^\\s]*)?$"
    );
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> REQUESTED = new ConcurrentHashMap<>();

    private SkyJewImagePreview() {}

    public static void render(GuiGraphicsExtractor graphics, Style style, int mouseX, int mouseY) {
        if (style == null || style.getHoverEvent() == null) return;
        if (!(style.getHoverEvent() instanceof HoverEvent.ShowText showText)) return;

        String url = showText.value().getString().trim();
        if (!IMAGE_URL.matcher(url).matches()) return;

        Identifier texture = ensureTexture(url);
        if (texture == null) return;

        int previewWidth = 320;
        int previewHeight = 180;
        int x = Math.max(6, Math.min(mouseX + 14, graphics.guiWidth() - previewWidth - 6));
        int y = Math.max(6, Math.min(mouseY - previewHeight - 10, graphics.guiHeight() - previewHeight - 6));

        graphics.fill(x - 4, y - 4, x + previewWidth + 4, y + previewHeight + 4, 0xF0101116);
        graphics.fill(x - 4, y - 4, x + previewWidth + 4, y - 2, 0xFF55FFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0,
            previewWidth, previewHeight, previewWidth, previewHeight);
    }

    private static Identifier ensureTexture(String url) {
        Identifier existing = TEXTURES.get(url);
        if (existing != null) return existing;
        if (REQUESTED.putIfAbsent(url, Boolean.TRUE) != null) return null;

        try {
            Minecraft mc = Minecraft.getInstance();
            Path cacheDir = mc.gameDirectory.toPath().resolve("config").resolve("skyjew").resolve("image-cache");
            Files.createDirectories(cacheDir);

            String hash = sha256(url);
            File file = cacheDir.resolve(hash + ".img").toFile();
            Identifier id = Identifier.fromNamespaceAndPath("skyjew", "chat_preview/" + hash);

            NativeImage image = NativeImage.read(Files.newInputStream(file.toPath()));
            DynamicTexture texture = new DynamicTexture(() -> id.toString(), image);
            mc.getTextureManager().register(id, texture);
            TEXTURES.put(url, id);
            return id;
        } catch (Throwable ignored) {
            REQUESTED.remove(url);
            return null;
        }
    }

    private static String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }
}
