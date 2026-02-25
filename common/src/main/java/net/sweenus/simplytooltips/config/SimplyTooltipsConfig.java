package net.sweenus.simplytooltips.config;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.util.Identifier;

public class SimplyTooltipsConfig extends Config {

    public static SimplyTooltipsConfig INSTANCE =
            ConfigApiJava.registerAndLoadConfig(SimplyTooltipsConfig::new);

    public SimplyTooltipsConfig() {
        super(Identifier.of("simplytooltips", "config"));
    }

    public GeneralSection   general   = new GeneralSection();
    public LayoutSection    layout    = new LayoutSection();
    public AnimationSection animation = new AnimationSection();

    public static class GeneralSection extends ConfigSection {
        /** Enable mouse-wheel scrolling when tooltip body content exceeds the viewport cap. */
        public ValidatedBoolean scrollableTooltip = new ValidatedBoolean(true);
        /** Split tooltip content into LORE / FORGE / STATS tabs; cycle with the keybind (default G). */
        public ValidatedBoolean tooltipTabs = new ValidatedBoolean(true);
    }

    public static class LayoutSection extends ConfigSection {
        /** Inner padding (px) around tooltip content on all sides. */
        public ValidatedInt padding = new ValidatedInt(10, 24, 4);
        /** Extra vertical space (px) added between text lines. */
        public ValidatedInt lineSpacing = new ValidatedInt(1, 6, 0);
        /** Maximum text width (px) before body lines wrap. */
        public ValidatedInt maxTextWidth = new ValidatedInt(200, 400, 80);
        /** Maximum body viewport height (px) before the scrollbar activates (~17 lines at default). */
        public ValidatedInt maxBodyHeight = new ValidatedInt(160, 400, 40);
    }

    public static class AnimationSection extends ConfigSection {
        /** Milliseconds of no-tooltip before the entry animation resets. */
        public ValidatedInt animationResetDelayMs = new ValidatedInt(180, 2000, 0);
    }
}
