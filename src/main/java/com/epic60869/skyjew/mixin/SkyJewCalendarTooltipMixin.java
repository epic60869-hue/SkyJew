package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewSkyblockTime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewCalendarTooltipMixin {
    private static final Pattern CALENDAR_TITLE =
        Pattern.compile("(?<month>.+), Year (?<year>\\d+)");

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private void skyjew$addRealWorldCalendarTime(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String title = screen.getTitle().getString();
        Matcher matcher = CALENDAR_TITLE.matcher(title);
        if (!matcher.matches()) {
            return;
        }

        int monthIndex = monthIndex(matcher.group("month"));
        int year;
        try {
            year = Integer.parseInt(matcher.group("year"));
        } catch (NumberFormatException ignored) {
            return;
        }

        int day = stack.getCount();
        if (monthIndex < 0 || year < 1 || day < 1 || day > 31) {
            return;
        }

        List<Component> tooltip = cir.getReturnValue();
        if (tooltip == null) {
            return;
        }

        String realTime;
        try {
            realTime = SkyJewSkyblockTime.formatRealWorld(year, monthIndex, day);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        tooltip.add(Component.literal("Real-world time: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(realTime).withStyle(ChatFormatting.WHITE)));
    }

    private static int monthIndex(String month) {
        String normalized = month.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "early spring" -> 0;
            case "spring" -> 1;
            case "late spring" -> 2;
            case "early summer" -> 3;
            case "summer" -> 4;
            case "late summer" -> 5;
            case "early autumn", "early fall" -> 6;
            case "autumn", "fall" -> 7;
            case "late autumn", "late fall" -> 8;
            case "early winter" -> 9;
            case "winter" -> 10;
            case "late winter" -> 11;
            default -> -1;
        };
    }
}
