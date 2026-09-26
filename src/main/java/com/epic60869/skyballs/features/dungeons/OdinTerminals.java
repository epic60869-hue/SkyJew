package com.epic60869.skyballs.features.dungeons;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.features.FeatureConfigs;
import com.epic60869.skyballs.features.core.SkyBallsLocation;
import com.epic60869.skyballs.sb.events.ServerTickCallback;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Floor 7 terminal solvers, ported from Odin's TerminalSolver and terminal handlers
 * (https://github.com/odtheking/Odin, BSD-3-Clause).
 * <ul>
 *     <li>Every terminal slot is covered and only the slots to click are drawn, in Odin's colours.</li>
 *     <li>Clicks are taken over: a click on a wrong slot is dropped (no misclicks), clicks in the first
 *     moments after opening are blocked (first click protection), and right slots are clicked with a
 *     middle click like Odin.</li>
 *     <li>Client prediction: the solution updates straight away, and falls back to the server's state
 *     if the click doesn't register.</li>
 * </ul>
 */
public final class OdinTerminals {
    enum Type {
        PANES(Pattern.compile("^Correct all the panes!$"), 45),
        RUBIX(Pattern.compile("^Change all to same color!$"), 45),
        NUMBERS(Pattern.compile("^Click in order!$"), 36),
        STARTS_WITH(Pattern.compile("^What starts with: '(\\w)'\\?$"), 45),
        SELECT(Pattern.compile("^Select all the ([\\w ]+) items!$"), 54),
        MELODY(Pattern.compile("^Click the button on time!$"), 54);

        final Pattern title;
        final int windowSize;

        Type(Pattern title, int windowSize) {
            this.title = title;
            this.windowSize = windowSize;
        }
    }

    private static final DyeColor[] RUBIX_ORDER = {DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED};
    private static final int RUBIX_LAST_PANE = 32;

    private static Handler current;

    private OdinTerminals() {}

    static FeatureConfigs.Terminals config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.dungeons.terminals;
    }

    public static boolean enabled() {
        FeatureConfigs.Terminals config = config();
        return config != null && config.odinSolver;
    }

    public static void init() {
        ServerTickCallback.EVENT.register(() -> {
            if (current != null) current.ticksOpened++;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Screen screen = mc.gui.screen();
            if (!(screen instanceof AbstractContainerScreen<?> container)) {
                current = null;
                return;
            }
            if (current != null && current.menu != container.getMenu()) current = null;
            if (current == null && enabled() && SkyBallsLocation.inDungeon()) open(container);
            if (current == null) return;
            // Client prediction fallback: a click the server never answered is forgotten after the timeout.
            FeatureConfigs.Terminals config = config();
            long timeout = config == null ? 600 : config.resolveTimeout;
            if (!current.clicked.isEmpty() && System.currentTimeMillis() - current.lastClick >= timeout) {
                current.clicked.clear();
                current.resolve(container.getMenu().slots, -1);
            }
        });
    }

    private static void open(AbstractContainerScreen<?> screen) {
        String title = SkyBallsLocation.strip(screen.getTitle().getString()).trim();
        for (Type type : Type.values()) {
            Matcher m = type.title.matcher(title);
            if (!m.matches()) continue;
            Handler handler = switch (type) {
                case PANES -> new Panes();
                case RUBIX -> new Rubix();
                case NUMBERS -> new Numbers();
                case STARTS_WITH -> new StartsWith(m.group(1));
                case SELECT -> {
                    DyeColor color = selectColor(m.group(1));
                    yield color == null ? null : new Select(color);
                }
                case MELODY -> new Melody();
            };
            if (handler == null) return;
            handler.menu = screen.getMenu();
            current = handler;
            handler.resolve(screen.getMenu().slots, -1);
            return;
        }
    }

    private static DyeColor selectColor(String name) {
        String wanted = name.replace("SILVER", "LIGHT GRAY").replace('_', ' ');
        for (DyeColor color : DyeColor.values()) {
            if (color.getName().replace('_', ' ').equalsIgnoreCase(wanted) || color.name().replace('_', ' ').equalsIgnoreCase(wanted)) return color;
        }
        return null;
    }

    /** True while a terminal is open and solved by this class. */
    public static boolean active() {
        return current != null && enabled();
    }

    // ----- Hooks (ContainerSolverMenuMixin / ContainerSolverScreenMixin) -----

    /** A slot of the open menu changed. */
    public static void onSetItem(AbstractContainerMenu menu, int slot) {
        Handler handler = current;
        if (handler == null || menu != handler.menu) return;
        if (slot < 0 || slot >= handler.type().windowSize - 9) return;
        ItemStack stack = menu.getSlot(slot).getItem();
        if (stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK))) return;
        // The server has answered clicks up to this slot.
        for (int i = 0; i < handler.clicked.size(); i++) {
            if (handler.clicked.get(i)[0] == slot) {
                handler.clicked.subList(0, i + 1).clear();
                break;
            }
        }
        handler.resolve(menu.slots, slot);
    }

    /** @return true to cancel the vanilla click. */
    public static boolean onSlotClicked(int slot, int button) {
        Handler handler = current;
        if (handler == null || !enabled()) return false;
        if (slot < 0 || slot >= handler.type().windowSize) return true;
        handler.click(slot, button);
        return true;
    }

    /** Covers the terminal and draws the solution. The pose is already at the container's origin. */
    public static void render(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen) {
        Handler handler = current;
        FeatureConfigs.Terminals config = config();
        if (handler == null || config == null || screen.getMenu() != handler.menu) return;
        if (handler.type() == Type.MELODY && !config.melodySolver) return;
        var font = Minecraft.getInstance().font;
        int rows = handler.type().windowSize / 9;
        graphics.fill(7, 17, 7 + 9 * 18, 17 + rows * 18, colour(config.backgroundColor, 0x80262626));
        for (Slot slot : screen.getMenu().slots) {
            if (slot.index >= handler.type().windowSize) continue;
            if (!handler.solution.contains(slot.index)) continue;
            Render r = handler.render(slot.index);
            if (r == null) continue;
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, r.colour());
            if (r.text() != null) graphics.text(font, r.text(), slot.x + 8 - font.width(r.text()) / 2, slot.y + 4, 0xFFFFFFFF, true);
        }
    }

    static int colour(String value, int fallback) {
        try {
            return ChromaColour.Companion.specialToChromaRGB(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ----- Handlers -----

    record Render(int colour, String text) {}

    abstract static class Handler {
        AbstractContainerMenu menu;
        final List<int[]> clicked = new ArrayList<>();
        final List<Integer> solution = new ArrayList<>();
        final long opened = System.currentTimeMillis();
        long lastClick;
        int ticksOpened;

        abstract Type type();

        abstract List<Integer> solve(List<Slot> slots, int updated);

        abstract Render render(int slot);

        void resolve(List<Slot> allSlots, int updated) {
            int size = Math.min(type().windowSize - 9, allSlots.size());
            solution.clear();
            solution.addAll(solve(allSlots.subList(0, size), updated));
            FeatureConfigs.Terminals config = config();
            if (config == null || config.clickPrediction) for (int[] c : clicked) simulate(c[0], c[1]);
        }

        void simulate(int slot, int button) {
            solution.remove(Integer.valueOf(slot));
        }

        boolean canClick(int slot, int button) {
            return solution.contains(slot);
        }

        boolean protect() {
            FeatureConfigs.Terminals config = config();
            if (config == null) return false;
            if (System.currentTimeMillis() - opened < config.firstClickProt) return true;
            return config.lagProtection && ticksOpened < config.lagProtectionTicks;
        }

        void click(int slot, int button) {
            FeatureConfigs.Terminals config = config();
            boolean blockWrong = config == null || config.blockMisclicks;
            if (protect()) return;
            if (blockWrong && !canClick(slot, button)) return;
            int mouse = button == 1 && type() == Type.RUBIX ? 1 : 2;
            clicked.add(new int[]{slot, mouse});
            lastClick = System.currentTimeMillis();
            if (config == null || config.clickPrediction) simulate(slot, mouse);
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null || mc.player == null) return;
            mc.gameMode.handleContainerInput(menu.containerId, slot, mouse, mouse == 2 ? ContainerInput.CLONE : ContainerInput.PICKUP, mc.player);
        }
    }

    private static boolean is(ItemStack stack, Item item) {
        return stack.is(item);
    }

    static final class Panes extends Handler {
        Type type() { return Type.PANES; }

        List<Integer> solve(List<Slot> slots, int updated) {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) if (is(slots.get(i).getItem(), Items.STAINED_GLASS_PANE.pick(DyeColor.RED))) out.add(i);
            return out;
        }

        Render render(int slot) {
            return new Render(colour(config().panesColor, 0xFF55FF55), null);
        }
    }

    static final class Numbers extends Handler {
        Type type() { return Type.NUMBERS; }

        List<Integer> solve(List<Slot> slots, int updated) {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) if (is(slots.get(i).getItem(), Items.STAINED_GLASS_PANE.pick(DyeColor.RED))) out.add(i);
            out.sort((a, b) -> Integer.compare(slots.get(a).getItem().getCount(), slots.get(b).getItem().getCount()));
            return out;
        }

        @Override
        void simulate(int slot, int button) {
            if (!solution.isEmpty()) solution.removeFirst();
        }

        @Override
        boolean canClick(int slot, int button) {
            return !solution.isEmpty() && solution.getFirst() == slot;
        }

        Render render(int slot) {
            FeatureConfigs.Terminals c = config();
            int index = solution.indexOf(slot);
            int colour = switch (index) {
                case 0 -> colour(c.numbers1Color, 0xFF55FF55);
                case 1 -> colour(c.numbers2Color, 0xFF2A7F2A);
                case 2 -> colour(c.numbers3Color, 0xFF153F15);
                default -> 0;
            };
            return new Render(colour, String.valueOf(Math.abs((solution.size() - 14) - index) + 1));
        }
    }

    static final class Rubix extends Handler {
        private DyeColor locked;
        private final Set<Integer> rightClick = new HashSet<>();

        Type type() { return Type.RUBIX; }

        private static DyeColor paneColour(ItemStack stack) {
            return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof StainedGlassPaneBlock pane ? pane.getColor() : null;
        }

        private static int indexOf(DyeColor color) {
            for (int i = 0; i < RUBIX_ORDER.length; i++) if (RUBIX_ORDER[i] == color) return i;
            return -1;
        }

        private static int dist(int pane, int goal) {
            return pane > goal ? goal + RUBIX_ORDER.length - pane : goal - pane;
        }

        private Map<Integer, Integer> clicksFor(DyeColor goal, List<int[]> panes) {
            boolean leftOnly = config() != null && config().rubixLeftClicksOnly;
            int goalIndex = indexOf(goal);
            Map<Integer, Integer> out = new HashMap<>();
            for (int[] p : panes) {
                int forward = dist(p[1], goalIndex);
                int clicks = forward > 2 && !leftOnly ? forward - RUBIX_ORDER.length : forward;
                if (clicks != 0) out.put(p[0], clicks);
            }
            return out;
        }

        List<Integer> solve(List<Slot> slots, int updated) {
            List<int[]> panes = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                DyeColor color = paneColour(slots.get(i).getItem());
                if (color == null || color == DyeColor.BLACK) continue;
                int index = indexOf(color);
                if (index >= 0) panes.add(new int[]{i, index});
            }
            if ((updated == RUBIX_LAST_PANE || updated == -1) && locked == null && panes.size() >= 9) {
                int best = Integer.MAX_VALUE;
                for (DyeColor goal : RUBIX_ORDER) {
                    int total = clicksFor(goal, panes).values().stream().mapToInt(Math::abs).sum();
                    if (total < best) {
                        best = total;
                        locked = goal;
                    }
                }
            }
            rightClick.clear();
            List<Integer> out = new ArrayList<>();
            if (locked == null) return out;
            for (var entry : clicksFor(locked, panes).entrySet()) {
                if (entry.getValue() < 0) rightClick.add(entry.getKey());
                for (int i = 0; i < Math.abs(entry.getValue()); i++) out.add(entry.getKey());
            }
            return out;
        }

        @Override
        boolean canClick(int slot, int button) {
            return solution.contains(slot) && (button == 1) == rightClick.contains(slot);
        }

        Render render(int slot) {
            FeatureConfigs.Terminals c = config();
            int remaining = (int) solution.stream().filter(s -> s == slot).count();
            if (remaining == 0) return null;
            int clicks = rightClick.contains(slot) ? -remaining : remaining;
            int colour = switch (clicks) {
                case 1 -> colour(c.rubix1Color, 0xFF55FF55);
                case 2 -> colour(c.rubix2Color, 0xFF2A7F2A);
                case -1, 4 -> colour(c.rubixMinus1Color, 0xFFAA0000);
                default -> colour(c.rubixMinus2Color, 0xFF550000);
            };
            return new Render(colour, String.valueOf(clicks));
        }
    }

    static final class StartsWith extends Handler {
        private final String letter;
        private final Map<Integer, Boolean> clickedOverrides = new HashMap<>();

        StartsWith(String letter) {
            this.letter = letter.toLowerCase(Locale.ROOT);
        }

        Type type() { return Type.STARTS_WITH; }

        List<Integer> solve(List<Slot> slots, int updated) {
            clickedOverrides.computeIfPresent(updated, (k, v) -> true);
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                String name = SkyBallsLocation.strip(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
                boolean glint = stack.hasFoil() && !stack.is(Items.GOLDEN_APPLE) && !stack.getPrototype().has(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                if (name.startsWith(letter) && clickedOverrides.get(i) != Boolean.TRUE && !glint) out.add(i);
            }
            return out;
        }

        @Override
        void click(int slot, int button) {
            if (canClick(slot, button) && !clickedOverrides.containsKey(slot)) clickedOverrides.put(slot, false);
            super.click(slot, button);
        }

        Render render(int slot) {
            return new Render(colour(config().startsWithColor, 0xFF55FF55), null);
        }
    }

    static final class Select extends Handler {
        private final Set<String> prefixes;

        Select(DyeColor color) {
            prefixes = switch (color) {
                case BLACK -> Set.of("black", "ink");
                case BLUE -> Set.of("blue", "lapis");
                case BROWN -> Set.of("brown", "cocoa");
                case WHITE -> Set.of("white", "bone", "wool");
                case GREEN -> Set.of("green", "cactus");
                case RED -> Set.of("red", "rose");
                case YELLOW -> Set.of("yellow", "dandelion");
                case LIGHT_GRAY -> Set.of("silver", "light gray");
                default -> Set.of(color.name().toLowerCase(Locale.ROOT).replace('_', ' '));
            };
        }

        Type type() { return Type.SELECT; }

        List<Integer> solve(List<Slot> slots, int updated) {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i).getItem();
                if (stack.isEmpty() || stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK)) || stack.hasFoil()) continue;
                String name = SkyBallsLocation.strip(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
                for (String prefix : prefixes) {
                    if (name.startsWith(prefix)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }

        Render render(int slot) {
            return new Render(colour(config().selectColor, 0xFF55FF55), null);
        }
    }

    static final class Melody extends Handler {
        Type type() { return Type.MELODY; }

        List<Integer> solve(List<Slot> slots, int updated) {
            int magenta = -1;
            int greenPane = -1;
            int greenClay = -1;
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i).getItem();
                if (magenta < 0 && stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.MAGENTA))) magenta = i;
                if (stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.LIME))) greenPane = i;
                if (stack.is(Items.DYED_TERRACOTTA.pick(DyeColor.LIME))) greenClay = i;
            }
            List<Integer> out = new ArrayList<>();
            out.add(greenPane);
            out.add(magenta);
            if (greenPane >= 0 && magenta >= 0 && greenPane % 9 == magenta % 9) out.add(greenClay);
            return out;
        }

        @Override
        boolean canClick(int slot, int button) {
            return slot == 16 || slot == 25 || slot == 34 || slot == 43;
        }

        @Override
        void simulate(int slot, int button) {}

        Render render(int slot) {
            FeatureConfigs.Terminals c = config();
            int row = slot / 9;
            return new Render(row == 0 || row == 4 ? colour(c.melodyColumnColor, 0xFFAA00AA) : colour(c.melodyPointerColor, 0xFF55FF55), null);
        }
    }
}
