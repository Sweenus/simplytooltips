package net.sweenus.simplytooltips.client.render;

import net.minecraft.client.gui.DrawContext;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipTheme;

/**
 * Draws the decorative border frame and per-style border patterns.
 */
public class BorderRenderer {

    /**
     * Draws the outer border lines, inner highlight lines, corner diamonds,
     * and delegates to {@link #drawBorderPattern} for style-specific decoration.
     */
    public static void drawDecorativeBorder(DrawContext context, int x, int y, int w, int h,
                                            TooltipTheme theme, int borderStyle) {
        // Outer border lines
        context.fill(x, y, x + w, y + 1, theme.border());
        context.fill(x, y + h - 1, x + w, y + h, theme.border());
        context.fill(x, y, x + 1, y + h, theme.border());
        context.fill(x + w - 1, y, x + w, y + h, theme.border());

        // Inner highlight lines
        context.fill(x + 1, y + 1, x + w - 1, y + 2, theme.borderInner());
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, theme.borderInner());
        context.fill(x + 1, y + 1, x + 2, y + h - 1, theme.borderInner());
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, theme.borderInner());

        // Corner diamonds
        drawSmallDiamond(context, x + 6,     y,         theme.border());
        drawSmallDiamond(context, x + w - 7, y,         theme.border());
        drawSmallDiamond(context, x + 6,     y + h - 1, theme.border());
        drawSmallDiamond(context, x + w - 7, y + h - 1, theme.border());

        drawBorderPattern(context, x, y, w, h, theme, borderStyle);
    }

    /**
     * Draws style-specific pixel-art decorations along the top and bottom borders.
     */
    public static void drawBorderPattern(DrawContext context, int x, int y, int w, int h,
                                         TooltipTheme theme, int borderStyle) {
        switch (borderStyle) {
            case TooltipBorderStyle.VINE -> {
                int leafA = 0xFF79BE77, leafB = 0xFF5EA661, stem = 0xFF3E7A44;
                for (int px = x + 9, i = 0; px < x + w - 10; px += 11, i++) {
                    boolean flip = (i & 1) == 0;
                    int c = flip ? leafA : leafB;
                    context.fill(px, y + 1, px + 2, y + 2, c);
                    if (flip) { context.fill(px + 1, y + 2, px + 3, y + 3, c); }
                    else       { context.fill(px - 1, y + 2, px + 1, y + 3, c); }
                    context.fill(px, y + h - 3, px + 2, y + h - 2, c);
                    if (flip) { context.fill(px - 1, y + h - 2, px + 1, y + h - 1, c); }
                    else       { context.fill(px + 1, y + h - 2, px + 3, y + h - 1, c); }
                    if (i % 3 == 0) {
                        context.fill(px, y + 3, px + 1, y + 5, stem);
                        context.fill(px, y + h - 5, px + 1, y + h - 3, stem);
                    }
                }
            }
            case TooltipBorderStyle.BEE -> {
                int honey = 0xFFE8B847, wax = 0xFFF4D77B, outline = 0xFF6A4A1C;
                for (int px = x + 8; px < x + w - 10; px += 12) {
                    context.fill(px, y + 1, px + 3, y + 3, honey);
                    context.fill(px + 1, y, px + 2, y + 1, wax);
                    context.fill(px, y + 1, px + 1, y + 2, outline);
                    context.fill(px + 2, y + 2, px + 3, y + 3, outline);
                    context.fill(px, y + h - 3, px + 3, y + h - 1, honey);
                    context.fill(px + 1, y + h - 1, px + 2, y + h, wax);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, outline);
                    context.fill(px + 2, y + h - 3, px + 3, y + h - 2, outline);
                }
                context.fill(x + 3, y + 4, x + 5, y + 5, 0x99DDF7FF);
                context.fill(x + w - 5, y + 4, x + w - 3, y + 5, 0x99DDF7FF);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 4, 0x99DDF7FF);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 4, 0x99DDF7FF);
            }
            case TooltipBorderStyle.BLOSSOM -> {
                int petal = 0xFFF3B1D2, core = 0xFFFFF4BE;
                for (int px = x + 12; px < x + w - 12; px += 16) {
                    context.fill(px, y + 1, px + 1, y + 4, petal);
                    context.fill(px - 1, y + 2, px + 2, y + 3, petal);
                    context.fill(px, y + 2, px + 1, y + 3, core);
                    context.fill(px, y + h - 4, px + 1, y + h - 1, petal);
                    context.fill(px - 1, y + h - 3, px + 2, y + h - 2, petal);
                    context.fill(px, y + h - 3, px + 1, y + h - 2, core);
                }
            }
            case TooltipBorderStyle.BUBBLE -> {
                int crest = 0xFF8EE7F8, foam = 0xFFC8F7FF;
                for (int px = x + 6, step = 0; px < x + w - 6; px += 8, step++) {
                    int dy = (step % 2 == 0) ? 0 : 1;
                    context.fill(px, y + 1 + dy, px + 5, y + 2 + dy, crest);
                    context.fill(px, y + h - 2 - dy, px + 5, y + h - 1 - dy, crest);
                }
                for (int px = x + 12; px < x + w - 12; px += 18) {
                    context.fill(px, y + 2, px + 2, y + 4, foam);
                    context.fill(px + 1, y + 3, px + 3, y + 5, crest);
                    context.fill(px, y + h - 5, px + 2, y + h - 3, foam);
                    context.fill(px + 1, y + h - 4, px + 3, y + h - 2, crest);
                }
            }
            case TooltipBorderStyle.EARTH -> {
                int rockDark = TooltipPainter.lerpColor(theme.border(), 0xFF3A2E22, 0.55f);
                int rockMid  = TooltipPainter.lerpColor(theme.border(), 0xFF6E5A40, 0.32f);
                int dust     = TooltipPainter.lerpColor(theme.borderInner(), 0xFFCAB28D, 0.22f);
                for (int px = x + 8, i = 0; px < x + w - 9; px += 10, i++) {
                    int wChunk = (i % 3 == 0) ? 3 : 2;
                    context.fill(px, y + 1, px + wChunk, y + 2, rockMid);
                    context.fill(px + 1, y + 2, px + wChunk + 1, y + 3, rockDark);
                    context.fill(px, y + h - 3, px + wChunk, y + h - 2, rockMid);
                    context.fill(px - 1, y + h - 2, px + wChunk - 1, y + h - 1, rockDark);
                }
                for (int px = x + 14; px < x + w - 14; px += 18) {
                    context.fill(px, y + 2, px + 1, y + 4, dust);
                    context.fill(px + 1, y + 3, px + 2, y + 4, rockDark);
                    context.fill(px, y + h - 4, px + 1, y + h - 2, dust);
                    context.fill(px - 1, y + h - 3, px, y + h - 2, rockDark);
                }
                context.fill(x + 3, y + 3, x + 5, y + 5, rockMid);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 5, rockMid);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 3, rockMid);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, rockMid);
            }
            case TooltipBorderStyle.ECHO -> {
                int runeA = 0xFFB59AFF, runeB = 0xFF7C67D9;
                for (int px = x + 10, i = 0; px < x + w - 10; px += 16, i++) {
                    if ((i & 1) == 0) {
                        context.fill(px, y + 1, px + 1, y + 4, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 2, runeB);
                        context.fill(px + 1, y + 3, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 3, runeB);
                        context.fill(px + 1, y + h - 2, px + 2, y + h - 1, runeB);
                    } else {
                        context.fill(px, y + 2, px + 3, y + 3, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                        context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
                    }
                }
            }
            case TooltipBorderStyle.ICE -> {
                int ice = 0xFFBFE9FF;
                for (int px = x + 10; px < x + w - 10; px += 12) {
                    context.fill(px, y, px + 1, y + 3, ice);
                    context.fill(px - 1, y + 1, px + 2, y + 2, ice);
                    context.fill(px, y + h - 3, px + 1, y + h, ice);
                    context.fill(px - 1, y + h - 2, px + 2, y + h - 1, ice);
                }
            }
            case TooltipBorderStyle.LIGHTNING -> {
                int boltA = 0xFFE7F1FF, boltB = 0xFF9CB8F8, spark = 0xFF74A8FF;
                for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
                    if ((i & 1) == 0) {
                        context.fill(px, y + 1, px + 1, y + 3, boltA);
                        context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                        context.fill(px + 2, y + 2, px + 4, y + 3, spark);
                        context.fill(px, y + h - 4, px + 1, y + h - 2, boltA);
                        context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                        context.fill(px + 2, y + h - 4, px + 4, y + h - 3, spark);
                    } else {
                        context.fill(px + 2, y + 1, px + 3, y + 3, boltA);
                        context.fill(px + 1, y + 2, px + 2, y + 4, boltB);
                        context.fill(px, y + 2, px + 1, y + 3, spark);
                        context.fill(px + 2, y + h - 4, px + 3, y + h - 2, boltA);
                        context.fill(px + 1, y + h - 5, px + 2, y + h - 3, boltB);
                        context.fill(px, y + h - 4, px + 1, y + h - 3, spark);
                    }
                }
                context.fill(x + 3, y + 3, x + 5, y + 4, spark);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, spark);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, spark);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, spark);
            }
            case TooltipBorderStyle.EMBER -> {
                int emberA = 0xFFFF8A4A;
                int emberB = 0xFFE3522E;
                int hot    = 0xFFFFC178;
                int coal   = 0xFF5E1D16;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
                    boolean offset = (i & 1) == 0;

                    // Top flame tongues
                    context.fill(px, y + 1, px + 2, y + 2, emberA);
                    context.fill(px + 1, y + 2, px + 3, y + 3, emberB);
                    if (offset) {
                        context.fill(px + 1, y, px + 2, y + 1, hot);
                    } else {
                        context.fill(px, y, px + 1, y + 1, hot);
                    }

                    // Bottom mirrored cinders
                    context.fill(px, y + h - 3, px + 2, y + h - 2, emberA);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, emberB);
                    if (offset) {
                        context.fill(px, y + h - 1, px + 1, y + h, hot);
                    } else {
                        context.fill(px + 1, y + h - 1, px + 2, y + h, hot);
                    }

                    // Dark coal accent between motifs
                    if (i % 3 == 1) {
                        context.fill(px + 3, y + 1, px + 4, y + 2, coal);
                        context.fill(px - 2, y + h - 2, px - 1, y + h - 1, coal);
                    }
                }

                // Corner spark accents
                context.fill(x + 3, y + 3, x + 5, y + 4, hot);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, hot);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, hot);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, hot);
            }
            case TooltipBorderStyle.ENCHANTED -> {
                int runeA  = 0xFFD4B7FF;
                int runeB  = 0xFF9D73E8;
                int sigil  = 0xFF6D4AC7;
                int spark  = 0xFFE8DBFF;

                for (int px = x + 10, i = 0; px < x + w - 10; px += 14, i++) {
                    if ((i & 1) == 0) {
                        context.fill(px, y + 1, px + 1, y + 4, runeA);
                        context.fill(px + 1, y + 2, px + 3, y + 3, runeB);
                        context.fill(px + 1, y + 1, px + 2, y + 2, spark);

                        context.fill(px, y + h - 4, px + 1, y + h - 1, runeA);
                        context.fill(px - 1, y + h - 3, px + 1, y + h - 2, runeB);
                        context.fill(px, y + h - 2, px + 1, y + h - 1, spark);
                    } else {
                        context.fill(px, y + 2, px + 3, y + 3, runeA);
                        context.fill(px + 1, y + 1, px + 2, y + 4, runeB);
                        context.fill(px + 1, y + 2, px + 2, y + 3, sigil);

                        context.fill(px, y + h - 3, px + 3, y + h - 2, runeA);
                        context.fill(px + 1, y + h - 4, px + 2, y + h - 1, runeB);
                        context.fill(px + 1, y + h - 3, px + 2, y + h - 2, sigil);
                    }
                }

                context.fill(x + 3, y + 3, x + 5, y + 5, spark);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 5, spark);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 3, spark);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, spark);
            }
            case TooltipBorderStyle.AUTUMN -> {
                int leafA  = 0xFFE5A24F;
                int leafB  = 0xFFD67B35;
                int vein   = 0xFF8A4A26;
                int twig   = 0xFF6A3D24;
                int acorn  = 0xFFC18B54;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 13, i++) {
                    boolean flip = (i & 1) == 0;
                    int leaf = flip ? leafA : leafB;

                    // Top drifting leaf motif
                    context.fill(px, y + 1, px + 2, y + 2, leaf);
                    context.fill(px + 1, y + 2, px + 3, y + 3, leaf);
                    context.fill(px + 1, y + 1, px + 2, y + 2, vein);
                    if (flip) context.fill(px - 1, y + 2, px, y + 3, twig);
                    else      context.fill(px + 3, y + 2, px + 4, y + 3, twig);

                    // Bottom mirrored motif
                    context.fill(px, y + h - 3, px + 2, y + h - 2, leaf);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, leaf);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, vein);
                    if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, twig);
                    else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, twig);

                    // occasional acorn knot accent
                    if ((i % 3) == 1) {
                        context.fill(px + 4, y + 1, px + 5, y + 2, acorn);
                        context.fill(px - 3, y + h - 2, px - 2, y + h - 1, acorn);
                    }
                }

                // Corner twig accents
                context.fill(x + 3, y + 3, x + 5, y + 4, twig);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, twig);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, twig);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, twig);
            }
            case TooltipBorderStyle.SOUL -> {
                int wispA = 0xFF8EFFF5;
                int wispB = 0xFF52D7CA;
                int core  = 0xFFD8FFFB;
                int ash   = 0xFF1E3436;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 13, i++) {
                    boolean flip = (i & 1) == 0;

                    context.fill(px, y + 1, px + 2, y + 2, wispA);
                    context.fill(px + 1, y + 2, px + 3, y + 3, wispB);
                    context.fill(px + 1, y + 1, px + 2, y + 2, core);
                    if (flip) context.fill(px - 1, y + 2, px, y + 3, ash);
                    else      context.fill(px + 3, y + 2, px + 4, y + 3, ash);

                    context.fill(px, y + h - 3, px + 2, y + h - 2, wispA);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, wispB);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, core);
                    if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, ash);
                    else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, ash);
                }

                context.fill(x + 3, y + 3, x + 5, y + 4, core);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, core);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, core);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, core);
            }
            case TooltipBorderStyle.DEEP_DARK -> {
                int veinA = 0xFF2E8985;
                int veinB = 0xFF235F5B;
                int glow  = 0xFF66D7CD;
                int ink   = 0xFF142327;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
                    boolean flip = (i & 1) == 0;

                    context.fill(px, y + 1, px + 2, y + 2, veinA);
                    context.fill(px + 1, y + 2, px + 3, y + 3, veinB);
                    context.fill(px + 1, y + 1, px + 2, y + 2, glow);
                    if (flip) context.fill(px - 1, y + 2, px, y + 3, ink);
                    else      context.fill(px + 3, y + 2, px + 4, y + 3, ink);

                    context.fill(px, y + h - 3, px + 2, y + h - 2, veinA);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, veinB);
                    context.fill(px, y + h - 2, px + 1, y + h - 1, glow);
                    if (flip) context.fill(px + 2, y + h - 2, px + 3, y + h - 1, ink);
                    else      context.fill(px - 2, y + h - 2, px - 1, y + h - 1, ink);
                }

                context.fill(x + 3, y + 3, x + 5, y + 5, glow);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 5, glow);
                context.fill(x + 3, y + h - 5, x + 5, y + h - 3, glow);
                context.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, glow);
            }
            case TooltipBorderStyle.POISON -> {
                int oozeA = 0xFF6AB64A;
                int oozeB = 0xFF3C8A34;
                int toxic = 0xFFA5F06E;
                int dark  = 0xFF223A1E;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
                    boolean flip = (i & 1) == 0;

                    // Top ooze drips
                    context.fill(px, y + 1, px + 2, y + 2, oozeA);
                    context.fill(px + 1, y + 2, px + 3, y + 3, oozeB);
                    if (flip) context.fill(px + 1, y + 3, px + 2, y + 4, toxic);
                    else      context.fill(px, y + 3, px + 1, y + 4, toxic);

                    // Bottom mirrored spores
                    context.fill(px, y + h - 3, px + 2, y + h - 2, oozeA);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, oozeB);
                    if (flip) context.fill(px, y + h - 4, px + 1, y + h - 3, toxic);
                    else      context.fill(px + 1, y + h - 4, px + 2, y + h - 3, toxic);

                    if ((i % 3) == 1) {
                        context.fill(px + 3, y + 1, px + 4, y + 2, dark);
                        context.fill(px - 2, y + h - 2, px - 1, y + h - 1, dark);
                    }
                }

                context.fill(x + 3, y + 3, x + 5, y + 4, toxic);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, toxic);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, toxic);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, toxic);
            }
            case TooltipBorderStyle.OCEAN -> {
                int emberA = 0xFF3A662F;
                int emberB = 0xFF264A24;
                int hot = 0xFF5D8E49;
                int coal = 0xFF132013;

                for (int px = x + 9, i = 0; px < x + w - 10; px += 12, i++) {
                    boolean flip = (i & 1) == 0;

                    // Top tongues
                    context.fill(px, y + 1, px + 2, y + 2, emberA);
                    context.fill(px + 1, y + 2, px + 3, y + 3, emberB);
                    if (flip) context.fill(px + 1, y, px + 2, y + 1, hot);
                    else      context.fill(px, y, px + 1, y + 1, hot);

                    // Bottom cinders
                    context.fill(px, y + h - 3, px + 2, y + h - 2, emberA);
                    context.fill(px - 1, y + h - 2, px + 1, y + h - 1, emberB);
                    if (flip) context.fill(px, y + h - 1, px + 1, y + h, hot);
                    else      context.fill(px + 1, y + h - 1, px + 2, y + h, hot);

                    if ((i % 3) == 1) {
                        context.fill(px + 3, y + 1, px + 4, y + 2, coal);
                        context.fill(px - 2, y + h - 2, px - 1, y + h - 1, coal);
                    }
                }

                context.fill(x + 3, y + 3, x + 5, y + 4, hot);
                context.fill(x + w - 5, y + 3, x + w - 3, y + 4, hot);
                context.fill(x + 3, y + h - 4, x + 5, y + h - 3, hot);
                context.fill(x + w - 5, y + h - 4, x + w - 3, y + h - 3, hot);
            }
            default -> {}
        }
    }

    /** 3×3 diamond centered at (cx, cy). */
    public static void drawSmallDiamond(DrawContext context, int cx, int cy, int color) {
        context.fill(cx, cy - 1, cx + 1, cy, color);
        context.fill(cx - 1, cy, cx + 2, cy + 1, color);
        context.fill(cx, cy + 1, cx + 1, cy + 2, color);
    }

    private BorderRenderer() {}
}
