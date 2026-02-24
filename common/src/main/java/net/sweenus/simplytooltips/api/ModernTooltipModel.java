package net.sweenus.simplytooltips.api;

import net.minecraft.text.Text;

import java.util.List;

public record ModernTooltipModel(
        String title,
        List<String> badges,
        int borderStyle,
        List<String> abilityLines,
        List<String> bodyLines,
        List<Text> extraLines,
        TooltipTheme theme,
        UpgradeSection upgradeSection,
        String animKeyExtra
) {}
