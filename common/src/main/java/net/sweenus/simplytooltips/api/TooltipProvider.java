package net.sweenus.simplytooltips.api;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public interface TooltipProvider {
    boolean supports(ItemStack stack);

    ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown);
}
