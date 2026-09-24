package com.epic60869.skyjew;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * SkyOcean-inspired item search screen.
 *
 * The layout intentionally follows SkyOcean's compact search UI:
 * centered panel, category strip, search field, sort controls and an
 * inventory-style item grid. SkyJew keeps its own Java implementation so
 * it does not require SkyOcean or its Kotlin UI library.
 */
public final class SkyJewStorageSearchScreen extends Screen {
    private static final int BG = 0xB905070B;
    private static final int PANEL = 0xFF121722;
    private static final int PANEL_DARK = 0xFF0B0F17;
    private static final int SLOT = 0xFF202735;
    private static final int SLOT_HOVER = 0xFF35445B;
    private static final int BORDER = 0xFF3B465B;
    private static final int PRIMARY = 0xFF5A65FF;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF8D98AB;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CELL = 20;
    private static final int CATEGORY = 22;

    private final Screen parent;
    private final String initialQuery;

    private EditBox searchBox;
    private boolean searchLore = true;
    private int sortMode = 0; // 0 = amount, 1 = name, 2 = location
    private boolean ascending = true;
    private int scrollRows;
    private List<SkyJewStorageSearch.Result> results = List.of();

    public SkyJewStorageSearchScreen(Screen parent, String query) {
        super(Component.literal("Item Search"));
        this.parent = parent;
        this.initialQuery = query == null ? "" : query;
    }

