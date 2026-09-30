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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * TSW Interface - glass style inventory.
 *
 * Everything is laid out in a fixed 1000x560 "design space" that matches the reference mockup,
 * then scaled to fit whatever GUI size the player has.
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
    private static final int[] P_RIGHT = {719, 123, 238, 312};
    private static final int[] P_BAR = {232, 462, 538, 72};

    // model box inside the left panel
    private static final int MODEL_X0 = 55;
    private static final int MODEL_Y0 = 135;
    private static final int MODEL_X1 = 211;
    private static final int MODEL_Y1 = 425;

    // scrolling crafting list
    private static final int VP_X0 = 727;
    private static final int VP_Y0 = 133;
    private static final int VP_X1 = 939;
    private static final int VP_Y1 = 425;
    private static final int CARD_W = VP_X1 - VP_X0;
    private static final int LIVE_H = 92;
    private static final int CARD_H = 66;
    private static final int GAP = 6;

    // ---------- decorative recipe cards (edit freely) ----------
    private record Recipe(Item result, Item[] grid) {
    }

    private static final Item N = null;

    private static final List<Recipe> RECIPES = List.of(
            new Recipe(Items.TORCH, new Item[]{N, Items.COAL, N, N, Items.STICK, N, N, N, N}),
            new Recipe(Items.CRAFTING_TABLE, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, N, Items.OAK_PLANKS, Items.OAK_PLANKS, N, N, N, N}),
            new Recipe(Items.CHEST, new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, N, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS}),
            new Recipe(Items.FURNACE, new Item[]{Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, N, Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE}),
            new Recipe(Items.IRON_PICKAXE, new Item[]{Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, N, Items.STICK, N, N, Items.STICK, N}),
            new Recipe(Items.IRON_SWORD, new Item[]{N, Items.IRON_INGOT, N, N, Items.IRON_INGOT, N, N, Items.STICK, N}),
            new Recipe(Items.BUCKET, new Item[]{Items.IRON_INGOT, N, Items.IRON_INGOT, N, Items.IRON_INGOT, N, N, N, N}),
            new Recipe(Items.SHIELD, new Item[]{Items.OAK_PLANKS, Items.IRON_INGOT, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, N, Items.OAK_PLANKS, N})
    );

    // ---------- state ----------
    private record SlotView(int index, int x, int y, int size, boolean locked, boolean scrolling) {
    }

    private final LocalPlayer player;
    private final InventoryMenu menu;
    private final List<SlotView> views = new ArrayList<>();
    private SlotView hovered;

    private float s = 1f;
    private float ox = 0f;
    private float oy = 0f;
    private int scroll = 0;

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

    private int maxScroll() {
        int content = LIVE_H + GAP + RECIPES.size() * (CARD_H + GAP) - GAP;
        return Math.max(0, content - (VP_Y1 - VP_Y0));
    }

    private void buildViews() {
        views.clear();

        // armor (menu 5..8) + offhand (45)
        int[] armor = {5, 6, 7, 8, 45};
        for (int i = 0; i < armor.length; i++) {
            views.add(new SlotView(armor[i], ARMOR_X, ARMOR_Y + i * STEP, SLOT, false, false));
        }

        // 7x5 grid: first 27 cells = main inventory (menu 9..35), the rest are locked (decor)
        for (int k = 0; k < GRID_COLS * GRID_ROWS; k++) {
            int c = k % GRID_COLS;
            int r = k / GRID_COLS;
            boolean locked = k >= 27;
            views.add(new SlotView(locked ? -1 : 9 + k, GRID_X + c * STEP, GRID_Y + r * STEP, SLOT, locked, false));
        }

        // quick bar (menu 36..44)
        for (int i = 0; i < 9; i++) {
            views.add(new SlotView(36 + i, BAR_X + i * STEP, BAR_Y, SLOT, false, false));
        }

        // live 2x2 crafting + result (menu 1..4 and 0), inside the scrolling list
        int cy = VP_Y0 - scroll;
        int gx = VP_X0 + 78;
        int gy = cy + 14;
        for (int i = 0; i < 4; i++) {
            views.add(new SlotView(1 + i, gx + (i % 2) * 34, gy + (i / 2) * 34, 30, false, true));
        }
        views.add(new SlotView(0, VP_X0 + 164, cy + (LIVE_H - 40) / 2, 40, false, true));
    }

    private SlotView slotAt(double mx, double my) {
        for (int i = views.size() - 1; i >= 0; i--) {
            SlotView v = views.get(i);
            if (mx >= v.x() && mx < v.x() + v.size() && my >= v.y() && my < v.y() + v.size()) {
                if (v.scrolling() && (mx < VP_X0 || mx >= VP_X1 || my < VP_Y0 || my >= VP_Y1)) {
                    continue;
                }
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

    /** Rounded rectangle built from a handful of fills. */
    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        int[] ins = new int[r];
        for (int i = 0; i < r; i++) {
            double cy = r - i - 0.5;
            ins[i] = (int) Math.round(r - Math.sqrt(r * r - cy * cy));
        }
        int i = 0;
        while (i < r) {
            int j = i;
            while (j + 1 < r && ins[j + 1] == ins[i]) {
                j++;
            }
            int inset = ins[i];
            g.fill(x + inset, y + i, x + w - inset, y + j + 1, color);
            g.fill(x + inset, y + h - 1 - j, x + w - inset, y + h - i, color);
            i = j + 1;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    private void panel(GuiGraphics g, int[] p) {
        rrect(g, p[0], p[1], p[2], p[3], 16, 0x70FFFFFF);
        rrect(g, p[0] + 1, p[1] + 1, p[2] - 2, p[3] - 2, 15, 0xC8262D3D);
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

        // ---- static part ----
        begin(g);
        drawLogo(g);
        panel(g, P_LEFT);
        panel(g, P_GRID);
        panel(g, P_RIGHT);
        panel(g, P_BAR);
        drawTitles(g);
        for (SlotView v : views) {
            if (!v.scrolling()) {
                drawSlot(g, v, v == hovered);
            }
        }
        drawScrollbar(g);
        end(g);

        // ---- player model with the real skin (uses real screen coordinates) ----
        int boxH = MODEL_Y1 - MODEL_Y0;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                rx(MODEL_X0), ry(MODEL_Y0), rx(MODEL_X1), ry(MODEL_Y1),
                Math.round(0.42f * boxH * s),
                0.0625f,
                (float) mouseX, (float) mouseY,
                this.player);

        // ---- crafting list (clipped) ----
        g.enableScissor(rx(VP_X0), ry(VP_Y0), rx(VP_X1), ry(VP_Y1));
        begin(g);
        drawScrollContent(g);
        end(g);
        g.disableScissor();

        // ---- held item / tooltip ----
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
        pose.translate(455, 26);
        pose.scale(0.5f, 0.5f);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 180, 132, 180, 132);
        pose.popMatrix();
    }

    private void drawTitles(GuiGraphics g) {
        String name = this.player.getName().getString();
        name = this.font.plainSubstrByWidth(name, 70);
        text(g, name, 55, 102, 1.7f, 0xFFFFFFFF, false);
        text(g, "Level " + this.player.experienceLevel, 210, 102, 1.7f, 0xFFFFFFFF, false);
        text(g, "Armor", 133, 146, 1.5f, 0xFFFFFFFF, true);
        text(g, "Quick Bar", 500, 444, 1.7f, 0xFFFFFFFF, true);
    }

    private void drawSlot(GuiGraphics g, SlotView v, boolean hover) {
        int x = v.x();
        int y = v.y();
        int sz = v.size();

        if (v.locked()) {
            rrect(g, x, y, sz, sz, 9, 0x22FFFFFF);
            rrect(g, x + 1, y + 1, sz - 2, sz - 2, 8, 0x90161B26);
            return;
        }

        boolean bar = v.index() >= 36 && v.index() <= 44;
        boolean selected = bar && (v.index() - 36) == this.player.getInventory().getSelectedSlot();
        int r = sz >= 44 ? 9 : 6;
        int bw = selected ? 2 : 1;

        rrect(g, x, y, sz, sz, r, selected ? 0xFFA66BFF : 0x66FFFFFF);
        rrect(g, x + bw, y + bw, sz - 2 * bw, sz - 2 * bw, Math.max(1, r - bw), hover ? 0xF0667088 : 0xE0464E62);

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

    private void drawScrollbar(GuiGraphics g) {
        int tx = 946;
        int th = VP_Y1 - VP_Y0;
        rrect(g, tx, VP_Y0, 4, th, 2, 0x33FFFFFF);
        int max = maxScroll();
        int content = th + max;
        int thumb = Math.max(24, th * th / Math.max(content, 1));
        int ty = VP_Y0 + (max == 0 ? 0 : (int) ((th - thumb) * (scroll / (float) max)));
        rrect(g, tx, ty, 4, thumb, 2, 0xAAFFFFFF);
    }

    private void drawScrollContent(GuiGraphics g) {
        int x = VP_X0;
        int y = VP_Y0 - scroll;

        // live crafting card
        rrect(g, x, y, CARD_W, LIVE_H, 12, 0x88FFFFFF);
        rrect(g, x + 1, y + 1, CARD_W - 2, LIVE_H - 2, 11, 0xE05A6377);
        text(g, "Crafting", x + 12, y + LIVE_H / 2f - 7, 1.3f, 0xFFFFFFFF, false);
        arrow(g, x + 148, y + LIVE_H / 2 - 2, 0xFFDDE2F0);

        for (SlotView v : views) {
            if (v.scrolling()) {
                drawSlot(g, v, v == hovered);
            }
        }

        // decorative recipe cards
        int cy = y + LIVE_H + GAP;
        for (Recipe rec : RECIPES) {
            if (cy + CARD_H >= VP_Y0 && cy <= VP_Y1) {
                drawRecipeCard(g, x, cy, rec);
            }
            cy += CARD_H + GAP;
        }
    }

    private void drawRecipeCard(GuiGraphics g, int x, int y, Recipe rec) {
        rrect(g, x, y, CARD_W, CARD_H, 12, 0x55FFFFFF);
        rrect(g, x + 1, y + 1, CARD_W - 2, CARD_H - 2, 11, 0xC03A4152);

        ItemStack result = new ItemStack(rec.result());
        String name = this.font.plainSubstrByWidth(result.getHoverName().getString(), 68);
        text(g, name, x + 10, y + CARD_H / 2f - 4, 0.95f, 0xFFFFFFFF, false);

        int gx = x + 78;
        int gy = y + 6;
        for (int i = 0; i < 9; i++) {
            int cx = gx + (i % 3) * 18;
            int cyy = gy + (i / 3) * 18;
            rrect(g, cx, cyy, 17, 17, 3, 0x55101420);
            Item it = rec.grid()[i];
            if (it != null) {
                drawItem(g, new ItemStack(it), cx, cyy, 17, 1f);
            }
        }

        arrow(g, x + 138, y + CARD_H / 2 - 2, 0xFFDDE2F0);

        rrect(g, x + 158, y + 16, 34, 34, 7, 0x88FFFFFF);
        rrect(g, x + 159, y + 17, 32, 32, 6, 0xE04B5468);
        drawItem(g, result, x + 159, y + 17, 32, 8f);
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

        SlotView v = slotAt(mx, my);
        if (v != null) {
            if (v.locked()) {
                return true;
            }
            ClickType type = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? ClickType.QUICK_MOVE : ClickType.PICKUP;
            slotClick(v.index(), button, type);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double mx = (mouseX - ox) / s;
        double my = (mouseY - oy) / s;
        if (inside(P_RIGHT, mx, my)) {
            scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - scrollY * 24));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
                    slotClick(hovered.index(), (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 ? 1 : 0, ClickType.THROW);
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
