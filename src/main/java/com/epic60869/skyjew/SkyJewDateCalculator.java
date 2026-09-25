package com.epic60869.skyjew;

import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.mixin.SkyJewContainerScreenAccessor;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds the real-world date to SkyBlock calendar tooltips. Ported from Skyblocker's
 * DateCalculatorTooltip (LGPL-3.0), including its use of Fabric's tooltip event so it
 * works alongside other mods that change tooltip rendering.
 */
public final class SkyJewDateCalculator {
    private static final Pattern TIMER_PATTERN = Pattern.compile("((?<days>\\d+)d)? ?((?<hours>\\d+)h)? ?((?<minutes>\\d+)m)? ?((?<seconds>\\d+)s)?");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("E MMM d yyyy HH:mm", Locale.US).withZone(ZoneId.systemDefault());
    private static final TimeProvider[] PROVIDERS = {new Events(), new Calendar()};
    private static TimeProvider currentTimer;

    private SkyJewDateCalculator() {}

    public static void init() {
        // Look at whatever screen is open when the tooltip is built, rather than relying on a
        // particular container screen class or its hovered slot, so it also works when another
        // mod replaces or wraps Hypixel's menus.
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen == null) return;
            currentTimer = timerFor(screen);
            addToTooltip(stack, lines);
        });
    }

    private static TimeProvider timerFor(Screen screen) {
        String screenTitle = ChatFormatting.stripFormatting(screen.getTitle().getString()).trim();
        for (TimeProvider timer : PROVIDERS) {
            if (timer.test(screenTitle)) return timer;
        }
        return null;
    }

    private static void addToTooltip(ItemStack stack, List<Component> lines) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || !Compat.isOnSkyblock()) return;
        if (currentTimer == null) return;

        for (int i = 1; i < lines.size(); i++) {
            String text = ChatFormatting.stripFormatting(lines.get(i).getString());

            //Only attempt to look for a timer if the line contains the qualifying text
            if (!currentTimer.qualifier().test(text)) continue;

            Instant instant = currentTimer.getStartTime(stack, text);

            if (instant != null) {
                lines.add(++i, Component.literal(DATE_FORMATTER.format(instant)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static boolean hasAnyGroup(MatchResult result) {
        for (String group : result.namedGroups().keySet()) {
            if (result.group(group) != null) return true;
        }
        return false;
    }

    private static int group(MatchResult result, String name) {
        String value = result.group(name);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private interface TimeProvider {
        boolean test(String screenTitle);

        default Predicate<String> qualifier() {
            return l -> l.contains("Starts in:") || l.contains(" (");
        }

        Instant getStartTime(ItemStack stack, String qualifiedLine);
    }

    private static class Events implements TimeProvider {
        @Override
        public boolean test(String screenTitle) {
            return screenTitle.equals("Calendar and Events");
        }

        @Override
        public Instant getStartTime(ItemStack stack, String qualifiedLine) {
            MatchResult result = TIMER_PATTERN.matcher(qualifiedLine).results()
                .filter(SkyJewDateCalculator::hasAnyGroup) //Look for the first match that has what we're looking for
                .findFirst()
                .orElse(null);

            if (result != null) {
                return Instant.now()
                    .plus(group(result, "days"), ChronoUnit.DAYS)
                    .plus(group(result, "hours"), ChronoUnit.HOURS)
                    .plus(group(result, "minutes"), ChronoUnit.MINUTES)
                    .plusSeconds(group(result, "seconds"))
                    .plusSeconds(30) // Add 30 seconds to round to the nearest minute
                    .truncatedTo(ChronoUnit.MINUTES);
            }
            return null;
        }
    }

    private static class Calendar implements TimeProvider {
        private static final Pattern PATTERN = Pattern.compile("(?<month>.+), Year (?<year>\\d+)");
        private static final List<String> MONTHS = List.of(
            "early spring", "spring", "late spring", "early summer", "summer", "late summer",
            "early autumn", "autumn", "late autumn", "early winter", "winter", "late winter");
        private int monthIndex;
        private int year = 1;

        @Override
        public boolean test(String screenTitle) {
            Matcher matcher = PATTERN.matcher(screenTitle);
            if (!matcher.matches()) return false;
            int maybeMonth = MONTHS.indexOf(matcher.group("month").trim().toLowerCase(Locale.ROOT));
            if (maybeMonth < 0) return false;
            int maybeYear;
            try {
                maybeYear = Integer.parseInt(matcher.group("year"));
            } catch (NumberFormatException e) {
                return false;
            }
            if (maybeYear == 0) return false;
            monthIndex = maybeMonth;
            year = maybeYear;
            return true;
        }

        @Override
        public Instant getStartTime(ItemStack stack, String qualifiedLine) {
            if (stack.getCount() > 31) return null;
            return SkyJewSkyblockTime.toRealWorld(year, monthIndex, stack.getCount()).toInstant();
        }
    }
}
