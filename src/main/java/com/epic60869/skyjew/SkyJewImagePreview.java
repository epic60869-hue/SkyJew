package com.epic60869.skyjew;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Shows a preview of an image when hovering an image link in chat. */
public final class SkyJewImagePreview {
    private static final Pattern IMAGE_URL = Pattern.compile(
        "(?i)^https?://[^\\s]+\\.(?:png|jpe?g|gif|webp)(?:\\?[^\\s]*)?$"
    );
    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 180;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private record Loaded(Identifier id, int width, int height) {}

    private static final Map<String, Loaded> TEXTURES = new ConcurrentHashMap<>();
    /** PNG bytes downloaded off-thread, waiting to be uploaded as a texture on the render thread. */
    private static final Map<String, byte[]> READY = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> REQUESTED = new ConcurrentHashMap<>();

    private SkyJewImagePreview() {}

    public static void render(GuiGraphicsExtractor graphics, Style style, int mouseX, int mouseY) {
        String url = imageUrl(style);
        if (url == null) return;

        Loaded texture = texture(url);
        if (texture == null) return;

        float scale = Math.min((float) MAX_WIDTH / texture.width(), (float) MAX_HEIGHT / texture.height());
        int width = Math.max(1, Math.round(texture.width() * scale));
        int height = Math.max(1, Math.round(texture.height() * scale));
        int x = Math.max(6, Math.min(mouseX + 14, graphics.guiWidth() - width - 6));
        int y = Math.max(6, Math.min(mouseY - height - 10, graphics.guiHeight() - height - 6));

        graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, 0xF0101116);
        graphics.fill(x - 4, y - 4, x + width + 4, y - 2, 0xFF55FFFF);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), x, y, 0, 0, width, height, width, height, width, height);
    }

    private static String imageUrl(Style style) {
        if (style == null) return null;
        String url = null;
        if (style.getClickEvent() instanceof ClickEvent.OpenUrl open) {
            url = open.uri().toString();
        } else if (style.getHoverEvent() instanceof HoverEvent.ShowText showText) {
            url = showText.value().getString().trim();
        }
        return url != null && IMAGE_URL.matcher(url).matches() ? url : null;
    }

    private static Loaded texture(String url) {
        Loaded existing = TEXTURES.get(url);
        if (existing != null) return existing;

        byte[] png = READY.remove(url);
        if (png != null) {
            try {
                NativeImage image = NativeImage.read(png);
                Identifier id = Identifier.fromNamespaceAndPath("skyjew", "chat_preview/" + sha256(url));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
                Loaded loaded = new Loaded(id, image.getWidth(), image.getHeight());
                TEXTURES.put(url, loaded);
                return loaded;
            } catch (Exception e) {
                return null;
            }
        }

        if (REQUESTED.putIfAbsent(url, Boolean.TRUE) == null) {
            CompletableFuture.runAsync(() -> download(url));
        }
        return null;
    }

    private static void download(String url) {
        try {
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("skyjew").resolve("image-cache");
            Files.createDirectories(cacheDir);
            Path file = cacheDir.resolve(sha256(url) + ".png");

            if (!Files.exists(file)) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "SkyJew/1.0")
                    .GET().build();
                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
                // Minecraft can only read PNG, so JPG/GIF are converted (GIFs show their first frame).
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
                if (image == null) throw new IllegalStateException("Unsupported image format");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                Files.write(file, out.toByteArray());
            }
            READY.put(url, Files.readAllBytes(file));
        } catch (Exception e) {
            // Allow a retry the next time the link is hovered.
            REQUESTED.remove(url);
        }
    }

    private static String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }
}
