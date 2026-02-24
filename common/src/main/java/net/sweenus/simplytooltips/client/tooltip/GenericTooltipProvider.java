package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

        int borderStyle = TooltipBorderStyle.DEFAULT;
        TooltipTheme theme = TooltipTheme.defaultTheme();

        if (stack.isOf(Items.IRON_SWORD)) {
            borderStyle = TooltipBorderStyle.LIGHTNING;
            theme = new TooltipTheme(
                    0xFFE6EEFF, 0xFF6A7DAA, 0xF0141D2E, 0xF00B111D,
                    0xFFF4F7FF, 0xFFDCE8FF, 0xFF0A0F18, 0xFFD4E0FF,
                    0xFFE2E9FA, 0xFF859CD6, 0xFFE6EEFF, 0xFF152036,
                    0xFF859CD6, 0xFFB8C8FF, 0xFF8DB8FF, 0xFF9FB1FF,
                    0xFFC6D8FF, 0xFF232D40, 0xFFD4DEEF
            );
        }

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
                borderStyle,
                List.of(),
                bodyLines,
                extraLines,
                theme,
                null,
                null
        );
    }
}
