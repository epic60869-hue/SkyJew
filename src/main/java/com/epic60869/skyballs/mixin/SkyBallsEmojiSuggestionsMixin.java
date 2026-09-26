package com.epic60869.skyballs.mixin;

import com.epic60869.skyballs.SkyBallsNopoFeatures;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Adapts NopoMod's emoji autocomplete behaviour: when the current chat token
 * starts with ':', SkyBalls supplies the emoji shortcode suggestions instead of
 * vanilla command suggestions.
 */
@Mixin(CommandSuggestions.class)
public abstract class SkyBallsEmojiSuggestionsMixin {
    @Unique
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Shadow
    @Final
    EditBox input;

    @Shadow
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("TAIL"), cancellable = true)
    private void skyballs$emojiSuggestions(CallbackInfo ci) {
        if (!SkyBallsNopoFeatures.chatEmojisEnabled()) return;

        String text = input.getValue();
        int cursor = input.getCursorPosition();
        if (cursor <= 0 || cursor > text.length()) return;

        String uptoCursor = text.substring(0, cursor);
        int whitespace = lastWhitespaceEnd(uptoCursor);
        String token = uptoCursor.substring(whitespace);

        if (!token.startsWith(":")) return;
        if (token.indexOf(':', 1) >= 0) return;

        pendingSuggestions = SharedSuggestionProvider.suggest(
            SkyBallsNopoFeatures.getChatEmojiSuggestions(),
            new SuggestionsBuilder(uptoCursor, whitespace)
        );
        pendingSuggestions.thenRun(() -> {
            if (pendingSuggestions != null && pendingSuggestions.isDone()) {
                showSuggestions(false);
            }
        });
        ci.cancel();
    }

    @Unique
    private int lastWhitespaceEnd(String text) {
        int end = 0;
        var matcher = WHITESPACE.matcher(text);
        while (matcher.find()) end = matcher.end();
        return end;
    }
}
