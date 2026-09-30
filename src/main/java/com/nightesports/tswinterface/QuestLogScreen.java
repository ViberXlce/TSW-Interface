package com.nightesports.tswinterface;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Quest Log (press L). Neon purple / cyan style, two columns of mission cards, scrollable. */
public class QuestLogScreen extends Screen {

    // design space
    private static final int DW = 1000;
    private static final int DH = 580;

    // main panel
    private static final int PX = 70;
    private static final int PY = 16;
    private static final int PW = 860;
    private static final int PH = 548;

    // scrolling viewport
    private static final int VX0 = 96;
    private static final int VY0 = 104;
    private static final int VX1 = 884;
    private static final int VY1 = 520;

    private static final int COL_W = 382;
    private static final int COL_GAP = 24;
    private static final int CARD_H = 124;
    private static final int PITCH = CARD_H + 10;

    private static final int CYAN = 0xFF35E0FF;
    private static final int PURPLE = 0xFF8A4FFF;
    private static final int GREEN = 0xFF4ADE80;

    private record Btn(int x, int y, int w, int h, QuestData.Quest quest, boolean details) {
    }

    private final List<Btn> btns = new ArrayList<>();
    private float s = 1f;
    private float ox = 0f;
    private float oy = 0f;
    private int scroll = 0;

    public QuestLogScreen() {
        super(Component.literal("Quest Log"));
    }

    // =====================================================================
    // helpers
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

