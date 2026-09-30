package com.nightesports.tswinterface;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * TSW Interface - glass style inventory.
 *
 * Everything is laid out in a fixed 1000x560 "design space", then scaled to fit the GUI size.
 */
public class TswInventoryScreen extends Screen {

    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath(TswInterfaceClient.MOD_ID, "textures/gui/logo.png");

    // ---------- design space ----------
    private static final int DW = 1000;
    private static final int DH = 560;

    private static final int SLOT = 50;
    private static final int STEP = 59;

    private static final int GRID_X = 300;
    private static final int GRID_Y = 137;
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 5;

    private static final int BAR_X = 241;
    private static final int BAR_Y = 473;

    private static final int ARMOR_X = 221;
    private static final int ARMOR_Y = 137;

    // panels: x, y, w, h
    private static final int[] P_LEFT = {43, 125, 238, 310};
    private static final int[] P_GRID = {289, 123, 420, 312};
    private static final int[] P_RIGHT = {719, 123, 238, 112};
    private static final int[] P_BAR = {232, 462, 538, 72};

    // model box inside the left panel
    private static final int MODEL_X0 = 55;
    private static final int MODEL_Y0 = 135;
    private static final int MODEL_X1 = 211;
    private static final int MODEL_Y1 = 425;

    // crafting card (inside the right panel)
    private static final int CARD_X = 727;
    private static final int CARD_Y = 133;
    private static final int CARD_W = 212;
    private static final int CARD_H = 92;

    // ---------- state ----------
    private record SlotView(int index, int x, int y, int size, boolean locked) {
    }

    private final LocalPlayer player;
    private final InventoryMenu menu;
    private final List<SlotView> views = new ArrayList<>();
    private SlotView hovered;

    private float s = 1f;
    private float ox = 0f;
    private float oy = 0f;

    public TswInventoryScreen(LocalPlayer player) {
        super(Component.literal("TSW Interface"));
        this.player = player;
        this.menu = player.inventoryMenu;
    }

    // =====================================================================
    // layout helpers
    // =====================================================================

    private void layout() {
        s = Math.min(this.width * 0.97f / DW, this.height * 0.97f / DH);
        s = Math.min(s, 1.6f);
        ox = (this.width - DW * s) / 2f;
        oy = (this.height - DH * s) / 2f;
    }

    private int rx(float v) {
        return Math.round(ox + v * s);
    }

    private int ry(float v) {
        return Math.round(oy + v * s);
    }

    private void buildViews() {
        views.clear();

        // armor (menu 5..8) + offhand (45)
        int[] armor = {5, 6, 7, 8, 45};
        for (int i = 0; i < armor.length; i++) {
            views.add(new SlotView(armor[i], ARMOR_X, ARMOR_Y + i * STEP, SLOT, false));
        }

        // 7x5 grid: first 27 cells = main inventory (menu 9..35), the rest are locked (decor)
        for (int k = 0; k < GRID_COLS * GRID_ROWS; k++) {
            int c = k % GRID_COLS;
            int r = k / GRID_COLS;
            boolean locked = k >= 27;
            views.add(new SlotView(locked ? -1 : 9 + k, GRID_X + c * STEP, GRID_Y + r * STEP, SLOT, locked));
        }

        // quick bar (menu 36..44)
        for (int i = 0; i < 9; i++) {
            views.add(new SlotView(36 + i, BAR_X + i * STEP, BAR_Y, SLOT, false));
        }

        // live 2x2 crafting (menu 1..4) + result (menu 0)
        int gx = CARD_X + 78;
        int gy = CARD_Y + 14;
        for (int i = 0; i < 4; i++) {
            views.add(new SlotView(1 + i, gx + (i % 2) * 34, gy + (i / 2) * 34, 30, false));
        }
        views.add(new SlotView(0, CARD_X + 164, CARD_Y + (CARD_H - 40) / 2, 40, false));
    }

