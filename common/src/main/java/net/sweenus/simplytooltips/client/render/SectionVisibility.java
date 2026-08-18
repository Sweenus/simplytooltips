package net.sweenus.simplytooltips.client.render;

/**
 * Which body sections a tooltip render should draw.
 */
public record SectionVisibility(boolean tabsActive, boolean lore, boolean forge,
                                boolean stats, boolean affixes) {

    public static SectionVisibility of(boolean exportMode, boolean previewMode, boolean tabsEnabled,
                                       int availableTabCount, boolean globalMultiTab,
                                       TabState.Tab active) {
        boolean tabsActive = !exportMode && tabsEnabled
                && (previewMode ? availableTabCount >= 2 : globalMultiTab);

        return new SectionVisibility(
                tabsActive,
                exportMode || !tabsActive || active == TabState.Tab.LORE,
                !exportMode && (!tabsActive || active == TabState.Tab.FORGE),
                !exportMode && (!tabsActive || active == TabState.Tab.STATS),
                !exportMode && (!tabsActive || active == TabState.Tab.AFFIXES));
    }
}
