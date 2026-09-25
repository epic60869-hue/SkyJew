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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skyblocker-style Calendar date calculator.
 *
 * The Calendar GUI title is the authoritative source for month/year:
 *   "Early Spring, Year 412"
 * The hovered calendar item stack count is the day (1-31).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewCalendarTooltipMixin {
    private static final Pattern CALENDAR_TITLE =
        Pattern.compile("^(?<month>.+), Year (?<year>\\d+)$");

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void skyjew$addRealWorldCalendarTime(
        ItemStack stack,
        CallbackInfoReturnable<List<Component>> cir
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        CalendarDate date = readCalendarDate(screen.getTitle().getString(), stack);
        if (date == null) return;

        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<Component> tooltip = new ArrayList<>(original);
        tooltip.add(
            Component.literal(SkyJewSkyblockTime.formatRealWorld(
                date.year(), date.monthIndex(), date.day()
            )).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)
        );
        cir.setReturnValue(tooltip);
    }

    private static CalendarDate readCalendarDate(String title, ItemStack stack) {
        Matcher matcher = CALENDAR_TITLE.matcher(title.trim());
        if (!matcher.matches()) return null;

        int year;
        try {
            year = Integer.parseInt(matcher.group("year"));
        } catch (NumberFormatException ignored) {
            return null;
        }

        String month = matcher.group("month");
        int monthIndex = switch (month.toLowerCase(java.util.Locale.ROOT)) {
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

        int day = stack.getCount();
        if (monthIndex < 0 || year < 1 || day < 1 || day > 31) return null;
        return new CalendarDate(monthIndex, day, year);
    }

    private record CalendarDate(int monthIndex, int day, int year) {}
}
