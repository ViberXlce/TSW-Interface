package com.nightesports.tswinterface;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Replaces the vanilla HUD (hotbar, hearts, food, armor, xp bar) with the TSW look.
 */
public final class TswHud {

    private static final int PURPLE = 0xFFA66BFF;
    private static final int PURPLE_LIGHT = 0xFFD9C2FF;

    // animated state (updated every client tick)
    private static float ghostHealth = 1f;
    private static float thirst = 1f;
    private static float xpShown = 0f;
    private static int lastLevel = -1;
    private static int doubleTickIn = -1;
    private static boolean wasDrinking = false;

    private TswHud() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TswHud::tick);

        // the hotbar element draws everything; the other vanilla bars are hidden
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR,
                prev -> (g, dt) -> renderAll(prev, g, dt));
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, prev -> (g, dt) -> {
        });
        HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, prev -> (g, dt) -> {
        });
        HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, prev -> (g, dt) -> {
        });
        HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, prev -> (g, dt) -> {
        });
        HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, prev -> (g, dt) -> {
        });
    }

    // =====================================================================
    // state / sounds
    // =====================================================================

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            lastLevel = -1;
            return;
        }

        // blood tube: the light "ghost" part drains slowly after damage
        float hp = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
        if (ghostHealth < hp) {
            ghostHealth = hp;
        } else {
            ghostHealth = Math.max(hp, ghostHealth - 0.012f);
        }

        // xp bar fills smoothly
        int level = p.experienceLevel;
        if (lastLevel >= 0 && level != lastLevel) {
            xpShown = 0f;
        }
        xpShown += (p.experienceProgress - xpShown) * 0.2f;

        // level up sounds: one tick per level, double tick every 10 levels
        if (lastLevel >= 0 && level > lastLevel) {
            tickSound(mc, 1.5f);
            if (level / 10 > lastLevel / 10) {
                doubleTickIn = 4;
            }
        }
        lastLevel = level;
        if (doubleTickIn > 0) {
            doubleTickIn--;
            if (doubleTickIn == 0) {
                tickSound(mc, 1.9f);
            }
        }

        // thirst: slow drain (client-side visual), refill after drinking a potion
        thirst = Math.max(0f, thirst - (p.isSprinting() ? 0.00024f : 0.00008f));
        boolean drinking = p.isUsingItem() && p.getUseItem().is(Items.POTION);
        if (wasDrinking && !p.isUsingItem()) {
            thirst = Math.min(1f, thirst + 0.35f);
        }
        wasDrinking = drinking;
    }

    private static void tickSound(Minecraft mc, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // =====================================================================
    // render
    // =====================================================================

    private static void renderAll(HudElement prev, GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            prev.render(g, dt);
            return;
        }

        int w = g.guiWidth();
        int h = g.guiHeight();

        int barX = (w - 250) / 2;
        int barY = h - 40;
        drawHotbar(g, mc, p, barX, barY);

        if (mc.gameMode.canHurtPlayer()) {
            drawVitals(g, mc, p, h);
        }
        drawProfile(g, mc, p, w);
    }

    private static void drawHotbar(GuiGraphics g, Minecraft mc, LocalPlayer p, int x0, int y0) {
        // panel
        Gfx.rrect(g, x0, y0, 250, 34, 12, 0x70FFFFFF);
        Gfx.rrect(g, x0 + 1, y0 + 1, 248, 32, 11, 0xC8262D3D);

        int selected = p.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            int sx = x0 + 5 + i * 27;
            int sy = y0 + 5;
            boolean sel = i == selected;
            Gfx.rrect(g, sx, sy, 24, 24, 6, sel ? PURPLE : 0x66FFFFFF);
            int bw = sel ? 2 : 1;
            Gfx.rrect(g, sx + bw, sy + bw, 24 - 2 * bw, 24 - 2 * bw, 6 - bw, 0xE0464E62);

            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) {
                g.renderItem(st, sx + 4, sy + 4);
                g.renderItemDecorations(mc.font, st, sx + 4, sy + 4);
            }

            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(sx + 3, sy + 2);
            pose.scale(0.7f, 0.7f);
            g.drawString(mc.font, String.valueOf(i + 1), 0, 0, sel ? 0xFFE6D6FF : 0xFFAAB0C0);
            pose.popMatrix();
        }

        // off-hand slot, only when something is in it
        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) {
            int ox = x0 - 32;
            Gfx.rrect(g, ox, y0 + 2, 28, 30, 8, 0x70FFFFFF);
            Gfx.rrect(g, ox + 1, y0 + 3, 26, 28, 7, 0xC8262D3D);
            g.renderItem(off, ox + 6, y0 + 9);
            g.renderItemDecorations(mc.font, off, ox + 6, y0 + 9);
        }
    }

    private static void drawVitals(GuiGraphics g, Minecraft mc, LocalPlayer p, int screenH) {
        int tw = 12;
        int th = 130;
        int gap = 8;
        int x = 10;
        int y = screenH - 10 - th;

        float hp = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
        float food = Mth.clamp(p.getFoodData().getFoodLevel() / 20f, 0f, 1f);

        // left to right: blood, water, food
        drawTube(g, x, y, tw, th, hp, ghostHealth, 0xFFC8102E, 0xFFFF6A80, 0xFFFFB3BF);
        centeredText(g, mc, String.valueOf(Math.round(p.getHealth())), x + tw / 2, y - 11, 0xFFFFC2CB);

        int x2 = x + tw + gap;
        drawTube(g, x2, y, tw, th, thirst, thirst, 0xFF2E8BFF, 0xFF9CD0FF, 0xFF9CD0FF);
        centeredText(g, mc, Math.round(thirst * 100f) + "%", x2 + tw / 2, y - 11, 0xFFB9DBFF);

        int x3 = x2 + tw + gap;
        drawTube(g, x3, y, tw, th, food, food, 0xFFE09A2B, 0xFFFFD98A, 0xFFFFD98A);
        centeredText(g, mc, String.valueOf(p.getFoodData().getFoodLevel()), x3 + tw / 2, y - 11, 0xFFFFE2A8);

        int armor = p.getArmorValue();
        if (armor > 0) {
            centeredText(g, mc, "ARM " + armor, x + (3 * tw + 2 * gap) / 2, y - 22, 0xFFC7CEDD);
        }
    }

    /** A vertical glass tube: dark interior, liquid rising from the bottom, light trailing part, gloss strip. */
    private static void drawTube(GuiGraphics g, int x, int y, int w, int h,
                                 float frac, float ghost, int color, int shine, int ghostColor) {
        Gfx.rrect(g, x, y, w, h, w / 2, 0xAAFFFFFF);
        Gfx.rrect(g, x + 1, y + 1, w - 2, h - 2, w / 2 - 1, 0xF0141822);

        int ix = x + 2;
        int iy = y + 2;
        int iw = w - 4;
        int ih = h - 4;

        if (ghost > frac) {
            int gh = Math.max(iw, Math.round(ih * ghost));
            Gfx.rrect(g, ix, iy + ih - gh, iw, gh, iw / 2, (ghostColor & 0x00FFFFFF) | 0x99000000);
        }
        if (frac > 0.001f) {
            int lh = Math.max(iw, Math.round(ih * frac));
            int ly = iy + ih - lh;
            Gfx.rrect(g, ix, ly, iw, lh, iw / 2, color);
            if (lh > 10) {
                g.fill(ix + 1, ly + 3, ix + 2, iy + ih - 3, (shine & 0x00FFFFFF) | 0x99000000);
            }
        }
    }

    private static void centeredText(GuiGraphics g, Minecraft mc, String s, int cx, int y, int color) {
        float sc = 0.8f;
        float w = mc.font.width(s) * sc;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx - w / 2f, y);
        pose.scale(sc, sc);
        g.drawString(mc.font, s, 0, 0, color);
        pose.popMatrix();
    }

    private static void text(GuiGraphics g, Minecraft mc, String s, int x, int y, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(0.8f, 0.8f);
        g.drawString(mc.font, s, 0, 0, color);
        pose.popMatrix();
    }

    private static void drawProfile(GuiGraphics g, Minecraft mc, LocalPlayer p, int screenW) {
        int face = 32;
        int fx = screenW - face - 12;
        int fy = 12;

        // xp bar sits directly left of the face and fills toward it
        int bw = 130;
        int bh = 10;
        int bx = fx - 8 - bw;
        int by = fy + 17;
        Gfx.rrect(g, bx, by, bw, bh, bh / 2, 0xAAFFFFFF);
        Gfx.rrect(g, bx + 1, by + 1, bw - 2, bh - 2, bh / 2 - 1, 0xF0141822);
        int ih = bh - 4;
        int iw = bw - 4;
        if (xpShown > 0.001f) {
            int fw = Math.max(ih, Math.round(iw * Mth.clamp(xpShown, 0f, 1f)));
            Gfx.rrect(g, bx + 2, by + 2, fw, ih, ih / 2, PURPLE);
            if (fw > 8) {
                g.fill(bx + 5, by + 3, bx + 2 + fw - 3, by + 4, (PURPLE_LIGHT & 0x00FFFFFF) | 0x99000000);
            }
        }

        // level label above the bar, right aligned
        String label = "Level " + p.experienceLevel;
        float sc = 0.95f;
        int lw = Math.round(mc.font.width(label) * sc);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(bx + bw - lw, by - 11);
        pose.scale(sc, sc);
        g.drawString(mc.font, label, 0, 0, 0xFFFFFFFF);
        pose.popMatrix();

        circleFace(g, mc, p, fx, fy, face);
    }

    /** Player head cut into a circle by drawing it in thin horizontal bands. */
    private static void circleFace(GuiGraphics g, Minecraft mc, LocalPlayer p, int x, int y, int size) {
        Gfx.rrect(g, x - 2, y - 2, size + 4, size + 4, size / 2 + 2, PURPLE);
        Gfx.rrect(g, x, y, size, size, size / 2, 0xFF141822);

        var skin = p.getSkin();
        int half = size / 2;
        int band = 2;
        for (int row = 0; row < size; row += band) {
            double cy = row + band / 2.0 - half;
            int chord = (int) Math.round(Math.sqrt(Math.max(0, (double) half * half - cy * cy)));
            g.enableScissor(x + half - chord, y + row, x + half + chord, y + row + band);
            PlayerFaceRenderer.draw(g, skin, x, y, size);
            g.disableScissor();
        }
    }
}
