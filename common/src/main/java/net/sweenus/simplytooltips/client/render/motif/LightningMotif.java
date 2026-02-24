package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class LightningMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 40 || h < 40) return;
        int minX = x + 6, maxX = x + w - 6, minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(16, maxX - minX - 8);
        int travel = Math.max(14, maxY - minY - 8);
        int maxBoltDepth = 120;
        int safeTravel = Math.max(1, travel - maxBoltDepth);
        final long cycleMs = 3200L;
        final long activeMs = 1200L;
        final int forkStep = 7;

        for (int i = 0; i < 2; i++) {
            long cycleIndex = timeMs / cycleMs;
            long jitter = Math.floorMod((cycleIndex * 197L) + (i * 521L), cycleMs / 2);
            long offset = i * 113L + jitter;
            long phase = Math.floorMod(timeMs + offset, cycleMs);
            if (phase >= activeMs) continue;

            float t = phase / (float) activeMs;
            float fade = t < 0.12F
                    ? (t / 0.12F)
                    : Math.max(0.0F, 1.0F - ((t - 0.12F) / 0.88F));

            long strikeIndex = (timeMs + offset) / cycleMs;
            int seed = (int) (strikeIndex * 1103515245L + i * 12345L + 0x6E5F3A1D);
            int rand0 = seed;
            int rand1 = seed * 1664525 + 1013904223;
            int rand2 = rand1 * 1664525 + 1013904223;
            int rand3 = rand2 * 1664525 + 1013904223;

            int lane = Math.floorMod(rand0, spanX);
            int y0 = minY + Math.floorMod(rand1, safeTravel);
            int x0 = Math.max(minX + 1, Math.min(maxX - 5, minX + lane));

            int s1 = 4 + Math.floorMod(rand2, 3);
            int s2 = 4 + Math.floorMod(rand2 >>> 3, 3);
            int s3 = 4 + Math.floorMod(rand2 >>> 6, 3);
            int s4 = 4 + Math.floorMod(rand2 >>> 9, 3);
            int s5 = 4 + Math.floorMod(rand2 >>> 12, 3);
            int s6 = 4 + Math.floorMod(rand2 >>> 15, 3);

            int d1 = Math.floorMod(rand3, 9) - 4;
            int d2 = Math.floorMod(rand3 >>> 2, 9) - 4;
            int d3 = Math.floorMod(rand3 >>> 4, 9) - 4;
            int d4 = Math.floorMod(rand3 >>> 6, 9) - 4;
            int d5 = Math.floorMod(rand3 >>> 8, 9) - 4;
            int d6 = Math.floorMod(rand3 >>> 10, 9) - 4;
            int nonZero = (d1 != 0 ? 1 : 0) + (d2 != 0 ? 1 : 0) + (d3 != 0 ? 1 : 0)
                    + (d4 != 0 ? 1 : 0) + (d5 != 0 ? 1 : 0) + (d6 != 0 ? 1 : 0);
            int diagonalPower = Math.abs(d1) + Math.abs(d2) + Math.abs(d3) + Math.abs(d4) + Math.abs(d5) + Math.abs(d6);
            if (nonZero < 5 || diagonalPower < 18) {
                d1 = 4; d2 = -4; d3 = 3; d4 = -3; d5 = 4; d6 = -4;
            }

            int x1 = x0 + d1; int y1 = Math.min(maxY, y0 + s1);
            int x2 = x1 + d2; int y2 = Math.min(maxY, y1 + s2);
            int x3 = x2 + d3; int y3 = Math.min(maxY, y2 + s3);
            int x4 = x3 + d4; int y4 = Math.min(maxY, y3 + s4);
            int x5 = x4 + d5; int y5 = Math.min(maxY, y4 + s5);
            int x6 = x5 + d6; int y6 = Math.min(maxY, y5 + s6);

            int trunkA  = (int) (0x22 * fade);
            int branchA = (int) (0x16 * fade);
            int sparkA  = (int) (0x36 * fade);
            int trunkColor  = (trunkA  << 24) | 0x00DFF0FF;
            int branchColor = (branchA << 24) | 0x00AFCCFF;
            int sparkColor  = (sparkA  << 24) | 0x00FFFFFF;

            // Trunk
            context.fill(x0, y0, x0 + 1, y1, trunkColor);
            context.fill(Math.min(x1, x0), y1 - 1, Math.max(x1, x0) + 1, y1, trunkColor);
            context.fill(x1, y1, x1 + 1, y2, trunkColor);
            context.fill(Math.min(x2, x1), y2 - 1, Math.max(x2, x1) + 1, y2, trunkColor);
            context.fill(x2, y2, x2 + 1, y3, trunkColor);
            context.fill(Math.min(x3, x2), y3 - 1, Math.max(x3, x2) + 1, y3, trunkColor);
            context.fill(x3, y3, x3 + 1, y4, trunkColor);
            context.fill(Math.min(x4, x3), y4 - 1, Math.max(x4, x3) + 1, y4, trunkColor);
            context.fill(x4, y4, x4 + 1, y5, trunkColor);
            context.fill(Math.min(x5, x4), y5 - 1, Math.max(x5, x4) + 1, y5, trunkColor);
            context.fill(x5, y5, x5 + 1, y6, trunkColor);

            // Fork direction seeds
            int forkDirA0 = Math.floorMod(rand3 >>> 12, 9) - 4;
            int forkDirA1 = Math.floorMod(rand3 >>> 14, 9) - 4;
            int forkDirA2 = Math.floorMod(rand3 >>> 16, 9) - 4;
            int forkDirA3 = Math.floorMod(rand3 >>> 6,  9) - 4;
            int forkDirA4 = Math.floorMod(rand3 >>> 8,  9) - 4;
            int forkDirB0 = Math.floorMod(rand3 >>> 18, 9) - 4;
            int forkDirB1 = Math.floorMod(rand3 >>> 20, 9) - 4;
            int forkDirB2 = Math.floorMod(rand3 >>> 22, 9) - 4;
            int forkDirB3 = Math.floorMod(rand3 >>> 9,  9) - 4;
            int forkDirB4 = Math.floorMod(rand3 >>> 11, 9) - 4;
            if (forkDirA0 == 0) forkDirA0 = ((rand3 >>> 24) & 1) == 0 ? -1 : 1;
            if (forkDirB0 == 0) forkDirB0 = ((rand3 >>> 25) & 1) == 0 ? -1 : 1;
            int subForkDirA0 = Math.floorMod(rand3 >>> 26, 9) - 4;
            int subForkDirA1 = Math.floorMod(rand3 >>> 28, 9) - 4;
            int subForkDirA2 = Math.floorMod(rand2 >>> 17, 9) - 4;
            int subForkDirA3 = Math.floorMod(rand2 >>> 19, 9) - 4;
            int subForkDirB0 = Math.floorMod(rand2 >>> 1,  9) - 4;
            int subForkDirB1 = Math.floorMod(rand2 >>> 3,  9) - 4;
            int subForkDirB2 = Math.floorMod(rand2 >>> 5,  9) - 4;
            int subForkDirB3 = Math.floorMod(rand2 >>> 7,  9) - 4;
            int sweepA0 = Math.floorMod(rand0 >>> 6,  5) - 2;
            int sweepA1 = Math.floorMod(rand0 >>> 8,  5) - 2;
            int sweepA2 = Math.floorMod(rand0 >>> 10, 5) - 2;
            int sweepA3 = Math.floorMod(rand0 >>> 12, 5) - 2;
            int sweepA4 = Math.floorMod(rand0 >>> 14, 5) - 2;
            int sweepB0 = Math.floorMod(rand1 >>> 6,  5) - 2;
            int sweepB1 = Math.floorMod(rand1 >>> 8,  5) - 2;
            int sweepB2 = Math.floorMod(rand1 >>> 10, 5) - 2;
            int sweepB3 = Math.floorMod(rand1 >>> 12, 5) - 2;
            int sweepB4 = Math.floorMod(rand1 >>> 14, 5) - 2;
            if (subForkDirA0 == 0) subForkDirA0 = 1;
            if (subForkDirB0 == 0) subForkDirB0 = -1;

            // Primary branch A
            float aStart = 0.04F, aEnd = 0.98F;
            float aLife = t <= aStart ? 0.0F : Math.max(0.0F, (aEnd - t) / (aEnd - aStart));
            if (aLife > 0.0F) {
                int aAlpha = (int) (branchA * aLife);
                int aColor = (aAlpha << 24) | 0x00A5C7FF;
                int bx1 = Math.max(minX, Math.min(maxX - 1, x1 + forkDirA0 + sweepA0));
                int by1 = Math.min(maxY, y1 + forkStep);
                int bx2 = Math.max(minX, Math.min(maxX - 1, bx1 + forkDirA1 + sweepA1));
                int by2 = Math.min(maxY, by1 + forkStep);
                int bx3 = Math.max(minX, Math.min(maxX - 1, bx2 + forkDirA2 + sweepA2));
                int by3 = Math.min(maxY, by2 + forkStep);
                int bx4 = Math.max(minX, Math.min(maxX - 1, bx3 + forkDirA3 + sweepA3));
                int by4 = Math.min(maxY, by3 + forkStep);
                int bx5 = Math.max(minX, Math.min(maxX - 1, bx4 + forkDirA4 + sweepA4));
                int by5 = Math.min(maxY, by4 + forkStep);
                context.fill(x1, y1, x1 + 1, by1, aColor);
                context.fill(Math.min(bx1, x1), by1 - 1, Math.max(bx1, x1) + 1, by1, aColor);
                context.fill(bx1, by1, bx1 + 1, by2, aColor);
                context.fill(Math.min(bx2, bx1), by2 - 1, Math.max(bx2, bx1) + 1, by2, aColor);
                context.fill(bx2, by2, bx2 + 1, by3, aColor);
                context.fill(Math.min(bx3, bx2), by3 - 1, Math.max(bx3, bx2) + 1, by3, aColor);
                context.fill(bx3, by3, bx3 + 1, by4, aColor);
                context.fill(Math.min(bx4, bx3), by4 - 1, Math.max(bx4, bx3) + 1, by4, aColor);
                context.fill(bx4, by4, bx4 + 1, by5, aColor);

                // Secondary fork from branch A
                float aaStart = 0.08F, aaEnd = 0.92F;
                float aaLife = t <= aaStart ? 0.0F : Math.max(0.0F, (aaEnd - t) / (aaEnd - aaStart));
                if (aaLife > 0.0F) {
                    int aaAlpha = (int) (branchA * 0.95F * aaLife);
                    int aaColor = (aaAlpha << 24) | 0x0097BEFF;
                    int ab1x = Math.max(minX, Math.min(maxX - 1, bx2 + subForkDirA0 + sweepA1));
                    int ab1y = Math.min(maxY, by2 + forkStep);
                    int ab2x = Math.max(minX, Math.min(maxX - 1, ab1x + subForkDirA1 + sweepA2));
                    int ab2y = Math.min(maxY, ab1y + forkStep);
                    int ab3x = Math.max(minX, Math.min(maxX - 1, ab2x + subForkDirA2 + sweepA3));
                    int ab3y = Math.min(maxY, ab2y + forkStep);
                    int ab4x = Math.max(minX, Math.min(maxX - 1, ab3x + subForkDirA3 + sweepA4));
                    int ab4y = Math.min(maxY, ab3y + forkStep);
                    context.fill(bx2, by2, bx2 + 1, ab1y, aaColor);
                    context.fill(Math.min(ab1x, bx2), ab1y - 1, Math.max(ab1x, bx2) + 1, ab1y, aaColor);
                    context.fill(ab1x, ab1y, ab1x + 1, ab2y, aaColor);
                    context.fill(Math.min(ab2x, ab1x), ab2y - 1, Math.max(ab2x, ab1x) + 1, ab2y, aaColor);
                    context.fill(ab2x, ab2y, ab2x + 1, ab3y, aaColor);
                    context.fill(Math.min(ab3x, ab2x), ab3y - 1, Math.max(ab3x, ab2x) + 1, ab3y, aaColor);
                    context.fill(ab3x, ab3y, ab3x + 1, ab4y, aaColor);

                    // Tertiary fork from secondary A
                    float aaaStart = 0.18F, aaaEnd = 0.84F;
                    float aaaLife = t <= aaaStart ? 0.0F : Math.max(0.0F, (aaaEnd - t) / (aaaEnd - aaaStart));
                    if (aaaLife > 0.0F) {
                        int aaaAlpha = (int) (branchA * 0.72F * aaaLife);
                        int aaaColor = (aaaAlpha << 24) | 0x008CB6FF;
                        int ac1x = Math.max(minX, Math.min(maxX - 1, ab2x + (subForkDirA0 == 0 ? 1 : subForkDirA0) + sweepA0));
                        int ac1y = Math.min(maxY, ab2y + forkStep);
                        int ac2x = Math.max(minX, Math.min(maxX - 1, ac1x + (subForkDirA1 == 0 ? -1 : subForkDirA1) + sweepA1));
                        int ac2y = Math.min(maxY, ac1y + forkStep);
                        context.fill(ab2x, ab2y, ab2x + 1, ac1y, aaaColor);
                        context.fill(Math.min(ac1x, ab2x), ac1y - 1, Math.max(ac1x, ab2x) + 1, ac1y, aaaColor);
                        context.fill(ac1x, ac1y, ac1x + 1, ac2y, aaaColor);
                        context.fill(ac2x - 1, ac2y, ac2x + 1, ac2y + 1, sparkColor);
                    }
                    context.fill(ab4x - 1, ab4y, ab4x + 1, ab4y + 1, sparkColor);
                }
                context.fill(bx5 - 1, by5, bx5 + 1, by5 + 1, sparkColor);
            }

            // Primary branch B (spawns conditionally)
            boolean spawnBranchB = ((rand0 >>> 4) & 7) == 0;
            float bStart = 0.12F, bEnd = 0.98F;
            float bLife = t <= bStart ? 0.0F : Math.max(0.0F, (bEnd - t) / (bEnd - bStart));
            if (spawnBranchB && bLife > 0.0F) {
                int bAlpha = (int) (branchA * bLife);
                int bColor = (bAlpha << 24) | 0x00A5C7FF;
                int cx1 = Math.max(minX, Math.min(maxX - 1, x3 + forkDirB0 + sweepB0));
                int cy1 = Math.min(maxY, y4 + forkStep);
                int cx2 = Math.max(minX, Math.min(maxX - 1, cx1 + forkDirB1 + sweepB1));
                int cy2 = Math.min(maxY, cy1 + forkStep);
                int cx3 = Math.max(minX, Math.min(maxX - 1, cx2 + forkDirB2 + sweepB2));
                int cy3 = Math.min(maxY, cy2 + forkStep);
                int cx4 = Math.max(minX, Math.min(maxX - 1, cx3 + forkDirB3 + sweepB3));
                int cy4 = Math.min(maxY, cy3 + forkStep);
                int cx5 = Math.max(minX, Math.min(maxX - 1, cx4 + forkDirB4 + sweepB4));
                int cy5 = Math.min(maxY, cy4 + forkStep);
                context.fill(x4, y4, x4 + 1, cy1, bColor);
                context.fill(Math.min(cx1, x4), cy1 - 1, Math.max(cx1, x4) + 1, cy1, bColor);
                context.fill(cx1, cy1, cx1 + 1, cy2, bColor);
                context.fill(Math.min(cx2, cx1), cy2 - 1, Math.max(cx2, cx1) + 1, cy2, bColor);
                context.fill(cx2, cy2, cx2 + 1, cy3, bColor);
                context.fill(Math.min(cx3, cx2), cy3 - 1, Math.max(cx3, cx2) + 1, cy3, bColor);
                context.fill(cx3, cy3, cx3 + 1, cy4, bColor);
                context.fill(Math.min(cx4, cx3), cy4 - 1, Math.max(cx4, cx3) + 1, cy4, bColor);
                context.fill(cx4, cy4, cx4 + 1, cy5, bColor);

                // Secondary fork from branch B
                float bbStart = 0.16F, bbEnd = 0.94F;
                float bbLife = t <= bbStart ? 0.0F : Math.max(0.0F, (bbEnd - t) / (bbEnd - bbStart));
                if (bbLife > 0.0F) {
                    int bbAlpha = (int) (branchA * 0.88F * bbLife);
                    int bbColor = (bbAlpha << 24) | 0x0090B6FF;
                    int cb1x = Math.max(minX, Math.min(maxX - 1, cx2 + subForkDirB0 + sweepB1));
                    int cb1y = Math.min(maxY, cy2 + forkStep);
                    int cb2x = Math.max(minX, Math.min(maxX - 1, cb1x + subForkDirB1 + sweepB2));
                    int cb2y = Math.min(maxY, cb1y + forkStep);
                    int cb3x = Math.max(minX, Math.min(maxX - 1, cb2x + subForkDirB2 + sweepB3));
                    int cb3y = Math.min(maxY, cb2y + forkStep);
                    int cb4x = Math.max(minX, Math.min(maxX - 1, cb3x + subForkDirB3 + sweepB4));
                    int cb4y = Math.min(maxY, cb3y + forkStep);
                    context.fill(cx2, cy2, cx2 + 1, cb1y, bbColor);
                    context.fill(Math.min(cb1x, cx2), cb1y - 1, Math.max(cb1x, cx2) + 1, cb1y, bbColor);
                    context.fill(cb1x, cb1y, cb1x + 1, cb2y, bbColor);
                    context.fill(Math.min(cb2x, cb1x), cb2y - 1, Math.max(cb2x, cb1x) + 1, cb2y, bbColor);
                    context.fill(cb2x, cb2y, cb2x + 1, cb3y, bbColor);
                    context.fill(Math.min(cb3x, cb2x), cb3y - 1, Math.max(cb3x, cb2x) + 1, cb3y, bbColor);
                    context.fill(cb3x, cb3y, cb3x + 1, cb4y, bbColor);

                    // Tertiary fork from secondary B
                    float bbbStart = 0.24F, bbbEnd = 0.88F;
                    float bbbLife = t <= bbbStart ? 0.0F : Math.max(0.0F, (bbbEnd - t) / (bbbEnd - bbbStart));
                    if (bbbLife > 0.0F) {
                        int bbbAlpha = (int) (branchA * 0.68F * bbbLife);
                        int bbbColor = (bbbAlpha << 24) | 0x0083AEFF;
                        int cc1x = Math.max(minX, Math.min(maxX - 1, cb2x + (subForkDirB0 == 0 ? -1 : subForkDirB0) + sweepB0));
                        int cc1y = Math.min(maxY, cb2y + forkStep);
                        int cc2x = Math.max(minX, Math.min(maxX - 1, cc1x + (subForkDirB1 == 0 ? 1 : subForkDirB1) + sweepB1));
                        int cc2y = Math.min(maxY, cc1y + forkStep);
                        context.fill(cb2x, cb2y, cb2x + 1, cc1y, bbbColor);
                        context.fill(Math.min(cc1x, cb2x), cc1y - 1, Math.max(cc1x, cb2x) + 1, cc1y, bbbColor);
                        context.fill(cc1x, cc1y, cc1x + 1, cc2y, bbbColor);
                        context.fill(cc2x - 1, cc2y, cc2x + 1, cc2y + 1, sparkColor);
                    }
                    context.fill(cb4x - 1, cb4y, cb4x + 1, cb4y + 1, sparkColor);
                }
                context.fill(cx5 - 1, cy5, cx5 + 1, cy5 + 1, sparkColor);
            }

            context.fill(x6 - 1, y6, x6 + 1, y6 + 1, sparkColor);
        }
    }
}