    @Override
    protected void init() {
        clearWidgets();

        int panelWidth = panelWidth();
        int left = panelLeft();
        int top = panelTop();

        searchBox = new EditBox(font, left + 8, top + 34, panelWidth - 86, 20,
                Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setValue(initialQuery);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(value -> refresh());
        addRenderableWidget(searchBox);

        refresh();
        setInitialFocus(searchBox);
    }

    private int panelWidth() {
        return Math.min(760, Math.max(430, width / 3 + 50));
    }

    private int panelHeight() {
        return Math.min(520, Math.max(300, height / 3 + 50));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private int gridLeft() {
        return panelLeft() + 30;
    }

    private int gridTop() {
        return panelTop() + 68;
    }

    private int gridWidth() {
        return panelWidth() - 38;
    }

    private int columns() {
        return Math.max(1, gridWidth() / CELL);
    }

    private int rows() {
        return Math.max(1, (panelHeight() - 108) / CELL);
    }

    private void refresh() {
        if (searchBox == null) return;

        results = SkyJewStorageSearch.search(
                Minecraft.getInstance(),
                searchBox.getValue(),
                searchLore,
                true
        );


        results = results.stream().sorted((a, b) -> {
            int cmp;
            if (sortMode == 1) {
                cmp = a.name().compareToIgnoreCase(b.name());
            } else if (sortMode == 2) {
                cmp = a.location().compareToIgnoreCase(b.location());
            } else {
                cmp = Integer.compare(b.stack().getCount(), a.stack().getCount());
            }
            return ascending ? cmp : -cmp;
        }).toList();

        int maxRows = Math.max(0, (results.size() + columns() - 1) / columns() - rows());
        scrollRows = Math.max(0, Math.min(scrollRows, maxRows));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, BG);

        int pw = panelWidth();
        int ph = panelHeight();
        int left = panelLeft();
        int top = panelTop();
        int right = left + pw;
        int bottom = top + ph;

        // SkyOcean-style compact centered window.
        g.fill(left, top, right, bottom, PANEL);
        outline(g, left, top, right, bottom, BORDER);
        g.fill(left, top, right, top + 2, PRIMARY);

        g.text(font, "Item Search", left + 8, top + 10, TEXT, true);
        g.text(font,
                SkyJewStorageSearch.cachedStorageCount() + " storages cached",
                right - 112, top + 10, MUTED, false);

        // Only the compass remains on the left. Storage and inventory are
        // searched together.
        drawCategory(g, left - 24, top + 30, Items.COMPASS, "Search");

        // Sort controls.
        int sortX = right - 76;
        g.fill(sortX, top + 34, right - 8, top + 54, SLOT);
        String sort = switch (sortMode) {
            case 1 -> "Name";
            case 2 -> "Location";
            default -> "Amount";
        };
        g.text(font, sort, sortX + 7, top + 40, TEXT, false);
        g.text(font, ascending ? "↑" : "↓", right - 19, top + 40, WHITE, true);

        int gridL = gridLeft();
        int gridT = gridTop();
        int gridR = gridL + columns() * CELL;
        int gridB = gridT + rows() * CELL;

        g.fill(gridL - 3, gridT - 3, gridR + 3, gridB + 3, PANEL_DARK);
        outline(g, gridL - 3, gridT - 3, gridR + 3, gridB + 3, BORDER);

        int start = scrollRows * columns();
        for (int visible = 0; visible < rows() * columns(); visible++) {
            int index = start + visible;
            if (index >= results.size()) break;

            int col = visible % columns();
            int row = visible / columns();
            int x = gridL + col * CELL;
            int y = gridT + row * CELL;

            SkyJewStorageSearch.Result result = results.get(index);
            boolean hover = mouseX >= x && mouseX < x + CELL
                    && mouseY >= y && mouseY < y + CELL;

            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1,
                    hover ? SLOT_HOVER : SLOT);

            g.item(result.stack(), x + 2, y + 2);
            g.itemDecorations(font, result.stack(), x + 2, y + 2);

            if (hover) {
                g.fill(left + 8, bottom - 34, right - 8, bottom - 8, 0xEE080C13);
                String info = result.name() + "  ×" + result.stack().getCount();
                g.text(font, trim(info, pw - 130), left + 14, bottom - 27, TEXT, true);
                g.text(font, trim(result.location(), pw - 130), left + 14, bottom - 16, MUTED, false);
            }
        }

        int resultCount = results.size();
        g.text(font,
                resultCount + " results",
                left + 8, top + 58, MUTED, false);

        long age = SkyJewStorageSearch.oldestCacheAgeMs();
        if (age >= 0) {
            g.text(font, "Cache " + formatAge(age), right - 72, top + 58, MUTED, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawCategory(GuiGraphicsExtractor g, int x, int y,
                              net.minecraft.world.item.Item item, String label) {
        boolean selected = true;
        g.fill(x, y, x + CATEGORY, y + CATEGORY,
                selected ? PRIMARY : SLOT);
        g.item(new net.minecraft.world.item.ItemStack(item), x + 3, y + 3);

        if (selected) {
            g.fill(x + CATEGORY - 2, y + 2, x + CATEGORY, y + CATEGORY - 2, WHITE);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();

        // The compass is the only side control now.

        // Sort selector cycles Amount -> Name -> Location.
        if (event.x() >= right - 76 && event.x() < right - 8
                && event.y() >= top + 34 && event.y() < top + 54) {
            if (event.x() >= right - 28) {
                ascending = !ascending;
            } else {
                sortMode = (sortMode + 1) % 3;
            }
            refresh();
            return true;
        }

        int gridL = gridLeft();
        int gridT = gridTop();
        int gridR = gridL + columns() * CELL;
        int gridB = gridT + rows() * CELL;

        if (event.x() >= gridL && event.x() < gridR
                && event.y() >= gridT && event.y() < gridB) {
            int col = (int)((event.x() - gridL) / CELL);
            int row = (int)((event.y() - gridT) / CELL);
            int index = scrollRows * columns() + row * columns() + col;

            if (index >= 0 && index < results.size()) {
                SkyJewStorageSearch.openResult(Minecraft.getInstance(), results.get(index));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scrollRows += scrollY > 0 ? -1 : 1;
            int maxRows = Math.max(0, (results.size() + columns() - 1) / columns() - rows());
            scrollRows = Math.max(0, Math.min(scrollRows, maxRows));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static void outline(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int color) {
        g.fill(left, top, right, top + 1, color);
        g.fill(left, bottom - 1, right, bottom, color);
        g.fill(left, top, left + 1, bottom, color);
        g.fill(right - 1, top, right, bottom, color);
    }

    private static String trim(String value, int width) {
        if (value == null) return "";
        int max = Math.max(8, width / 6);
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String formatAge(long millis) {
        long seconds = millis / 1000L;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 24) return hours + "h";
        return (hours / 24L) + "d";
    }
}