    private void begin(GuiGraphics g) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(ox, oy);
        pose.scale(s, s);
    }

    private void end(GuiGraphics g) {
        g.pose().popMatrix();
    }

    /** align: 0 = left, 1 = centre, 2 = right */
    private void text(GuiGraphics g, String str, float x, float y, float sc, int color, int align, boolean bold) {
        Component c = bold ? Component.literal(str).withStyle(ChatFormatting.BOLD) : Component.literal(str);
        int w = this.font.width(c);
        int dx = align == 1 ? -w / 2 : (align == 2 ? -w : 0);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(sc, sc);
        g.drawString(this.font, c, dx, 0, color);
        pose.popMatrix();
    }

    private int maxScroll() {
        int rows = (QuestData.QUESTS.size() + 1) / 2;
        int content = rows * PITCH - 10;
        return Math.max(0, content - (VY1 - VY0));
    }

    // =====================================================================
    // render
    // =====================================================================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        layout();
        btns.clear();

        begin(g);
        drawFrame(g);
        drawHeader(g);
        drawFooter(g);
        drawScrollbar(g);
        end(g);

        g.enableScissor(rx(VX0), ry(VY0), rx(VX1), ry(VY1));
        begin(g);
        drawCards(g);
        end(g);
        g.disableScissor();
    }

    private void drawFrame(GuiGraphics g) {
        // soft glow, cyan frame, purple inner line, dark body
        Gfx.rrect(g, PX - 4, PY - 4, PW + 8, PH + 8, 18, 0x3335E0FF);
        Gfx.rrect(g, PX, PY, PW, PH, 14, CYAN);
        Gfx.rrect(g, PX + 2, PY + 2, PW - 4, PH - 4, 12, 0xFF100A20);
        Gfx.rrect(g, PX + 7, PY + 7, PW - 14, PH - 14, 9, 0xFF6D3FD0);
        Gfx.rrect(g, PX + 9, PY + 9, PW - 18, PH - 18, 8, 0xFF120C24);
    }

    private void drawHeader(GuiGraphics g) {
        // title pill
        Gfx.rrect(g, 370, 4, 260, 48, 18, CYAN);
        Gfx.rrect(g, 373, 7, 254, 42, 16, 0xFF160F2E);
        text(g, "QUEST LOG", 500, 19, 2.0f, 0xFF5CF2FF, 1, true);

        // sub header with side lines
        text(g, "ACTIVE MISSIONS", 500, 68, 1.5f, 0xFF5CF2FF, 1, true);
        g.fill(250, 77, 372, 79, CYAN);
        g.fill(628, 77, 750, 79, CYAN);
    }

    private void drawFooter(GuiGraphics g) {
        text(g, "[L]", 100, 533, 1.1f, 0xFFFFD166, 0, true);
        text(g, "CLOSE", 122, 533, 1.1f, 0xFF9AA0B5, 0, true);
        text(g, "SORT BY: TYPE", 500, 533, 1.1f, 0xFF9AA0B5, 1, true);
        text(g, "TOTAL QUESTS: " + QuestData.QUESTS.size(), 900, 533, 1.1f, 0xFF9AA0B5, 2, true);
    }

    private void drawScrollbar(GuiGraphics g) {
        int tx = 894;
        int th = VY1 - VY0;
        Gfx.rrect(g, tx, VY0, 8, th, 4, 0x55FFFFFF);
        Gfx.rrect(g, tx + 1, VY0 + 1, 6, th - 2, 3, 0xFF1A1233);
        int max = maxScroll();
        int content = th + max;
        int thumb = Math.max(30, th * th / Math.max(content, 1));
        int ty = VY0 + (max == 0 ? 0 : (int) ((th - thumb) * (scroll / (float) max)));
        Gfx.rrect(g, tx + 1, ty + 1, 6, thumb - 2, 3, PURPLE);
    }

    private void drawCards(GuiGraphics g) {
        List<QuestData.Quest> list = QuestData.QUESTS;
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int x = VX0 + col * (COL_W + COL_GAP);
            int y = VY0 + row * PITCH - scroll;
            if (y + CARD_H < VY0 || y > VY1) {
                continue;
            }
            drawCard(g, x, y, list.get(i));
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, QuestData.Quest q) {
        boolean done = q.status == QuestData.Status.COMPLETED;

        Gfx.rrect(g, x, y, COL_W, CARD_H, 8, done ? 0xFF34D399 : 0xFF7C3AED);
        Gfx.rrect(g, x + 2, y + 2, COL_W - 4, CARD_H - 4, 7, done ? 0xFF0F2A26 : 0xFF150F28);

        // icon box
        Gfx.rrect(g, x + 10, y + 10, 68, 68, 6, 0xFF2EE6E6);
        Gfx.rrect(g, x + 12, y + 12, 64, 64, 5, 0xFF0D1024);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 20, y + 20);
        pose.scale(3f, 3f);
        g.renderItem(new ItemStack(q.icon), 0, 0);
        pose.popMatrix();

        int tx = x + 90;
        int maxW = 255;

        // title + status
        text(g, q.title, tx, y + 9, 1.15f, 0xFFFFFFFF, 0, true);
        text(g, statusText(q.status), x + COL_W - 12, y + 9, 1.05f, statusColor(q.status), 2, false);

        text(g, "Objective:", tx, y + 28, 1.0f, 0xFF8C90A6, 0, false);
        String obj = q.objective + (q.goal > 0 ? " (" + q.progress + "/" + q.goal + ")" : "");
        obj = this.font.plainSubstrByWidth(obj, maxW);
        text(g, obj, tx, y + 40, 1.05f, 0xFFFFFFFF, 0, false);

        // progress bar
        if (q.goal > 0) {
            int bw = COL_W - 110;
            Gfx.rrect(g, tx, y + 54, bw, 5, 2, 0xFF2A2F45);
            int fw = Math.max(3, Math.round(bw * Math.min(1f, q.progress / (float) q.goal)));
            if (q.progress > 0) {
                Gfx.rrect(g, tx, y + 54, fw, 5, 2, done ? 0xFF22C55E : CYAN);
            }
        }

        text(g, "Rewards:", tx, y + 66, 1.0f, 0xFF8C90A6, 0, false);
        text(g, this.font.plainSubstrByWidth(q.rewards, maxW), tx, y + 78, 1.05f, 0xFFFFD166, 0, false);

        // buttons
        button(g, x + 10, y + 92, 68, 22, buttonLabel(q.status), 0xFF9AA0B5, q, false);
        button(g, x + COL_W - 92, y + 92, 82, 22, "Details", 0xFF35C6E8, q, true);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int border,
                        QuestData.Quest q, boolean details) {
        Gfx.rrect(g, x, y, w, h, 7, border);
        Gfx.rrect(g, x + 1, y + 1, w - 2, h - 2, 6, 0xFF1B2034);
        text(g, label, x + w / 2f, y + 7, 1.0f, 0xFFFFFFFF, 1, false);
        btns.add(new Btn(x, y, w, h, q, details));
    }

    private static String statusText(QuestData.Status st) {
        return switch (st) {
            case NEW -> "New";
            case ACTIVE -> "Active";
            case TRACKED -> "Tracked";
            case COMPLETED -> "Completed";
        };
    }

    private static int statusColor(QuestData.Status st) {
        return st == QuestData.Status.NEW ? 0xFFFF6B6B : GREEN;
    }

    private static String buttonLabel(QuestData.Status st) {
        return switch (st) {
            case NEW -> "Accept";
            case ACTIVE -> "Track";
            case TRACKED -> "Tracked";
            case COMPLETED -> "Complete";
        };
    }

    // =====================================================================
    // input
    // =====================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = (event.x() - ox) / s;
        double my = (event.y() - oy) / s;
        if (mx >= VX0 && mx < VX1 && my >= VY0 && my < VY1) {
            for (Btn b : btns) {
                if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
                    if (!b.details()) {
                        switch (b.quest().status) {
                            case NEW -> b.quest().status = QuestData.Status.ACTIVE;
                            case ACTIVE -> b.quest().status = QuestData.Status.TRACKED;
                            case TRACKED -> b.quest().status = QuestData.Status.ACTIVE;
                            default -> {
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - scrollY * 30));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((TswInterfaceClient.QUEST_KEY != null && TswInterfaceClient.QUEST_KEY.matches(event))
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(event))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
