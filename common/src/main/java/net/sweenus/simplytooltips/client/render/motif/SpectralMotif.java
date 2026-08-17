package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class SpectralMotif implements BackgroundMotif {

    private static final int CORE = 0xEAF6FF;
    private static final int GLOW = 0x8FD4FF;
    private static final int MID  = 0x3E9BE0;
    private static final int DEEP = 0x123A5C;

    private static final int  HAZE_BANDS    = 3;
    private static final int  JAG_SEGMENT   = 6;
    private static final int  WISP_COUNT    = 12;
    private static final long WISP_CYCLE_MS = 5200L;

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 42 || h < 42) return;

        int topY = y + 4;
        int bottomY = y + h - 4;
        int centerX = x + w / 2;
        double breath = (Math.sin(timeMs * 0.0011) + 1.0) * 0.5;

        drawWisps(context, x, w, topY, bottomY, centerX, timeMs);
        drawSeam(context, centerX, topY, bottomY, x, w, breath, timeMs);
    }

    private static void drawSeam(DrawContext context, int centerX, int topY, int bottomY,
                                 int panelX, int panelW, double breath, long timeMs) {
        int span = Math.max(1, bottomY - topY);
        int topReach = clamp(span / 6, 6, 16);
        int bottomReach = clamp(span / 4, 8, 24);
        int drift = (int) Math.floor(timeMs * 0.0012);
        int minX = panelX + 3;
        int maxX = panelX + panelW - 3;

        for (int row = topY; row < bottomY; row++) {
            double presence = presence(row, topY, bottomY, topReach, bottomReach);
            if (presence <= 0.0) continue;

            int local = row - topY;
            int seamX = centerX + jagOffset(local, drift);
            int spread = (int) Math.round(presence * (2.2 + breath * 1.4));

            int coreA = (int) Math.round((58 + breath * 38) * presence);
            int glowA = (int) Math.round((26 + breath * 20) * presence);
            int midA  = (int) Math.round((12 + breath * 9) * presence);
            int hazeA = (int) Math.round((5 + breath * 3) * presence);
            if (coreA <= 0) continue;

            for (int band = 0; band < HAZE_BANDS; band++) {
                int halfW = (int) Math.round(presence * (10 - band * 3));
                context.fill(clamp(seamX - halfW, minX, maxX), row,
                        clamp(seamX + halfW + 1, minX, maxX), row + 1,
                        rgba(band == 0 ? DEEP : MID, hazeA));
            }

            context.fill(clamp(seamX - spread - 2, minX, maxX), row,
                    clamp(seamX + spread + 3, minX, maxX), row + 1, rgba(MID, midA));
            context.fill(clamp(seamX - spread, minX, maxX), row,
                    clamp(seamX + spread + 1, minX, maxX), row + 1, rgba(GLOW, glowA));
            context.fill(clamp(seamX, minX, maxX), row,
                    clamp(seamX + 1, minX, maxX), row + 1, rgba(CORE, coreA));

            drawBleed(context, seamX, row, local, minX, maxX, presence, breath, timeMs);
        }
    }

    private static void drawBleed(DrawContext context, int seamX, int row, int local,
                                  int minX, int maxX, double presence, double breath, long timeMs) {
        if (Math.floorMod(local * 31 + (int) (timeMs / 300L), 17) != 0) return;

        int alpha = (int) Math.round((20 + breath * 12) * presence);
        if (alpha <= 0) return;

        int reach = (int) Math.round(presence * (4 + Math.floorMod(local * 13, 6) + Math.round(breath * 3)));
        if (reach < 2) return;

        boolean left = (local & 1) == 0;
        int from = left ? seamX - reach : seamX + 2;
        int to   = left ? seamX - 1     : seamX + reach + 1;
        context.fill(clamp(from, minX, maxX), row, clamp(to, minX, maxX), row + 1, rgba(GLOW, alpha));
    }

    private static void drawWisps(DrawContext context, int panelX, int panelW,
                                  int topY, int bottomY, int centerX, long timeMs) {
        int spanY = Math.max(1, bottomY - topY - 2);

        for (int i = 0; i < WISP_COUNT; i++) {
            boolean left = (i & 1) == 0;
            long cycle = WISP_CYCLE_MS + i * 311L;
            float progress = Math.floorMod(timeMs + i * 617L, cycle) / (float) cycle;

            int startX = left ? panelX + 5 : panelX + panelW - 5;
            int travel = Math.abs(centerX - startX);
            int px = left
                    ? startX + Math.round(travel * progress)
                    : startX - Math.round(travel * progress);

            int py = topY + 1 + Math.floorMod(i * 29 + i * i * 11, spanY);
            py += (int) Math.round(Math.sin(timeMs * 0.0013 + i * 1.37) * 2.0);
            py = clamp(py, topY, bottomY - 1);

            int alpha = Math.round(40 * (1.0F - progress) * Math.min(1.0F, progress * 6.0F));
            if (alpha <= 0) continue;

            int tailX = left ? px - 4 : px + 1;
            context.fill(tailX, py, tailX + 4, py + 1, rgba(MID, Math.max(1, alpha / 3)));
            context.fill(px, py, px + 1, py + 1, rgba(GLOW, alpha));
        }
    }

    private static double presence(int row, int topY, int bottomY, int topReach, int bottomReach) {
        int fromTop = row - topY;
        int fromBottom = bottomY - 1 - row;
        boolean nearTop = fromTop <= fromBottom;
        int d = nearTop ? fromTop : fromBottom;
        int reach = nearTop ? topReach : bottomReach;
        if (d >= reach) return 0.0;

        double fall = 1.0 - d / (double) reach;
        return fall * fall;
    }

    private static int jagOffset(int local, int drift) {
        int node = Math.floorDiv(local, JAG_SEGMENT) + drift;
        float frac = Math.floorMod(local, JAG_SEGMENT) / (float) JAG_SEGMENT;
        int a = jagNode(node);
        int b = jagNode(node + 1);
        return Math.round(a + (b - a) * frac);
    }

    private static int jagNode(int i) {
        int h = i * 374761393 + 668265263;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        return Math.floorMod(h, 5) - 2;
    }

    private static int rgba(int rgb, int alpha) {
        int a = clamp(alpha, 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
