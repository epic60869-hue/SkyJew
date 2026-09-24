package com.epic60869.skyjew.mixin;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.SkyJewSkyblockTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
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
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?i)(early spring|spring|late spring|early summer|summer|late summer|early autumn|autumn|late autumn|early winter|winter|late winter)\\s*(?:,?\\s*)"
            + "(?:day\\s*)?(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*|\\s+)year\\s*(\\d+)"
    );

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private void skyjew$addRealWorldCalendarTime(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        SkyJewConfig config = SkyJewConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || stack == null || stack.isEmpty()) return;

        CalendarDate date = findCalendarDate(stack);
        if (date == null) return;

        List<Component> tooltip = cir.getReturnValue();
        if (tooltip == null) return;

        try {
            String realTime = SkyJewSkyblockTime.formatRealWorld(date.year, date.monthIndex, date.day);
            tooltip.add(Component.literal("Real-world time: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(realTime).withStyle(ChatFormatting.WHITE)));
        } catch (IllegalArgumentException ignored) {}
    }

    private static CalendarDate findCalendarDate(ItemStack stack) {
        StringBuilder text = new StringBuilder();
        append(text, stack.getHoverName());
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                text.append(' ');
                append(text, line);
            }
        }
        Matcher matcher = DATE_PATTERN.matcher(text.toString().replace('\n', ' '));
        if (!matcher.find()) return null;
        int monthIndex = monthIndex(matcher.group(1));
        try {
            int day = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));
            if (monthIndex < 0 || day < 1 || day > 31 || year < 1) return null;
            return new CalendarDate(monthIndex, day, year);
        } catch (NumberFormatException ignored) { return null; }
    }

    private static void append(StringBuilder out, Component component) {
        if (component != null) out.append(component.getString()).append(' ');
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
