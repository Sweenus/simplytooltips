package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.sweenus.simplytooltips.api.ModernTooltipModel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Post-processes a {@link ModernTooltipModel} to properly surface Apotheosis
 * tooltip content in a dedicated AFFIXES tab.
 *
 * <h3>Content routed to the AFFIXES tab</h3>
 * <ul>
 *   <li>Apotheosis affix effect lines — plain-text bullet lines (U+2022 •) injected by
 *       {@code ItemTooltipEvent}. rawLines is used as the authoritative source.</li>
 *   <li>"Can be Imbued" lines — Apotheosis imbue indicator.</li>
 *   <li>Additional attribute modifier lines added by Apotheosis affixes/gems/imbues.</li>
 *   <li>Socket summary — replaces the hidden sentinel injected by Apotheosis's
 *       attribute-tooltip handler.</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Collect Apotheosis content lines from {@code rawLines} (index 0 = title is skipped).</li>
 *   <li>If nothing is found, return the model unchanged — zero cost for non-Apotheosis items.</li>
 *   <li>Strip Apotheosis lines from {@code abilityLines} (LORE tab), {@code bodyLines} (STATS tab),
 *       and the socket-marker sentinel from {@code extraLines} so each section stays clean.</li>
 *   <li>Build {@code affixLines}: affix bullets separated by subtle {@link ModernTooltipModel#AFFIX_DIVIDER}
 *       lines under a {@code SECTION_MARKER+"Affixes"} header, followed by any bonus attributes
 *       and a socket summary section if present.</li>
 * </ol>
 *
 * <p>This class carries no compile-time dependency on the Apotheosis API. All detection is
 * text-pattern based; socket details are accessed through the legacy 1.20.1 API reflectively,
 * with a direct NBT fallback for compatibility.
 */
public final class ApotheosisCompat {

    /** U+2022 — bullet character Apotheosis prefixes on every affix effect line. */
    private static final char BULLET = '\u2022';

    /** U+25C6 — filled diamond used by Simply Bows as a section-header prefix. */
    private static final char SECTION_DIAMOND = '\u25C6';

    /** U+25C8 — white square with upper-right quadrant; used for a filled socket pip. */
    private static final String SOCKET_FILLED = "\u25C8";

    /** U+25C7 — white diamond; used for an empty socket pip. */
    private static final String SOCKET_EMPTY = "\u25C7";

    /** Sentinel used by Apotheosis 1.20.1 before it replaces the row with its socket component. */
    private static final String APOTH_REMOVE_MARKER = "APOTH_REMOVE_MARKER";

    /** Later Apotheosis versions renamed the sentinel; accepting both is harmless and resilient. */
    private static final String APOTH_SOCKET_MARKER = "APOTH_SOCKET_MARKER";

    /** Apotheosis 7.x client component replaced by the dedicated socket section. */
    private static final String SOCKET_TOOLTIP_RENDERER_CLASS =
            "dev.shadowsoffire.apotheosis.adventure.client.SocketTooltipRenderer";

    private static final String AFFIX_DATA_NBT = "affix_data";
    private static final String SOCKETS_NBT = "sockets";
    private static final String GEMS_NBT = "gems";

    private static final String SLOT_HEADER_PREFIX = "item.modifiers.";
    private static final String ATTRIBUTE_MODIFIER_PREFIX = "attribute.modifier.";
    private static final String ATTACK_DAMAGE_KEY = "attribute.name.generic.attack_damage";
    private static final String ATTACK_SPEED_KEY = "attribute.name.generic.attack_speed";

    private ApotheosisCompat() {}

    // -------------------------------------------------------------------------
    // Public detection helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} if {@code s} is an Apotheosis affix effect bullet line. */
    public static boolean isAffixLine(String s) {
        return s != null && s.length() > 1 && s.charAt(0) == BULLET;
    }

    /** Returns {@code true} if {@code s} is an Apotheosis "Can be Imbued" line. */
    public static boolean isImbueLine(String s) {
        return s != null && s.startsWith("Can be Imbued");
    }

    /**
     * Returns {@code true} if {@code s} is any Apotheosis-injected tooltip line that should
     * appear in the AFFIXES tab (affix bullet or imbue indicator).
     */
    public static boolean isApotheosisLine(String s) {
        return isAffixLine(s) || isImbueLine(s);
    }

    /**
     * Returns whether a gathered native tooltip component duplicates the socket section that
     * Simply Tooltips successfully added to the model. The class-name check avoids a hard
     * Apotheosis dependency, while the model check preserves the native renderer as a fallback.
     */
    public static boolean shouldSuppressNativeComponent(ModernTooltipModel model, Object component) {
        if (model == null || component == null
                || !SOCKET_TOOLTIP_RENDERER_CLASS.equals(component.getClass().getName())) {
            return false;
        }

        List<String> affixLines = model.affixLines();
        return affixLines != null
                && affixLines.contains(ModernTooltipModel.SECTION_MARKER + "Sockets");
    }

    // -------------------------------------------------------------------------
    // Model augmentation
    // -------------------------------------------------------------------------

    /**
     * Returns an augmented copy of {@code model} with Apotheosis content routed to the
     * dedicated AFFIXES tab ({@link ModernTooltipModel#affixLines()}).
     *
     * <p>The method is a no-op (returns {@code model} unchanged) when there is nothing
     * Apotheosis-related to surface, avoiding any allocation overhead.
     *
     * @param model    model produced by any {@link net.sweenus.simplytooltips.api.TooltipProvider}
     * @param rawLines full raw tooltip lines passed to the provider
     * @param stack    the item stack being tooltipped (used for optional socket lookup)
     * @param altDown  whether the Alt key is currently held (shows gem descriptions when true)
     * @return augmented model, or {@code model} unchanged if no Apotheosis content found
     */
    public static ModernTooltipModel augment(ModernTooltipModel model,
                                             List<Text> rawLines,
                                             ItemStack stack,
                                             boolean altDown) {
        List<String> affixGroup = collectAffixLines(rawLines);
        List<String> attributeGroup = collectBonusAttributeLines(rawLines);
        List<String> normalizedAttributeGroup = normalizeLines(attributeGroup);
        List<String> attributeSlotHeaders = collectAttributeSlotHeaderLines(rawLines);
        boolean hasSocketMarker = hasSocketMarker(rawLines);
        List<String> socketLines = buildSocketLines(stack, altDown);

        if (affixGroup.isEmpty() && attributeGroup.isEmpty()
                && !hasSocketMarker && socketLines.isEmpty()) return model;

        // Strip Apotheosis lines from abilityLines so the LORE tab stays clean.
        // (SimplySwordsCompatTooltipProvider can collect affix bullets into abilityLines.)
        List<String> cleanedAbility = new ArrayList<>(model.abilityLines().size());
        for (String line : model.abilityLines()) {
            if (!isApotheosisLine(line)
                    && !isSocketMarker(line)
                    && !containsNormalized(normalizedAttributeGroup, line)
                    && !isAttributeContextLine(line, attributeSlotHeaders))
                cleanedAbility.add(line);
        }
        removeEmptySection(cleanedAbility, "Enchantments");
        trimTrailingBlanks(cleanedAbility);

        // Strip Apotheosis lines from bodyLines so the STATS tab stays clean.
        // GenericTooltipProvider places affix bullets into bodyLines (they appear before the
        // blank-line separator), so without this filter they would show on both STATS and AFFIXES.
        // We also drop Apotheosis's hidden socket marker, which can land in bodyLines.
        List<String> cleanedBody = new ArrayList<>(model.bodyLines().size());
        for (String line : model.bodyLines()) {
            if (!isApotheosisLine(line)
                    && !isSocketMarker(line)
                    && !containsNormalized(normalizedAttributeGroup, line)
                    && !isAttributeContextLine(line, attributeSlotHeaders))
                cleanedBody.add(line);
        }
        removeEmptySection(cleanedBody, "Enchantments");
        trimTrailingBlanks(cleanedBody);

        // Strip Apotheosis's hidden socket marker from extraLines.
        // Apotheosis injects it via AddAttributeTooltipsEvent into the attribute section,
        // which lands in extraLines for attribute-carrying items.
        List<Text> cleanedExtra = new ArrayList<>(model.extraLines().size());
        for (Text t : model.extraLines()) {
            String s = t.getString();
            if (!isSocketMarker(s)
                    && !containsNormalized(normalizedAttributeGroup, s)
                    && !isAttributeContextLine(s, attributeSlotHeaders))
                cleanedExtra.add(t);
        }

        // Build affixLines: affix-bullet section + optional attribute/socket sections.
        List<String> newAffixLines = new ArrayList<>();

        if (!affixGroup.isEmpty()) {
            newAffixLines.add(ModernTooltipModel.SECTION_MARKER + "Affixes");
            for (int i = 0; i < affixGroup.size(); i++) {
                newAffixLines.add(affixGroup.get(i));
                // Subtle divider sentinel between consecutive affix entries.
                // TooltipRenderer draws this as a thin 1px line at reduced opacity.
                if (i < affixGroup.size() - 1) {
                    newAffixLines.add(ModernTooltipModel.AFFIX_DIVIDER);
                }
            }
        }

        if (!attributeGroup.isEmpty()) {
            newAffixLines.add(ModernTooltipModel.SECTION_MARKER + "Attributes");
            newAffixLines.addAll(attributeGroup);
        }

        if (!socketLines.isEmpty()) {
            newAffixLines.addAll(socketLines);
        }

        return new ModernTooltipModel(
                model.title(),
                model.badges(),
                model.borderStyle(),
                cleanedAbility,
                cleanedBody,
                cleanedExtra,
                model.theme(),
                model.upgradeSection(),
                model.animKeyExtra(),
                model.themeKey(),
                model.hint(),
                newAffixLines.isEmpty() ? null : newAffixLines,
                model.itemFrameProgress()
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Collects all Apotheosis-injected affix/imbue lines from {@code rawLines}.
     * Stops collecting as soon as a U+25C6 (◆) line is encountered — that signals
     * a mod-injected section header (e.g. Simply Bows "◆ Upgrades", "◆ Ability")
     * after which the bullet lines belong to that mod, not to Apotheosis.
     */
    private static List<String> collectAffixLines(List<Text> rawLines) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i < rawLines.size(); i++) {
            String s = rawLines.get(i).getString();
            if (!s.isEmpty() && s.charAt(0) == SECTION_DIAMOND) break;
            if (isApotheosisLine(s)) result.add(s);
        }
        return result;
    }

    /** Collects displayed equipment slot headers such as "When in Main Hand:" from raw lines. */
    private static List<String> collectAttributeSlotHeaderLines(List<Text> rawLines) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i < rawLines.size(); i++) {
            Text line = rawLines.get(i);
            if (hasTranslatableKey(line, key -> key.startsWith(SLOT_HEADER_PREFIX))) {
                result.add(line.getString());
            }
        }
        return result;
    }

    /**
     * Collects non-base attribute rows from vanilla equipment modifier blocks.
     *
     * <p>Apotheosis appends affix/gem/imbue bonuses into the same slot sections used by vanilla
     * item attributes. The first attack damage and attack speed rows in a section are usually the
     * base weapon stats that Simply Tooltips already summarizes elsewhere, so only subsequent
     * attack rows and all other attributes are mirrored into the AFFIXES tab.
     */
    private static List<String> collectBonusAttributeLines(List<Text> rawLines) {
        List<String> result = new ArrayList<>();
        boolean inAttributeBlock = false;
        boolean skippedBaseAttackDamage = false;
        boolean skippedBaseAttackSpeed = false;

        for (int i = 1; i < rawLines.size(); i++) {
            Text line = rawLines.get(i);
            String s = line.getString();

            if (!s.isEmpty() && s.charAt(0) == SECTION_DIAMOND) {
                inAttributeBlock = false;
                continue;
            }

            if (hasTranslatableKey(line, key -> key.startsWith(SLOT_HEADER_PREFIX))) {
                inAttributeBlock = true;
                skippedBaseAttackDamage = false;
                skippedBaseAttackSpeed = false;
                continue;
            }

            if (!inAttributeBlock) continue;

            if (s.isBlank()) {
                inAttributeBlock = false;
                continue;
            }
            if (isSocketMarker(s)) continue;

            if (!isAttributeModifierLine(line)) continue;

            if (isAttackDamageLine(line) && !skippedBaseAttackDamage) {
                skippedBaseAttackDamage = true;
                continue;
            }
            if (isAttackSpeedLine(line) && !skippedBaseAttackSpeed) {
                skippedBaseAttackSpeed = true;
                continue;
            }

            result.add(s);
        }

        return result;
    }

    /** Returns {@code true} if rawLines contains the Apotheosis socket marker sentinel. */
    private static boolean hasSocketMarker(List<Text> rawLines) {
        for (int i = 1; i < rawLines.size(); i++) {
            if (isSocketMarker(rawLines.get(i).getString())) return true;
        }
        return false;
    }

    private static boolean isSocketMarker(String line) {
        return APOTH_REMOVE_MARKER.equals(line) || APOTH_SOCKET_MARKER.equals(line);
    }

    /**
     * Builds optional socket summary lines without a compile-time Apotheosis dependency.
     *
     * <p>When {@code altDown} is {@code true} and a socket is filled, the gem's bullet-prefixed
     * bonus-effect lines (lines starting with U+2022 •) are appended as indented child lines
     * below the gem name.
     *
     * <p>The 1.20.1 Apotheosis API is preferred because its socket count includes modifications
     * made through {@code GetItemSocketsEvent}. If that API is unavailable, legacy
     * {@code affix_data} NBT is read as a safe fallback.
     *
     * @param stack   the host item stack
     * @param altDown whether Alt is held; activates per-gem description expansion
     */
    private static List<String> buildSocketLines(ItemStack stack, boolean altDown) {
        SocketSnapshot snapshot = LegacyApotheosisSocketAccess.read(stack, altDown);
        if (snapshot == null) {
            snapshot = readLegacySocketNbt(stack);
        }
        if (snapshot == null || snapshot.totalSockets() <= 0) return List.of();

        List<String> lines = new ArrayList<>();
        lines.add(ModernTooltipModel.SECTION_MARKER + "Sockets");

        for (int slot = 0; slot < snapshot.totalSockets(); slot++) {
            SocketEntry entry = slot < snapshot.entries().size()
                    ? snapshot.entries().get(slot)
                    : SocketEntry.EMPTY;

            if (entry.gem().isEmpty()) {
                lines.add(SOCKET_EMPTY + " Empty");
                continue;
            }

            lines.add(SOCKET_FILLED + " " + entry.gem().getName().getString());
            if (altDown) appendSocketDescription(lines, entry.description());
        }

        return lines;
    }

    private static void appendSocketDescription(List<String> target, String description) {
        if (description == null) return;
        String desc = description.trim();
        if (desc.isEmpty() || "Invalid Gem Category".equalsIgnoreCase(desc)) return;
        if (desc.charAt(0) != BULLET) desc = BULLET + " " + desc;
        target.add("  " + desc);
    }

    private static SocketSnapshot readLegacySocketNbt(ItemStack stack) {
        NbtCompound affixData = stack.getSubNbt(AFFIX_DATA_NBT);
        if (affixData == null) return SocketSnapshot.EMPTY;

        int totalSockets = Math.max(0, affixData.getInt(SOCKETS_NBT));
        if (totalSockets == 0) return SocketSnapshot.EMPTY;

        List<SocketEntry> entries = new ArrayList<>(totalSockets);
        NbtList gems = affixData.getList(GEMS_NBT, NbtElement.COMPOUND_TYPE);
        int storedGems = Math.min(totalSockets, gems.size());
        for (int i = 0; i < storedGems; i++) {
            ItemStack gem = ItemStack.fromNbt(gems.getCompound(i));
            entries.add(gem.isEmpty() ? SocketEntry.EMPTY : new SocketEntry(gem, null));
        }
        while (entries.size() < totalSockets) entries.add(SocketEntry.EMPTY);
        return new SocketSnapshot(totalSockets, List.copyOf(entries));
    }

    /** Lazy reflection bridge for Apotheosis 7.x on Minecraft 1.20.1. */
    private static final class LegacyApotheosisSocketAccess {
        private static final Access ACCESS = createAccess();

        private LegacyApotheosisSocketAccess() {}

        private static Access createAccess() {
            try {
                Class<?> socketHelper = Class.forName(
                        "dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper");
                Class<?> gemInstance = Class.forName(
                        "dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance");
                return new Access(
                        socketHelper.getMethod("getSockets", ItemStack.class),
                        socketHelper.getMethod("getGems", ItemStack.class),
                        gemInstance.getMethod("isValid"),
                        gemInstance.getMethod("gemStack"),
                        gemInstance.getMethod("getSocketBonusTooltip")
                );
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        private static SocketSnapshot read(ItemStack stack, boolean includeDescriptions) {
            if (ACCESS == null) return null;
            try {
                Object socketsObj = ACCESS.getSockets().invoke(null, stack);
                int totalSockets = socketsObj instanceof Number number
                        ? Math.max(0, number.intValue())
                        : 0;
                if (totalSockets == 0) return SocketSnapshot.EMPTY;

                Object gemsObj = ACCESS.getGems().invoke(null, stack);
                if (!(gemsObj instanceof List<?> gems)) return null;

                List<SocketEntry> entries = new ArrayList<>(totalSockets);
                for (int slot = 0; slot < totalSockets; slot++) {
                    if (slot >= gems.size()) {
                        entries.add(SocketEntry.EMPTY);
                        continue;
                    }

                    Object gemInstance = gems.get(slot);
                    boolean valid = gemInstance != null
                            && Boolean.TRUE.equals(ACCESS.isValid().invoke(gemInstance));
                    if (!valid) {
                        entries.add(SocketEntry.EMPTY);
                        continue;
                    }

                    Object gemObj = ACCESS.gemStack().invoke(gemInstance);
                    if (!(gemObj instanceof ItemStack gem) || gem.isEmpty()) {
                        entries.add(SocketEntry.EMPTY);
                        continue;
                    }

                    String description = includeDescriptions
                            ? componentToString(ACCESS.getSocketBonusTooltip().invoke(gemInstance))
                            : null;
                    entries.add(new SocketEntry(gem, description));
                }
                return new SocketSnapshot(totalSockets, List.copyOf(entries));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return null;
            }
        }

        private record Access(Method getSockets,
                              Method getGems,
                              Method isValid,
                              Method gemStack,
                              Method getSocketBonusTooltip) {}
    }

    private record SocketSnapshot(int totalSockets, List<SocketEntry> entries) {
        private static final SocketSnapshot EMPTY = new SocketSnapshot(0, List.of());
    }

    private record SocketEntry(ItemStack gem, String description) {
        private static final SocketEntry EMPTY = new SocketEntry(ItemStack.EMPTY, null);
    }

    private static String componentToString(Object value) {
        if (value == null) return null;
        if (value instanceof Text t) return t.getString();
        try {
            java.lang.reflect.Method getString = value.getClass().getMethod("getString");
            Object result = getString.invoke(value);
            return result != null ? result.toString() : null;
        } catch (Exception ignored) {
            return value.toString();
        }
    }

    private static boolean isAttributeContextLine(String line, List<String> attributeSlotHeaders) {
        if (line == null) return false;
        if (attributeSlotHeaders.contains(line)) return true;

        String s = stripSectionMarker(line).replace('\u00A0', ' ').trim();
        if (s.isEmpty()) return false;
        String lower = s.toLowerCase(java.util.Locale.ROOT);
        return lower.equals("when held:")
                || (lower.startsWith("when in ") && lower.endsWith(":"));
    }

    private static List<String> normalizeLines(List<String> lines) {
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(normalizeLine(line));
        }
        return result;
    }

    private static boolean containsNormalized(List<String> normalizedLines, String line) {
        return normalizedLines.contains(normalizeLine(line));
    }

    private static String normalizeLine(String line) {
        return stripSectionMarker(line).replace('\u00A0', ' ').trim();
    }

    private static void removeEmptySection(List<String> lines, String sectionTitle) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isSection(line, sectionTitle)) continue;

            boolean hasContent = false;
            for (int j = i + 1; j < lines.size(); j++) {
                String next = lines.get(j);
                if (next != null && next.startsWith(ModernTooltipModel.SECTION_MARKER)) break;
                if (next != null && !next.isBlank()) {
                    hasContent = true;
                    break;
                }
            }

            if (!hasContent) {
                lines.remove(i);
                while (i < lines.size() && lines.get(i).isBlank()) {
                    lines.remove(i);
                }
                i--;
            }
        }
    }

    private static boolean isSection(String line, String sectionTitle) {
        String s = stripSectionMarker(line).replace('\u00A0', ' ').trim();
        if (s.startsWith("\u25C6")) s = s.substring(1).trim();
        return s.equalsIgnoreCase(sectionTitle);
    }

    private static String stripSectionMarker(String line) {
        if (line == null) return "";
        return line.startsWith(ModernTooltipModel.SECTION_MARKER)
                ? line.substring(ModernTooltipModel.SECTION_MARKER.length())
                : line;
    }

    private static boolean isAttributeModifierLine(Text line) {
        return hasTranslatableKey(line, key -> key.startsWith(ATTRIBUTE_MODIFIER_PREFIX));
    }

    private static boolean isAttackDamageLine(Text line) {
        return hasTranslatableKey(line, key -> ATTACK_DAMAGE_KEY.equals(key));
    }

    private static boolean isAttackSpeedLine(Text line) {
        return hasTranslatableKey(line, key -> ATTACK_SPEED_KEY.equals(key));
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

    /** Removes trailing blank strings from {@code list} in-place. */
    private static void trimTrailingBlanks(List<String> list) {
        while (!list.isEmpty() && list.get(list.size() - 1).isBlank()) {
            list.remove(list.size() - 1);
        }
    }
}
