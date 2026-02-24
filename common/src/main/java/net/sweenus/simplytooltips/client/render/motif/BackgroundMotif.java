package net.sweenus.simplytooltips.client.render.motif;

import net.minecraft.client.gui.DrawContext;

public interface BackgroundMotif {
    /**
     * Draw animated background particles/effects inside the tooltip panel.
     * Called every frame. Should be no-op if {@code w < 40 || h < 40}.
     */
    void draw(DrawContext context, int panelX, int panelY, int panelW, int panelH, long timeMs);
}
