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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
public abstract class SkyJewCalendarTooltipMixin {
    /*
     * Port of Skyblocker's DateCalculatorTooltip Calendar provider.
     *
     * Skyblocker does NOT read the calendar day from the item name/lore.
     * It reads the month/year from the calendar screen title and the day
     * from the hovered calendar item's stack count.
     */
    private static final Pattern CALENDAR_PATTERN = Pattern.compile(
        "(?i)(early spring|spring|late spring|early summer|summer|late summer|early autumn|autumn|late autumn|early winter|winter|late winter)"
            + "\\s*(?:,\\s*)?(?:day\\s*)?(\\d{1,2})?(?:\\s*,?\\s*)?year\\s*(\\d+)"
    );

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void skyjew$addRealWorldCalendarTime(
        ItemStack stack,
        CallbackInfoReturnable<List<Component>> cir
    ) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        CalendarDate date = readCalendarDate(screen.getTitle().getString(), original, stack);
        if (date == null) return;

        for (Component line : original) {
            if (line.getString().toLowerCase(Locale.ROOT).contains("real-world time:")) {
                return;
            }
        }

        try {
            // Copy the list before changing it. This also works if another
            // tooltip provider returned an immutable list.
            List<Component> tooltip = new ArrayList<>(original);
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
            cir.setReturnValue(tooltip);
        } catch (IllegalArgumentException ignored) {
            // Invalid calendar data must never break the inventory tooltip.
        }
    }

    private static CalendarDate readCalendarDate(
        String screenTitle,
        List<Component> tooltip,
        ItemStack stack
    ) {
        // The calendar GUI title supplies the SkyBlock month/year. The hovered
        // calendar item supplies the day through its stack count on Hypixel.
        CalendarDate titleDate = parseDate(screenTitle);
        if (titleDate != null) {
            int day = stack.getCount();
            if (day >= 1 && day <= 31) {
                return new CalendarDate(titleDate.monthIndex(), day, titleDate.year());
            }
        }

        // Resource-pack/menu variants sometimes expose the date in tooltip text
        // instead of the screen title. Prefer an explicit day if present.
        for (Component line : tooltip) {
            String text = line.getString();
            CalendarDate parsed = parseDate(text);
            if (parsed != null) {
                int day = parsed.day() > 0 ? parsed.day() : stack.getCount();
                if (day >= 1 && day <= 31) {
                    return new CalendarDate(parsed.monthIndex(), day, parsed.year());
                }
            }

            Matcher dayMatcher = Pattern.compile("(?i)day\\s*(\\d{1,2})").matcher(text);
            if (dayMatcher.find()) {
                CalendarDate date = parseDate(screenTitle);
                if (date != null) {
                    int day = Integer.parseInt(dayMatcher.group(1));
                    if (day >= 1 && day <= 31) {
                        return new CalendarDate(date.monthIndex(), day, date.year());
                    }
                }
            }
        }

        return null;
    }

    private static CalendarDate parseDate(String text) {
        if (text == null) return null;
        Matcher matcher = CALENDAR_PATTERN.matcher(text.trim());
        if (!matcher.find()) return null;

        int monthIndex = monthIndex(matcher.group(1));
        if (monthIndex < 0) return null;

        int explicitDay = -1;
        if (matcher.group(2) != null && !matcher.group(2).isBlank()) {
            try {
                explicitDay = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        try {
            int year = Integer.parseInt(matcher.group(3));
            if (year < 1) return null;
            return new CalendarDate(monthIndex, explicitDay, year);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
