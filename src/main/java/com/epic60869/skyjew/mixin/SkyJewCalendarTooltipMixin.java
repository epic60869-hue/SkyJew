package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewSkyblockTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewCalendarTooltipMixin {
    /*
     * This follows Skyblocker's DateCalculatorTooltip Calendar provider:
     * the calendar screen title contains the month/year, while the calendar
     * item's stack count is the day number. We intentionally do not try to
     * infer the date from the item's lore/name because Hypixel can change
     * those tooltip strings.
     */
    private static final Pattern CALENDAR_TITLE_PATTERN =
        Pattern.compile("(?<month>.+), Year (?<year>\\d+)");

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private void skyjew$addRealWorldCalendarTime(
        ItemStack stack,
        CallbackInfoReturnable<List<Component>> cir
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        CalendarDate date = readCalendarDate(screen.getTitle().getString(), stack);
        if (date == null) return;

        List<Component> tooltip = cir.getReturnValue();
        if (tooltip == null) return;

        try {
            tooltip.add(
                Component.literal("Real-world time: ")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)
                    .append(
                        Component.literal(SkyJewSkyblockTime.formatRealWorld(
                            date.year(),
                            date.monthIndex(),
                            date.day()
                        )).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)
                    )
            );
        } catch (IllegalArgumentException ignored) {
            // Invalid calendar data should never break the inventory tooltip.
        }
    }

    private static CalendarDate readCalendarDate(String screenTitle, ItemStack stack) {
        Matcher matcher = CALENDAR_TITLE_PATTERN.matcher(screenTitle);
        if (!matcher.matches()) return null;

        int monthIndex = monthIndex(matcher.group("month"));
        if (monthIndex < 0) return null;

        int year;
        try {
            year = Integer.parseInt(matcher.group("year"));
        } catch (NumberFormatException ignored) {
            return null;
        }

        // Skyblocker uses the calendar item's stack count as its day number.
        int day = stack.getCount();
        if (day < 1 || day > 31 || year < 1) return null;

        return new CalendarDate(monthIndex, day, year);
    }

    private static int monthIndex(String month) {
        return switch (month.trim().toLowerCase(Locale.ROOT)) {
            case "early spring" -> 0;
            case "spring" -> 1;
            case "late spring" -> 2;
            case "early summer" -> 3;
            case "summer" -> 4;
            case "late summer" -> 5;
            case "early autumn" -> 6;
            case "autumn" -> 7;
            case "late autumn" -> 8;
            case "early winter" -> 9;
            case "winter" -> 10;
            case "late winter" -> 11;
            default -> -1;
        };
    }

    private record CalendarDate(int monthIndex, int day, int year) {}
}
