package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public class DeepDarkMotif implements BackgroundMotif {

    @Override
    public void draw(DrawContext context, int x, int y, int w, int h, long timeMs) {
        if (w < 38 || h < 34) return;

        int minX = x + 6, maxX = x + w - 6;
        int minY = y + 6, maxY = y + h - 6;
        int spanX = Math.max(12, maxX - minX - 4);
        int spanY = Math.max(10, maxY - minY - 4);

        for (int i = 0; i < 6; i++) {
            int sx = minX + ((i * 19 + i * i * 7) % spanX);
            int sy = minY + ((i * 13 + i * i * 11) % spanY);
            int len = 6 + (i % 4);

            for (int s = 0; s < len; s++) {
                int vx = sx + s;
                int vy = sy + (int) Math.round(Math.sin((timeMs * 0.0016) + i + s * 0.85) * 1.6);
                int vein = ((s + i) % 3 == 0) ? 0x1437A6A0 : 0x102E8985;
                context.fill(vx, vy, vx + 1, vy + 1, vein);

                if ((s + i) % 4 == 0) {
                    int branch = 0x0E53C9C4;
                    context.fill(vx, vy + 1, vx + 1, vy + 2, branch);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            int nx = minX + ((i * 31 + i * i * 5) % spanX);
            int ny = minY + ((i * 23 + i * i * 9) % spanY);
            float pulse = (float) ((Math.sin((timeMs * 0.0043) + i * 1.47) + 1.0) * 0.5);
            int coreA = 14 + Math.round(16 * pulse);
            int ringA = 9 + Math.round(11 * pulse);
            int core = (coreA << 24) | 0x0089F3ED;
            int ring = (ringA << 24) | 0x0049C7C0;

            context.fill(nx, ny, nx + 1, ny + 1, core);
            context.fill(nx - 1, ny, nx, ny + 1, ring);
            context.fill(nx + 1, ny, nx + 2, ny + 1, ring);
            context.fill(nx, ny - 1, nx + 1, ny, ring);
            context.fill(nx, ny + 1, nx + 1, ny + 2, ring);
        }
    }
}