    private SlotView slotAt(double mx, double my) {
        for (int i = views.size() - 1; i >= 0; i--) {
            SlotView v = views.get(i);
            if (mx >= v.x() && mx < v.x() + v.size() && my >= v.y() && my < v.y() + v.size()) {
                return v;
            }
        }
        return null;
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    // =====================================================================
    // drawing primitives
    // =====================================================================

    private void begin(GuiGraphics g) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(ox, oy);
        pose.scale(s, s);
    }

    private void end(GuiGraphics g) {
        g.pose().popMatrix();
    }

    private void panel(GuiGraphics g, int[] p) {
        Gfx.rrect(g, p[0], p[1], p[2], p[3], 16, 0x70FFFFFF);
        Gfx.rrect(g, p[0] + 1, p[1] + 1, p[2] - 2, p[3] - 2, 15, 0xC8262D3D);
    }

    private void text(GuiGraphics g, String str, float x, float y, float sc, int color, boolean centered) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(sc, sc);
        int w = this.font.width(str);
        g.drawString(this.font, str, centered ? -w / 2 : 0, 0, color);
        pose.popMatrix();
    }

    private void arrow(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y + 1, x + 12, y + 3, color);
        g.fill(x + 8, y - 3, x + 9, y + 7, color);
        g.fill(x + 9, y - 2, x + 10, y + 6, color);
        g.fill(x + 10, y - 1, x + 11, y + 5, color);
        g.fill(x + 11, y, x + 12, y + 4, color);
        g.fill(x + 12, y + 1, x + 13, y + 3, color);
    }

    /** Draws an item centred in a square box, plus its count / durability decorations. */
    private void drawItem(GuiGraphics g, ItemStack st, float x, float y, float box, float pad) {
        if (st.isEmpty()) {
            return;
        }
        var pose = g.pose();
        float k = (box - pad) / 16f;
        float off = (box - 16f * k) / 2f;
        pose.pushMatrix();
        pose.translate(x + off, y + off);
        pose.scale(k, k);
        g.renderItem(st, 0, 0);
        pose.popMatrix();

        float d = Math.max(0.8f, box / 40f);
        pose.pushMatrix();
        pose.translate(x + box - 17f * d - 3f, y + box - 17f * d - 3f);
        pose.scale(d, d);
        g.renderItemDecorations(this.font, st, 0, 0);
        pose.popMatrix();
    }

    // =====================================================================
    // render
    // =====================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // vanilla background (blur + dim)
        super.render(g, mouseX, mouseY, partialTick);

        layout();
        buildViews();
        double mx = (mouseX - ox) / s;
        double my = (mouseY - oy) / s;
        hovered = slotAt(mx, my);

        begin(g);
        drawLogo(g);
        panel(g, P_LEFT);
        panel(g, P_GRID);
        panel(g, P_RIGHT);
        panel(g, P_BAR);
        drawTitles(g);
        drawCraftingCard(g);
        for (SlotView v : views) {
            drawSlot(g, v, v == hovered);
        }
        end(g);

        // player model with the real skin (real screen coordinates)
        int boxH = MODEL_Y1 - MODEL_Y0;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                rx(MODEL_X0), ry(MODEL_Y0), rx(MODEL_X1), ry(MODEL_Y1),
                Math.round(0.42f * boxH * s),
                0.0625f,
                (float) mouseX, (float) mouseY,
                this.player);

        // held item / tooltip
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty()) {
            float box = 16f * Math.max(1f, s * 2f) + 12f;
            drawItem(g, carried, mouseX - box / 2f, mouseY - box / 2f, box, 12f);
        } else if (hovered != null && !hovered.locked()) {
            ItemStack st = this.menu.getSlot(hovered.index()).getItem();
            if (!st.isEmpty()) {
                g.setTooltipForNextFrame(this.font, st, mouseX, mouseY);
            }
        }
    }

    private void drawLogo(GuiGraphics g) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(400, 14);
        pose.scale(0.5f, 0.5f);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 400, 160, 400, 160);
        pose.popMatrix();
    }

    private void drawTitles(GuiGraphics g) {
        String name = this.font.plainSubstrByWidth(this.player.getName().getString(), 90);
        text(g, name, 55, 102, 1.7f, 0xFFFFFFFF, false);
        text(g, "Armor", 133, 146, 1.5f, 0xFFFFFFFF, true);
        text(g, "Quick Bar", 500, 444, 1.7f, 0xFFFFFFFF, true);
    }

    private void drawCraftingCard(GuiGraphics g) {
        Gfx.rrect(g, CARD_X, CARD_Y, CARD_W, CARD_H, 12, 0x88FFFFFF);
        Gfx.rrect(g, CARD_X + 1, CARD_Y + 1, CARD_W - 2, CARD_H - 2, 11, 0xE05A6377);
        text(g, "Crafting", CARD_X + 12, CARD_Y + CARD_H / 2f - 7, 1.3f, 0xFFFFFFFF, false);
        arrow(g, CARD_X + 148, CARD_Y + CARD_H / 2 - 2, 0xFFDDE2F0);
    }

    private void drawSlot(GuiGraphics g, SlotView v, boolean hover) {
        int x = v.x();
        int y = v.y();
        int sz = v.size();

        if (v.locked()) {
            Gfx.rrect(g, x, y, sz, sz, 9, 0x22FFFFFF);
            Gfx.rrect(g, x + 1, y + 1, sz - 2, sz - 2, 8, 0x90161B26);
            return;
        }

        boolean bar = v.index() >= 36 && v.index() <= 44;
        boolean selected = bar && (v.index() - 36) == this.player.getInventory().getSelectedSlot();
        int r = sz >= 44 ? 9 : 6;
        int bw = selected ? 2 : 1;

        Gfx.rrect(g, x, y, sz, sz, r, selected ? 0xFFA66BFF : 0x66FFFFFF);
        Gfx.rrect(g, x + bw, y + bw, sz - 2 * bw, sz - 2 * bw, Math.max(1, r - bw),
                hover ? 0xF0667088 : 0xE0464E62);

        Slot slot = this.menu.getSlot(v.index());
        ItemStack st = slot.getItem();
        if (st.isEmpty()) {
            var icon = slot.getNoItemIcon();
            if (icon != null) {
                float k = (sz - 14) / 16f;
                float off = (sz - 16f * k) / 2f;
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(x + off, y + off);
                pose.scale(k, k);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 16, 16);
                pose.popMatrix();
            }
        } else {
            drawItem(g, st, x, y, sz, 12f);
        }

        if (bar) {
            text(g, String.valueOf(v.index() - 35), x + 6, y + 4, 0.9f,
                    selected ? 0xFFE6D6FF : 0xFFAAB0C0, false);
        }
    }

    // =====================================================================
    // input
    // =====================================================================

    private void slotClick(int slot, int button, ClickType type) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, slot, button, type, this.player);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = (event.x() - ox) / s;
        double my = (event.y() - oy) / s;
        int button = event.button();

        // use exactly the slot that is highlighted on screen; fall back to the event position
        SlotView v = hovered != null ? hovered : slotAt(mx, my);
        if (v != null) {
            if (v.locked()) {
                return true;
            }
            boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            slotClick(v.index(), button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            return true;
        }

        boolean overPanel = inside(P_LEFT, mx, my) || inside(P_GRID, mx, my)
                || inside(P_RIGHT, mx, my) || inside(P_BAR, mx, my);
        if (!overPanel && !this.menu.getCarried().isEmpty()) {
            slotClick(-999, button, ClickType.PICKUP);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null) {
            if (this.minecraft.options.keyInventory.matches(event)) {
                this.onClose();
                return true;
            }
            if (hovered != null && !hovered.locked()) {
                for (int i = 0; i < 9; i++) {
                    if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                        slotClick(hovered.index(), i, ClickType.SWAP);
                        return true;
                    }
                }
                if (this.minecraft.options.keySwapOffhand.matches(event)) {
                    slotClick(hovered.index(), 40, ClickType.SWAP);
                    return true;
                }
                if (this.minecraft.options.keyDrop.matches(event)) {
                    boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
                    slotClick(hovered.index(), ctrl ? 1 : 0, ClickType.THROW);
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.player.closeContainer();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
