package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.List;

public final class GenericTooltipProvider implements TooltipProvider {

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        List<String> bodyLines = new ArrayList<>();
        List<Text> extraLines = new ArrayList<>();

        // Lines before the first blank line → body; lines after → extras (enchantments etc.)
        boolean pastBlank = false;
        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString().trim();
            if (!pastBlank && s.isEmpty()) {
                pastBlank = true;
                continue;
            }
            if (pastBlank) {
                extraLines.add(rawLines.get(i));
            } else {
                bodyLines.add(rawLines.get(i).getString());
            }
        }

        return new ModernTooltipModel(
                title,
                List.of("ITEM"),
                TooltipBorderStyle.DEFAULT,
                List.of(),
                bodyLines,
                extraLines,
                TooltipTheme.defaultTheme(),
                null,
                null,
                null,  // themeKey=null: TooltipRenderer handles item/tag/rarity resolution
                null   // hint=null
        );
    }
}
