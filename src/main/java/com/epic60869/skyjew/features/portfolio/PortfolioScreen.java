package com.epic60869.skyjew.features.portfolio;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** /sj portfolio: the portfolio sheet, sold log and value graph. */
public final class PortfolioScreen extends Screen {
    private enum Tab { SHEET, SOLD, GRAPH }

    private static final int ROW = 12;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());

    private Tab tab = Tab.SHEET;
    private Portfolio.Entry selected;
    private int scroll;
    private EditBox addBox, buyBox, extraBox;
    private String status = "";

    public PortfolioScreen() {
        super(Component.literal("SkyJew Portfolio"));
    }

    private int left() { return 10; }
    private int top() { return 10; }
    private int panelWidth() { return width - 20; }
    private int panelHeight() { return height - 20; }
    private int tableTop() { return top() + 78; }
    private int tableBottom() { return top() + panelHeight() - (tab == Tab.SHEET ? 34 : 8); }

    @Override
    protected void init() {
        Portfolio.refreshPrices(false);
        Portfolio.recount();
        rebuild();
    }

    private void rebuild() {
        String addText = addBox == null ? "" : addBox.getValue();
        clearWidgets();
        int x = left() + 8, y = top() + 24;

        for (Tab t : Tab.values()) {
            String label = switch (t) { case SHEET -> "Portfolio"; case SOLD -> "Sold"; case GRAPH -> "Graph"; };
            Button b = Button.builder(Component.literal(t == tab ? "» " + label + " «" : label), btn -> {
                tab = t;
                scroll = 0;
                rebuild();
            }).bounds(x, y, 70, 20).build();
            addRenderableWidget(b);
            x += 74;
        }

        int rx = left() + panelWidth() - 8;
        rx -= 60;
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {
            Portfolio.refreshPrices(true);
            Portfolio.recount();
            status = "Refreshing prices...";
        }).bounds(rx, y, 60, 20).build());
        rx -= 84;
        addRenderableWidget(Button.builder(Component.literal("Export CSV"), b -> {
            try {
                Path out = Portfolio.exportCsv();
                com.mojang.blaze3d.Blaze3D.openPath(out);
                status = "Exported to " + out;
            } catch (Exception e) {
                status = "Export failed: " + e.getMessage();
            }
        }).bounds(rx, y, 80, 20).build());
        rx -= 124;
        addRenderableWidget(Button.builder(Component.literal(Portfolio.autoRemove() ? "On sale: Auto-log" : "On sale: Ask"), b -> {
            Portfolio.toggleAutoRemove();
            rebuild();
        }).bounds(rx, y, 120, 20).build());
        rx -= 224;
        addRenderableWidget(Button.builder(Component.literal("Prices: " + Portfolio.priceMode().label()), b -> {
            Portfolio.cyclePriceMode();
            rebuild();
        }).bounds(rx, y, 220, 20).build());

        if (tab == Tab.SHEET) {
            int ay = top() + 50;
            addBox = new EditBox(font, left() + 8, ay, 220, 20, Component.literal("Item name or ID"));
            addBox.setHint(Component.literal("Item name or ID (e.g. Enderman Skin)").withStyle(ChatFormatting.DARK_GRAY));
            addBox.setMaxLength(80);
            addBox.setValue(addText);
            addRenderableWidget(addBox);
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
                Portfolio.Entry e = Portfolio.add(addBox.getValue());
                status = e == null ? (Portfolio.pricesLoaded() ? "Couldn't find \"" + addBox.getValue() + "\". Try its SkyBlock ID." : "Item list is still loading, try again in a moment.") : "Added " + e.name + ".";
                if (e != null) {
                    addBox.setValue("");
                    selected = e;
                }
                rebuild();
            }).bounds(left() + 232, ay, 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Add held item"), b -> {
                Portfolio.Entry e = Portfolio.addHeld();
                status = e == null ? "Hold a SkyBlock item first." : "Added " + e.name + ".";
                if (e != null) selected = e;
                rebuild();
            }).bounds(left() + 286, ay, 90, 20).build());

            if (selected != null && Portfolio.entries().contains(selected)) {
                int by = top() + panelHeight() - 28;
                int bx = left() + 8 + Math.min(220, font.width(selected.name) + 12);
                buyBox = new EditBox(font, bx, by, 90, 20, Component.literal("Buy price each"));
                buyBox.setHint(Component.literal("Buy price each").withStyle(ChatFormatting.DARK_GRAY));
                if (selected.buyPrice > 0) buyBox.setValue(Portfolio.coins(selected.buyPrice));
                addRenderableWidget(buyBox);
                addRenderableWidget(Button.builder(Component.literal("Set buy"), b -> {
                    double p = Portfolio.parse(buyBox.getValue());
                    if (p < 0) status = "Type a price like 1.5m, 200k or 1,234,567.";
                    else {
                        selected.buyPrice = p;
                        Portfolio.changed();
                        status = "Buy price set.";
                    }
                }).bounds(bx + 94, by, 56, 20).build());
                bx += 156;
                extraBox = new EditBox(font, bx, by, 50, 20, Component.literal("Extra"));
                extraBox.setHint(Component.literal("Extra").withStyle(ChatFormatting.DARK_GRAY));
                if (selected.extra > 0) extraBox.setValue(String.valueOf(selected.extra));
                addRenderableWidget(extraBox);
                addRenderableWidget(Button.builder(Component.literal("Set extra"), b -> {
                    double v = Portfolio.parse(extraBox.getValue().isBlank() ? "0" : extraBox.getValue());
                    if (v < 0) status = "Type a whole number.";
                    else {
                        selected.extra = (int) v;
                        Portfolio.changed();
                        status = "Extra amount set (copies SkyJew can't see, e.g. applied skins).";
                    }
                }).bounds(bx + 54, by, 60, 20).build());
                bx += 120;
                addRenderableWidget(Button.builder(Component.literal("Log sale"), b -> {
                    Portfolio.recordSale(selected, Math.max(1, Portfolio.count(selected).total()), Portfolio.value(selected), true);
                    selected = null;
                    rebuild();
                }).bounds(bx, by, 60, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
                    Portfolio.remove(selected);
                    selected = null;
                    rebuild();
                }).bounds(bx + 64, by, 56, 20).build());
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF07090D);
        g.fill(left(), top(), left() + panelWidth(), top() + panelHeight(), 0xFF181B21);
        g.fill(left(), top(), left() + panelWidth(), top() + 2, 0xFFFFAA00);

        double total = Portfolio.totalValue(), profit = Portfolio.totalProfit();
        g.text(font, Component.literal("Portfolio").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left() + 8, top() + 8, 0xFFFFFFFF, true);
        Component summary = Component.literal("Value: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(Portfolio.coins(total)).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("   Unrealised P/L: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Portfolio.coins(profit)).withStyle(profit >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED))
            .append(Component.literal("   Realised: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(Portfolio.coins(Portfolio.realisedProfit())).withStyle(Portfolio.realisedProfit() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        g.text(font, summary, left() + 80, top() + 8, 0xFFFFFFFF, false);
        String prices = Portfolio.pricesLoaded() ? "prices " + (System.currentTimeMillis() - Portfolio.lastPriceRefresh()) / 1000 + "s old" : "loading prices...";
        g.text(font, prices, left() + panelWidth() - 8 - font.width(prices), top() + 8, 0xFF7A8290, false);

        switch (tab) {
            case SHEET -> drawSheet(g, mouseX, mouseY);
            case SOLD -> drawSold(g);
            case GRAPH -> drawGraph(g);
        }
        if (!status.isEmpty()) g.text(font, status, left() + 382, top() + 56, 0xFFB8C0CC, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    // ----- Sheet -----

    private int[] columns() {
        int w = panelWidth() - 16;
        int x = left() + 8;
        return new int[]{x, x + (int) (w * 0.30), x + (int) (w * 0.50), x + (int) (w * 0.62), x + (int) (w * 0.74), x + (int) (w * 0.87)};
    }

    private void drawSheet(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int[] c = columns();
        int y = tableTop();
        String[] headers = {"Item", "Qty (inv / storage / extra)", "Buy each", "Price each", "Value", "Profit/loss"};
        for (int i = 0; i < headers.length; i++) g.text(font, headers[i], c[i], y, 0xFFFFAA00, false);
        g.fill(left() + 6, y + 10, left() + panelWidth() - 6, y + 11, 0xFF3A3F48);

        List<Portfolio.Entry> entries = Portfolio.entries();
        if (entries.isEmpty()) {
            g.text(font, "Add an item above, or hold one and click Add held item. Quantities update from your inventory and every Ender Chest / backpack page you have opened.", c[0], y + 18, 0xFF7A8290, false);
        }
        int rowsVisible = (tableBottom() - y - 14) / ROW;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - rowsVisible)));
        int ry = y + 14;
        for (int i = scroll; i < entries.size() && i < scroll + rowsVisible; i++) {
            Portfolio.Entry e = entries.get(i);
            Portfolio.Count count = Portfolio.count(e);
            double price = Portfolio.price(e.id);
            boolean hover = mouseY >= ry - 1 && mouseY < ry + ROW - 1 && mouseX >= left() && mouseX < left() + panelWidth();
            if (e == selected) g.fill(left() + 6, ry - 2, left() + panelWidth() - 6, ry + ROW - 2, 0xFF2E3440);
            else if (hover) g.fill(left() + 6, ry - 2, left() + panelWidth() - 6, ry + ROW - 2, 0xFF22262E);
            g.text(font, font.plainSubstrByWidth(e.name, c[1] - c[0] - 6), c[0], ry, 0xFFFFFFFF, false);
            g.text(font, count.total() + "  (" + count.inventory() + " / " + count.storage() + " / " + count.extra() + ")", c[1], ry, count.total() > 0 ? 0xFFFFFFFF : 0xFF7A8290, false);
            g.text(font, e.buyPrice > 0 ? Portfolio.coins(e.buyPrice) : "-", c[2], ry, 0xFFB8C0CC, false);
            g.text(font, price > 0 ? Portfolio.coins(price) : "?", c[3], ry, 0xFFFFFFFF, false);
            g.text(font, Portfolio.coins(count.total() * price), c[4], ry, 0xFFFFAA00, false);
            if (e.buyPrice > 0 && price > 0) {
                double pl = count.total() * (price - e.buyPrice);
                double pct = (price - e.buyPrice) / e.buyPrice * 100;
                g.text(font, Portfolio.coins(pl) + String.format(" (%+.0f%%)", pct), c[5], ry, pl >= 0 ? 0xFF55FF55 : 0xFFFF5555, false);
            } else {
                g.text(font, "-", c[5], ry, 0xFF7A8290, false);
            }
            ry += ROW;
        }
        if (selected != null && entries.contains(selected)) {
            g.text(font, font.plainSubstrByWidth(selected.name, 210), left() + 8, top() + panelHeight() - 22, 0xFFFFFFFF, true);
        }
    }

    // ----- Sold -----

    private void drawSold(GuiGraphicsExtractor g) {
        int[] c = columns();
        int y = tableTop();
        String[] headers = {"Item", "Sold", "Amount", "Sold for", "Bought for", "Profit"};
        for (int i = 0; i < headers.length; i++) g.text(font, headers[i], c[i], y, 0xFFFFAA00, false);
        g.fill(left() + 6, y + 10, left() + panelWidth() - 6, y + 11, 0xFF3A3F48);
        List<Portfolio.Sale> sold = new ArrayList<>(Portfolio.sold());
        java.util.Collections.reverse(sold);
        if (sold.isEmpty()) g.text(font, "No sales yet. Auction and bazaar sales of tracked items show up here.", c[0], y + 18, 0xFF7A8290, false);
        int rowsVisible = (tableBottom() - y - 14) / ROW;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, sold.size() - rowsVisible)));
        int ry = y + 14;
        for (int i = scroll; i < sold.size() && i < scroll + rowsVisible; i++) {
            Portfolio.Sale s = sold.get(i);
            g.text(font, font.plainSubstrByWidth(s.name, c[1] - c[0] - 6), c[0], ry, 0xFFFFFFFF, false);
            g.text(font, DATE.format(Instant.ofEpochMilli(s.time)), c[1], ry, 0xFFB8C0CC, false);
            g.text(font, String.valueOf(s.amount), c[2], ry, 0xFFFFFFFF, false);
            g.text(font, s.price > 0 ? Portfolio.coins(s.price) : "-", c[3], ry, 0xFFFFAA00, false);
            g.text(font, s.buyPrice > 0 ? Portfolio.coins(s.buyPrice * s.amount) : "-", c[4], ry, 0xFFB8C0CC, false);
            if (s.buyPrice > 0 && s.price > 0) {
                double p = s.price - s.buyPrice * s.amount;
                g.text(font, Portfolio.coins(p), c[5], ry, p >= 0 ? 0xFF55FF55 : 0xFFFF5555, false);
            }
            ry += ROW;
        }
    }

    // ----- Graph -----

    private void drawGraph(GuiGraphicsExtractor g) {
        List<long[]> points = new ArrayList<>(); // time, value
        String label;
        if (selected != null && Portfolio.entries().contains(selected)) {
            label = selected.name + " price (select nothing to see total value)";
            for (Portfolio.Snapshot s : Portfolio.history()) {
                Double p = s.prices.get(selected.id);
                if (p != null && p > 0) points.add(new long[]{s.time, Math.round(p)});
            }
            double now = Portfolio.price(selected.id);
            if (now > 0) points.add(new long[]{System.currentTimeMillis(), Math.round(now)});
        } else {
            label = "Total portfolio value";
            for (Portfolio.Snapshot s : Portfolio.history()) points.add(new long[]{s.time, Math.round(s.total)});
            if (!Portfolio.entries().isEmpty() && Portfolio.pricesLoaded()) points.add(new long[]{System.currentTimeMillis(), Math.round(Portfolio.totalValue())});
        }

        int gx = left() + 70, gy = tableTop() + 14, gw = panelWidth() - 90, gh = tableBottom() - gy - 20;
        g.text(font, label, left() + 8, tableTop(), 0xFFFFAA00, false);
        g.fill(gx, gy, gx + gw, gy + gh, 0xFF101318);
        g.outline(gx, gy, gw, gh, 0xFF3A3F48);
        if (points.size() < 2) {
            g.text(font, "Not enough history yet. A snapshot is saved every 30 minutes while you play.", gx + 8, gy + 8, 0xFF7A8290, false);
            return;
        }
        long minT = points.getFirst()[0], maxT = points.getLast()[0];
        long minV = Long.MAX_VALUE, maxV = Long.MIN_VALUE;
        for (long[] p : points) {
            minV = Math.min(minV, p[1]);
            maxV = Math.max(maxV, p[1]);
        }
        if (maxV == minV) {
            maxV += 1;
            minV -= 1;
        }
        if (maxT == minT) maxT++;
        for (int i = 0; i <= 4; i++) {
            int ly = gy + gh - gh * i / 4;
            g.fill(gx, ly, gx + gw, ly + 1, 0xFF22262E);
            String v = Portfolio.coins(minV + (maxV - minV) * i / 4.0);
            g.text(font, v, gx - 4 - font.width(v), ly - 4, 0xFF7A8290, false);
        }
        g.text(font, DATE.format(Instant.ofEpochMilli(minT)), gx, gy + gh + 4, 0xFF7A8290, false);
        String end = DATE.format(Instant.ofEpochMilli(maxT));
        g.text(font, end, gx + gw - font.width(end), gy + gh + 4, 0xFF7A8290, false);

        int lastX = -1, lastY = -1;
        for (long[] p : points) {
            int x = gx + (int) ((p[0] - minT) * (gw - 1) / (double) (maxT - minT));
            int y = gy + gh - 1 - (int) ((p[1] - minV) * (gh - 1) / (double) (maxV - minV));
            if (lastX >= 0) line(g, lastX, lastY, x, y, 0xFFFFAA00);
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFD27A);
            lastX = x;
            lastY = y;
        }
    }

    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int colour) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, steps);
            int y = y0 + (y1 - y0) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 2, colour);
        }
    }

    // ----- Input -----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (tab == Tab.SHEET || tab == Tab.GRAPH) {
            int y = tableTop() + 14;
            if (tab == Tab.SHEET && event.y() >= y - 2 && event.y() < tableBottom()) {
                int index = scroll + (int) ((event.y() - y + 2) / ROW);
                List<Portfolio.Entry> entries = Portfolio.entries();
                Portfolio.Entry clicked = index >= 0 && index < entries.size() ? entries.get(index) : null;
                selected = clicked == selected ? null : clicked;
                rebuild();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
