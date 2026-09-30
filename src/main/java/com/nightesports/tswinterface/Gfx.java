package com.nightesports.tswinterface;

import net.minecraft.client.gui.GuiGraphics;

/** Small drawing helpers shared by the inventory screen and the HUD. */
final class Gfx {

    private Gfx() {
    }

    /** Rounded rectangle built from a handful of fills. */
    static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
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
}
