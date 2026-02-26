package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class GenericTooltipProvider implements TooltipProvider {

    private static final String MAINHAND_KEY = "item.modifiers.mainhand";
    private static final String SLOT_HEADER_PREFIX = "item.modifiers.";
    private static final String ATTRIBUTE_MODIFIER_PREFIX = "attribute.modifier.";
    private static final String ATTACK_DAMAGE_KEY = "attribute.name.generic.attack_damage";
    private static final String ATTACK_SPEED_KEY = "attribute.name.generic.attack_speed";

    @Override
    public boolean supports(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Text> rawLines, boolean altDown) {
        String title = rawLines.isEmpty()
                ? stack.getName().getString()
                : rawLines.get(0).getString();

        List<Text> filteredLines = filterMainHandAttackLines(rawLines);
        HeaderStats headerStats = extractMainHandStats(stack);
        Text hint = buildStatsHint(headerStats);

        List<String> bodyLines = new ArrayList<>();
        List<Text> extraLines = new ArrayList<>();

        // Lines before the first blank line → body; lines after → extras (enchantments etc.)
        boolean pastBlank = false;
        for (int i = 1; i < filteredLines.size(); i++) {
            String s = filteredLines.get(i).getString().trim();
            if (!pastBlank && s.isEmpty()) {
                pastBlank = true;
                continue;
            }
            if (pastBlank) {
                extraLines.add(filteredLines.get(i));
            } else {
                bodyLines.add(filteredLines.get(i).getString());
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
                hint
        );
    }

    private static HeaderStats extractMainHandStats(ItemStack stack) {
        HeaderStat attackDamage = HeaderStat.missing();
        HeaderStat attackSpeed = HeaderStat.missing();

        stack.applyAttributeModifier(AttributeModifierSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.matches(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                double value = displayedAttributeValue(attribute.matches(EntityAttributes.GENERIC_ATTACK_DAMAGE), modifier);
                int priority = modifierPriority(modifier, Item.BASE_ATTACK_DAMAGE_MODIFIER_ID);
                if (priority > attackDamage.priority()) {
                    attackDamage.replace(value, priority);
                }
            } else if (attribute.matches(EntityAttributes.GENERIC_ATTACK_SPEED)) {
                double value = displayedAttributeValue(attribute.matches(EntityAttributes.GENERIC_ATTACK_SPEED), modifier);
                int priority = modifierPriority(modifier, Item.BASE_ATTACK_SPEED_MODIFIER_ID);
                if (priority > attackSpeed.priority()) {
                    attackSpeed.replace(value, priority);
                }
            }
        });

        if (!attackDamage.present() && !attackSpeed.present()) {
            return null;
        }

        return new HeaderStats(
                attackDamage.present() ? attackDamage.value() : null,
                attackSpeed.present() ? attackSpeed.value() : null
        );
    }

    private static int modifierPriority(EntityAttributeModifier modifier, net.minecraft.util.Identifier baseModifierId) {
        if (modifier.idMatches(baseModifierId)) return 3;
        if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) return 2;
        return 1;
    }

    private static double displayedAttributeValue(boolean isAttackAttribute, EntityAttributeModifier modifier) {
        double value = modifier.value();
        if (isAttackAttribute) {
            if (modifier.idMatches(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID)) {
                value += EntityAttributes.GENERIC_ATTACK_DAMAGE.value().getDefaultValue();
            } else if (modifier.idMatches(Item.BASE_ATTACK_SPEED_MODIFIER_ID)) {
                value += EntityAttributes.GENERIC_ATTACK_SPEED.value().getDefaultValue();
            }
        }

        if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                || modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return value * 100.0;
        }
        return value;
    }

    private static Text buildStatsHint(HeaderStats stats) {
        if (stats == null) return null;

        List<String> parts = new ArrayList<>(2);
        if (stats.attackDamage() != null) {
            parts.add("\uD83D\uDDE1 " + AttributeModifiersComponent.DECIMAL_FORMAT.format(stats.attackDamage()));
        }
        if (stats.attackSpeed() != null) {
            parts.add("\u231B " + AttributeModifiersComponent.DECIMAL_FORMAT.format(stats.attackSpeed()));
        }
        return parts.isEmpty() ? null : Text.literal(String.join("   ", parts));
    }

    private static List<Text> filterMainHandAttackLines(List<Text> rawLines) {
        if (rawLines.size() <= 1) return rawLines;

        Set<Integer> remove = new HashSet<>();
        int lastIndex = rawLines.size() - 1;

        for (int i = 1; i <= lastIndex; i++) {
            Text line = rawLines.get(i);
            if (!hasTranslatableKey(line, key -> MAINHAND_KEY.equals(key))) continue;

            List<Integer> attackLines = new ArrayList<>();
            int nonAttackAttributeLines = 0;

            int j = i + 1;
            while (j <= lastIndex) {
                Text candidate = rawLines.get(j);
                if (isBlank(candidate) || hasTranslatableKey(candidate, key -> key.startsWith(SLOT_HEADER_PREFIX))) {
                    break;
                }

                if (isAttackAttributeLine(candidate)) {
                    attackLines.add(j);
                } else if (isAttributeModifierLine(candidate)) {
                    nonAttackAttributeLines++;
                }

                j++;
            }

            if (!attackLines.isEmpty()) {
                remove.addAll(attackLines);
                if (nonAttackAttributeLines == 0) {
                    remove.add(i);
                    if (i > 1 && isBlank(rawLines.get(i - 1))) {
                        remove.add(i - 1);
                    }
                }
            }

            i = j - 1;
        }

        List<Text> filtered = new ArrayList<>(rawLines.size());
        for (int i = 0; i <= lastIndex; i++) {
            if (!remove.contains(i)) {
                filtered.add(rawLines.get(i));
            }
        }

        return collapseBlankLines(filtered);
    }

    private static List<Text> collapseBlankLines(List<Text> lines) {
        if (lines.size() <= 1) return lines;

        List<Text> compact = new ArrayList<>(lines.size());
        compact.add(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (isBlank(line) && (compact.size() == 1 || isBlank(compact.get(compact.size() - 1)))) {
                continue;
            }
            compact.add(line);
        }

        while (compact.size() > 1 && isBlank(compact.get(compact.size() - 1))) {
            compact.remove(compact.size() - 1);
        }
        return compact;
    }

    private static boolean isAttributeModifierLine(Text line) {
        return hasTranslatableKey(line, key -> key.startsWith(ATTRIBUTE_MODIFIER_PREFIX));
    }

    private static boolean isAttackAttributeLine(Text line) {
        return isAttributeModifierLine(line)
                && hasTranslatableKey(line, key -> ATTACK_DAMAGE_KEY.equals(key) || ATTACK_SPEED_KEY.equals(key));
    }

    private static boolean isBlank(Text line) {
        return line == null || line.getString().trim().isEmpty();
    }

    private static boolean hasTranslatableKey(Text text, Predicate<String> matcher) {
        if (text == null) return false;
        return hasTranslatableKey0(text, matcher, 0);
    }

    private static boolean hasTranslatableKey0(Text text, Predicate<String> matcher, int depth) {
        if (depth > 8) return false;

        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            if (matcher.test(translatable.getKey())) return true;
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Text nested && hasTranslatableKey0(nested, matcher, depth + 1)) {
                    return true;
                }
            }
        }

        for (Text sibling : text.getSiblings()) {
            if (hasTranslatableKey0(sibling, matcher, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private record HeaderStats(Double attackDamage, Double attackSpeed) {}

    private static final class HeaderStat {
        private double value;
        private int priority;

        private HeaderStat(double value, int priority) {
            this.value = value;
            this.priority = priority;
        }

        static HeaderStat missing() {
            return new HeaderStat(0.0, -1);
        }

        boolean present() {
            return this.priority >= 0;
        }

        double value() {
            return this.value;
        }

        int priority() {
            return this.priority;
        }

        void replace(double newValue, int newPriority) {
            this.value = newValue;
            this.priority = newPriority;
        }
    }
}
