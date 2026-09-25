package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewSkyblockTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skyblocker-style Calendar date calculator.
 *
 * Two screens are supported:
 *  - "Calendar and Events" (/calendar): each event's "Starts in: 2d 3h 4m"
 *    countdown is converted to a real-world date.
 *  - A month view titled e.g. "Early Spring, Year 412": the hovered item's
 *    stack count is the day (1-31).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewCalendarTooltipMixin {
    private static final String EVENTS_TITLE = "Calendar and Events";
    private static final Pattern CALENDAR_TITLE =
        Pattern.compile("^(?<month>.+), Year (?<year>\\d+)$");
    private static final Pattern TIMER =
        Pattern.compile("((?<days>\\d+)d)? ?((?<hours>\\d+)h)? ?((?<minutes>\\d+)m)? ?((?<seconds>\\d+)s)?");

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void skyjew$addRealWorldCalendarTime(
        ItemStack stack,
        CallbackInfoReturnable<List<Component>> cir
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) return;
        if (hoveredSlot != null && hoveredSlot.container instanceof Inventory) return;

        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String title = screen.getTitle().getString().trim();

        List<Component> tooltip;
        if (title.equals(EVENTS_TITLE)) {
            tooltip = withEventDates(original);
        } else {
            CalendarDate date = readCalendarDate(title, stack);
            if (date == null) return;
            tooltip = new ArrayList<>(original);
            tooltip.add(dateLine(SkyJewSkyblockTime.formatRealWorld(
                date.year(), date.monthIndex(), date.day()
            )));
        }
        if (tooltip != null) cir.setReturnValue(tooltip);
    }

    /** Inserts a real-world date under every "Starts in:" line, or returns null if there are none. */
    private static List<Component> withEventDates(List<Component> original) {
        List<Component> tooltip = new ArrayList<>(original);
        boolean changed = false;
        for (int i = 1; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString();
            if (!line.contains("Starts in:")) continue;

            Instant start = parseCountdown(line);
            if (start == null) continue;
            tooltip.add(++i, dateLine(SkyJewSkyblockTime.formatRealWorld(start)));
            changed = true;
        }
        return changed ? tooltip : null;
    }

    private static Instant parseCountdown(String line) {
        Matcher matcher = TIMER.matcher(line);
        while (matcher.find()) {
            if (matcher.group("days") == null && matcher.group("hours") == null
                && matcher.group("minutes") == null && matcher.group("seconds") == null) continue;

            return Instant.now()
                .plus(group(matcher, "days"), ChronoUnit.DAYS)
                .plus(group(matcher, "hours"), ChronoUnit.HOURS)
                .plus(group(matcher, "minutes"), ChronoUnit.MINUTES)
                .plusSeconds(group(matcher, "seconds"))
                // Hypixel truncates the countdown, so round to the nearest minute.
                .plusSeconds(30)
                .truncatedTo(ChronoUnit.MINUTES);
        }
        return null;
    }

    private static long group(Matcher matcher, String name) {
        String value = matcher.group(name);
        return value == null ? 0 : Long.parseLong(value);
    }

    private static Component dateLine(String text) {
        return Component.literal(text).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
    }

    private static CalendarDate readCalendarDate(String title, ItemStack stack) {
        Matcher matcher = CALENDAR_TITLE.matcher(title);
        if (!matcher.matches()) return null;

        int year;
        try {
            year = Integer.parseInt(matcher.group("year"));
        } catch (NumberFormatException ignored) {
            return null;
        }

        String month = matcher.group("month").trim();
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
