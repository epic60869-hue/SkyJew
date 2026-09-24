package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SkyJew Experimentation Table helper.
 *
 * The helper intentionally does not trust a single sound or a single inventory
 * update. It continuously validates the table state and throws away stale
 * data when the GUI changes. This makes it much harder to get stuck after a
 * delayed/missed Hypixel inventory packet.
 */
public final class SkyJewExperimentHelper {
    private enum Phase { UNKNOWN, READ, REPLICATE }

    private static final Pattern ROUND = Pattern.compile("Round:\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> CHRON_COLORS = Set.of(
        "white", "orange", "magenta", "light blue", "yellow", "lime",
        "pink", "gray", "light gray", "cyan", "purple", "blue", "brown",
        "green", "red", "black"
    );

    private static SkyJewConfig config;
    private static Phase phase = Phase.UNKNOWN;
    private static final List<String> chronSequence = new ArrayList<>();
    private static final List<Integer> ultraSequence = new ArrayList<>();
    private static final Set<String> lastReadChronColors = new HashSet<>();
    private static int chronProgress;
    private static int ultraProgress;
    private static int lastRound = -1;
    private static int lastUltraCount = -1;
    private static int lastReadRound = -1;
    private static String lastPhaseText = "";
    private static long lastStateChange;
    private static long lastRecovery;

    private SkyJewExperimentHelper() {}

    public static void init(SkyJewConfig cfg) {
        config = cfg;
        reset();
    }

