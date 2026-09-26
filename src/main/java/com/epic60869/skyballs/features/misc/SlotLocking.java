package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.mixin.SkyBallsContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * Slot locking and slot binding. Neither clicks anything for you that you couldn't click yourself, so both are
 * allowed: locking only stops your own clicks and drops, binding turns a shift-click into the same swap you'd do with
 * a number key.
 * <ul>
 *   <li>Lock (like SkyblockAddons): press the lock key (L) over an inventory slot. A locked slot can't be clicked,
 *   moved, swapped or dropped, and Q does nothing while it's your selected hotbar slot. Press L again to unlock.</li>
 *   <li>Bind (Odin's Slot Binds): in your inventory, press the bind key (B) over a slot, then over another (one must be
 *   in the hotbar). Shift-clicking either then swaps them. Press B on a bound slot to remove the bind. A line joins
 *   bound slots while you hover one.</li>
 * </ul>
 */
public final class SlotLocking {
    private static Integer pendingBind;

    private SlotLocking() {}

    private static SkyBallsConfig.SlotLocking config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc.slotLocking;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !keyPressed(container, event.key()));
            ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> render(container, g, mouseX, mouseY));
            ScreenEvents.remove(screen).register(s -> pendingBind = null);
        });
    }

    // ---------------------------------------------------------------- locks

    /** The player inventory index of a slot (0-8 hotbar, 9-35 inventory), or -1 for container / armour slots. */
    private static int inventoryIndex(Slot slot) {
        if (slot == null || !(slot.container instanceof Inventory)) return -1;
        int index = slot.getContainerSlot();
        return index >= 0 && index < 36 ? index : -1;
    }

    public static boolean isLocked(int inventoryIndex) {
        SkyBallsConfig.SlotLocking c = config();
        return c != null && c.enabled && inventoryIndex >= 0 && c.locked.contains(inventoryIndex);
    }

    /**
     * Called for every slot click in a menu (ContainerSolverScreenMixin); true cancels it. Blocks anything touching a
     * locked slot, including number-key swaps into a locked hotbar slot, and turns a shift-click on a bound slot into
     * the bound swap.
     */
    public static boolean onSlotClicked(AbstractContainerScreen<?> screen, Slot slot, int button, ContainerInput input) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null || !c.enabled) return false;
        if (isLocked(inventoryIndex(slot))) return true;
        if (input == ContainerInput.SWAP && button >= 0 && button < 9 && isLocked(button)) return true;

        // Slot binds: shift-click in your own inventory swaps with the bound hotbar slot.
        if (screen instanceof InventoryScreen && input == ContainerInput.QUICK_MOVE && slot != null) {
            int clicked = slot.index;
            Integer bound = boundTo(clicked);
            if (bound == null) return false;
            int from;
            int hotbar;
            if (clicked >= 36 && clicked <= 44) {
                from = bound;
                hotbar = clicked - 36;
            } else if (bound >= 36 && bound <= 44) {
                from = clicked;
                hotbar = bound - 36;
            } else {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null || mc.player == null) return false;
            if (isLocked(hotbar) || isLocked(inventoryIndex(screen.getMenu().getSlot(from)))) return true;
            mc.gameMode.handleContainerInput(screen.getMenu().containerId, from, hotbar, ContainerInput.SWAP, mc.player);
            return true;
        }
        return false;
    }

    /** Q in the world: nothing drops while your selected hotbar slot is locked. */
    public static boolean blockDrop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        boolean blocked = isLocked(mc.player.getInventory().getSelectedSlot());
        if (blocked) say(Component.literal("That slot is locked (press L over it in your inventory to unlock).").withStyle(ChatFormatting.RED));
        return blocked;
    }

    private static Integer boundTo(int menuSlot) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null) return null;
        Integer direct = c.binds.get(menuSlot);
        if (direct != null) return direct;
        for (var e : c.binds.entrySet()) if (e.getValue() == menuSlot) return e.getKey();
        return null;
    }

    private static boolean keyPressed(AbstractContainerScreen<?> screen, int key) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null || !c.enabled || key == GLFW.GLFW_KEY_UNKNOWN) return false;
        Slot hovered = ((SkyBallsContainerScreenAccessor) screen).skyballs$getHoveredSlot();
        if (key == c.lockKey) {
            int index = inventoryIndex(hovered);
            if (index < 0) return false;
            if (!c.locked.remove((Integer) index)) c.locked.add(index);
            SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, c.locked.contains(index) ? 1.2f : 0.8f));
            return true;
        }
        if (key == c.bindKey && screen instanceof InventoryScreen) {
            if (hovered == null || hovered.index < 5 || hovered.index >= 45) return false;
            int clicked = hovered.index;
            if (pendingBind == null) {
                Integer existing = c.binds.containsKey(clicked) ? Integer.valueOf(clicked) : null;
                if (existing == null) {
                    for (var e : c.binds.entrySet()) if (e.getValue() == clicked) existing = e.getKey();
                }
                if (existing != null) {
                    int to = c.binds.remove(existing);
                    SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
                    say(Component.literal("Removed the bind between slots " + existing + " and " + to + ".").withStyle(ChatFormatting.YELLOW));
                    return true;
                }
                pendingBind = clicked;
                return true;
            }
            int first = pendingBind;
            pendingBind = null;
            if (first == clicked) {
                say(Component.literal("You can't bind a slot to itself.").withStyle(ChatFormatting.RED));
                return true;
            }
            if ((first < 36 || first > 44) && (clicked < 36 || clicked > 44)) {
                say(Component.literal("One of the two slots must be in your hotbar.").withStyle(ChatFormatting.RED));
                return true;
            }
            c.binds.put(first, clicked);
            SkyBallsConfig.saveCurrent(SkyBallsConfig.current());
            say(Component.literal("Bound slot " + first + " to slot " + clicked + ". Shift-click either to swap them.").withStyle(ChatFormatting.GREEN));
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    private static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SkyBallsConfig.SlotLocking c = config();
        if (c == null || !c.enabled) return;
        SkyBallsContainerScreenAccessor access = (SkyBallsContainerScreenAccessor) screen;
        int left = access.skyballs$getLeftPos();
        int top = access.skyballs$getTopPos();
        for (Slot slot : screen.getMenu().slots) {
            if (!isLocked(inventoryIndex(slot))) continue;
            int x = left + slot.x;
            int y = top + slot.y;
            g.fill(x, y, x + 16, y + 16, 0x60FF3030);
            g.outline(x, y, 16, 16, 0xC0FF3030);
            g.text(Minecraft.getInstance().font, "🔒", x + 9, y - 1, 0xFFFFFFFF, true);
        }

        if (!(screen instanceof InventoryScreen)) return;
        Slot hovered = access.skyballs$getHoveredSlot();
        Integer startIndex = pendingBind != null ? pendingBind : hovered == null ? null : Integer.valueOf(hovered.index);
        if (startIndex == null || startIndex < 5 || startIndex >= 45) return;
        Slot start = screen.getMenu().getSlot(startIndex);
        int sx = left + start.x + 8;
        int sy = top + start.y + 8;
        int ex;
        int ey;
        if (pendingBind != null) {
            ex = mouseX;
            ey = mouseY;
        } else {
            Integer bound = boundTo(startIndex);
            if (bound == null || (c.lineOnlyWithShift && !Minecraft.getInstance().hasShiftDown())) return;
            Slot end = screen.getMenu().getSlot(bound);
            ex = left + end.x + 8;
            ey = top + end.y + 8;
        }
        line(g, sx, sy, ex, ey, 0xFF55FF55);
    }

    /** A straight line made of small squares. */
    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int colour) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps);
            int y = y1 + (y2 - y1) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static void say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
            Component.literal("[SB] ").withStyle(ChatFormatting.DARK_GREEN).append(message));
    }
}