    public static void tick(Minecraft mc) {
        if (config == null || !config.experiments.table.enabled) {
            resetIfNeeded();
            return;
        }

        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen) || !isExperimentTable(screen)) {
            resetIfNeeded();
            return;
        }

        scan(screen);
    }

    public static void render(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen) {
        if (config == null || !config.experiments.table.enabled || !isExperimentTable(screen)) return;

        if (config.experiments.table.highlight) {
            if (phase == Phase.REPLICATE) {
                if (isChronomatron(screen)) {
                    highlightChron(g, screen);
                } else if (isUltrasequencer(screen)) {
                    highlightUltra(g, screen);
                }
            }
        }
    }

    public static boolean handleClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (config == null || !config.experiments.enabled) return false;
        if (phase != Phase.REPLICATE || !isExperimentTable(screen)) return false;

        Slot slot = findSlot(screen, mouseX, mouseY);
        if (slot == null || slot.getItem().isEmpty()) return false;

        if (isChronomatron(screen)) {
            String expected = chronSequence.size() > chronProgress ? chronSequence.get(chronProgress) : null;
            if (expected == null) return false;
            String clicked = colorName(slot.getItem());
            if (clicked == null) return false;
            if (!expected.equals(clicked)) return config.experiments.table.preventMisclicks;
            chronProgress++;
            return false;
        }

        if (isUltrasequencer(screen)) {
            if (ultraProgress >= ultraSequence.size()) return false;
            if (slot.index != ultraSequence.get(ultraProgress)) return config.experiments.table.preventMisclicks;
            ultraProgress++;
            return false;
        }

        return false;
    }

    private static void scan(AbstractContainerScreen<?> screen) {
        Phase detected = detectPhase(screen);
        if (detected == Phase.UNKNOWN) {
            // Don't instantly wipe state during one malformed packet. Recover after
            // a short grace period instead.
            if (System.currentTimeMillis() - lastStateChange > 900) recover();
            return;
        }

        boolean typeChanged = isChronomatron(screen) != lastPhaseWasChron;
        if (typeChanged) {
            reset();
            lastPhaseWasChron = isChronomatron(screen);
        }

        String phaseText = statusText(screen);
        if (!phaseText.equals(lastPhaseText)) {
            lastPhaseText = phaseText;
            lastStateChange = System.currentTimeMillis();
        }

        if (detected != phase) {
            Phase old = phase;
            phase = detected;
            lastStateChange = System.currentTimeMillis();

            if (detected == Phase.READ) {
                if (old == Phase.REPLICATE) {
                    chronProgress = 0;
                    ultraProgress = 0;
                }
                if (isChronomatron(screen)) {
                    lastReadChronColors.clear();
                }
            } else if (detected == Phase.REPLICATE) {
                chronProgress = 0;
                ultraProgress = 0;
            }
        }

        if (isChronomatron(screen)) readChronomatron(screen);
        if (isUltrasequencer(screen)) readUltrasequencer(screen);

        // If the server changed the round without us seeing a clean phase transition,
        // rebuild from the current board instead of using stale sequence data.
        int round = readRound(screen);
        if (round >= 0 && lastRound >= 0 && round < lastRound) recover();
        if (round > lastRound) {
            lastRound = round;
            if (phase == Phase.REPLICATE) chronProgress = 0;
        }
    }

    private static void readChronomatron(AbstractContainerScreen<?> screen) {
        Set<String> visible = new LinkedHashSet<>();
        for (Slot slot : slots(screen)) {
            String color = colorName(slot.getItem());
            if (color != null) visible.add(color);
        }

        if (phase == Phase.READ) {
            int round = readRound(screen);
            if (visible.size() == 1 && round >= 0 && round != lastReadRound) {
                chronSequence.add(visible.iterator().next());
                lastReadRound = round;
                lastStateChange = System.currentTimeMillis();
            }
            if (visible.isEmpty()) lastReadChronColors.clear();
        }
    }

    private static void readUltrasequencer(AbstractContainerScreen<?> screen) {
        if (phase != Phase.READ) return;

        List<IndexedNumber> numbers = new ArrayList<>();
        for (Slot slot : slots(screen)) {
            if (slot.getItem().isEmpty()) continue;
            Integer number = numberName(slot.getItem());
            if (number != null && number >= 1) {
                numbers.add(new IndexedNumber(number, slot.getContainerSlot()));
            }
        }

        numbers.sort(Comparator.comparingInt(IndexedNumber::number));
        if (numbers.isEmpty()) return;

        boolean changed = numbers.size() != lastUltraCount;
        if (!changed) {
            for (int i = 0; i < numbers.size(); i++) {
                if (ultraSequence.size() <= i || !Objects.equals(ultraSequence.get(i), numbers.get(i).slot())) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            ultraSequence.clear();
            for (IndexedNumber n : numbers) ultraSequence.add(n.slot());
            lastUltraCount = numbers.size();
            lastStateChange = System.currentTimeMillis();
        }
    }

    private static void highlightChron(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen) {
        if (chronProgress >= chronSequence.size()) return;
        String next = chronSequence.get(chronProgress);
        String nextNext = chronProgress + 1 < chronSequence.size() ? chronSequence.get(chronProgress + 1) : null;

        for (Slot slot : slots(screen)) {
            String color = colorName(slot.getItem());
            if (color == null) continue;
            if (color.equals(next)) {
                fillSlot(g, screen, slot, 0x6644FF44);
            } else if (nextNext != null && color.equals(nextNext)) {
                fillSlot(g, screen, slot, 0x3333AAFF);
            }
        }
    }

    private static void highlightUltra(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen) {
        if (ultraProgress >= ultraSequence.size()) return;
        int expected = ultraSequence.get(ultraProgress);
        for (Slot slot : slots(screen)) {
            if (slot.getContainerSlot() == expected) {
                fillSlot(g, screen, slot, 0x6655FF55);
                break;
            }
        }
    }

    private static void fillSlot(GuiGraphicsExtractor g, AbstractContainerScreen<?> screen, Slot slot, int color) {
        int left = (screen.width - 176) / 2;
        int top = (screen.height - 166) / 2;
        g.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, color);
    }

    private static Slot findSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int left = (screen.width - 176) / 2;
        int top = (screen.height - 166) / 2;
        for (Slot slot : slots(screen)) {
            if (mouseX >= left + slot.x - 1 && mouseX < left + slot.x + 17
                && mouseY >= top + slot.y - 1 && mouseY < top + slot.y + 17) {
                return slot;
            }
        }
        return null;
    }

    private static List<Slot> slots(AbstractContainerScreen<?> screen) {
        if (screen == null || screen.getMenu() == null) return List.of();
        return screen.getMenu().slots;
    }

    private static boolean isExperimentTable(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        return title.toLowerCase(Locale.ROOT).contains("experimentation table");
    }

    private static boolean isChronomatron(AbstractContainerScreen<?> screen) {
        for (Slot slot : slots(screen)) {
            String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("chronomatron")) return true;
        }
        return phase != Phase.UNKNOWN && !isUltrasequencer(screen);
    }

    private static boolean isUltrasequencer(AbstractContainerScreen<?> screen) {
        for (Slot slot : slots(screen)) {
            String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("ultrasequencer")) return true;
        }
        return false;
    }

    private static Phase detectPhase(AbstractContainerScreen<?> screen) {
        String text = statusText(screen).toLowerCase(Locale.ROOT);
        if (text.contains("remember the pattern")) return Phase.READ;
        if (text.contains("timer:")) return Phase.REPLICATE;
        return Phase.UNKNOWN;
    }

    private static String statusText(AbstractContainerScreen<?> screen) {
        for (Slot slot : slots(screen)) {
            String name = slot.getItem().getHoverName().getString();
            if (name.contains("Remember the pattern") || name.contains("Timer:")) return name;
        }
        return "";
    }

    private static int readRound(AbstractContainerScreen<?> screen) {
        for (Slot slot : slots(screen)) {
            String name = slot.getItem().getHoverName().getString();
            Matcher m = ROUND.matcher(name.replace("§", ""));
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private static String colorName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT).trim();
        for (String color : CHRON_COLORS) {
            if (name.equals(color) || name.endsWith(" " + color)) return color;
        }
        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        for (String color : CHRON_COLORS) {
            if (id.contains(color.replace(" ", "_"))) return color;
        }
        return null;
    }

    private static Integer numberName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String s = stack.getHoverName().getString().replaceAll("[^0-9]", "");
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { return null; }
    }

    private static void resetIfNeeded() {
        if (phase != Phase.UNKNOWN || !chronSequence.isEmpty() || !ultraSequence.isEmpty()) reset();
    }

    private static void recover() {
        if (System.currentTimeMillis() - lastRecovery < 250) return;
        lastRecovery = System.currentTimeMillis();
        phase = Phase.UNKNOWN;
        chronSequence.clear();
        ultraSequence.clear();
        lastReadChronColors.clear();
        chronProgress = 0;
        ultraProgress = 0;
        lastRound = -1;
        lastUltraCount = -1;
        lastReadRound = -1;
        lastPhaseText = "";
        lastStateChange = System.currentTimeMillis();
    }

    public static void reset() {
        recover();
        lastPhaseWasChron = false;
    }

    private static boolean lastPhaseWasChron;

    private record IndexedNumber(int number, int slot) {}
}
