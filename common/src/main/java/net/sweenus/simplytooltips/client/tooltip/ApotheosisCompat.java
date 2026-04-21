package net.sweenus.simplytooltips.client.tooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.api.ModernTooltipModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processes a {@link ModernTooltipModel} to properly surface Apotheosis
 * tooltip content in a dedicated AFFIXES tab.
 *
 * <h3>Content routed to the AFFIXES tab</h3>
 * <ul>
 *   <li>Apotheosis affix effect lines — plain-text bullet lines (U+2022 •) injected by
 *       {@code ItemTooltipEvent}. rawLines is used as the authoritative source.</li>
 *   <li>"Can be Imbued" lines — Apotheosis imbue indicator.</li>
 *   <li>Socket summary — replaces the {@code "APOTH_SOCKET_MARKER"} sentinel injected by
 *       Apotheosis's {@code AddAttributeTooltipsEvent} handler.</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Collect Apotheosis content lines from {@code rawLines} (index 0 = title is skipped).</li>
 *   <li>If nothing is found, return the model unchanged — zero cost for non-Apotheosis items.</li>
 *   <li>Strip Apotheosis lines from {@code abilityLines} (LORE tab), {@code bodyLines} (STATS tab),
 *       and the socket-marker sentinel from {@code extraLines} so each section stays clean.</li>
 *   <li>Build {@code affixLines}: affix bullets separated by subtle {@link ModernTooltipModel#AFFIX_DIVIDER}
 *       lines under a {@code SECTION_MARKER+"Affixes"} header, followed by a socket summary section
 *       if sockets are present.</li>
 * </ol>
 *
 * <p>This class carries no compile-time dependency on the Apotheosis API. All detection is
 * text-pattern based; DataComponent access uses the Minecraft registry by string ID.
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

    /** Sentinel injected by Apotheosis {@code AddAttributeTooltipsEvent} for socket rendering. */
    private static final String APOTH_SOCKET_MARKER = "APOTH_SOCKET_MARKER";

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
     * @param stack    the item stack being tooltipped (used for DataComponent socket lookup)
     * @param altDown  whether the Alt key is currently held (shows gem descriptions when true)
     * @return augmented model, or {@code model} unchanged if no Apotheosis content found
     */
    public static ModernTooltipModel augment(ModernTooltipModel model,
                                             List<Text> rawLines,
                                             ItemStack stack,
                                             boolean altDown) {
        List<String> affixGroup = collectAffixLines(rawLines);
        boolean      hasSocket  = hasSocketMarker(rawLines);

        if (affixGroup.isEmpty() && !hasSocket) return model;

        // Strip Apotheosis lines from abilityLines so the LORE tab stays clean.
        // (SimplySwordsCompatTooltipProvider can collect affix bullets into abilityLines.)
        List<String> cleanedAbility = new ArrayList<>(model.abilityLines().size());
        for (String line : model.abilityLines()) {
            if (!isApotheosisLine(line)) cleanedAbility.add(line);
        }
        trimTrailingBlanks(cleanedAbility);

        // Strip Apotheosis lines from bodyLines so the STATS tab stays clean.
        // GenericTooltipProvider places affix bullets into bodyLines (they appear before the
        // blank-line separator), so without this filter they would show on both STATS and AFFIXES.
        // We also drop APOTH_SOCKET_MARKER which ends up in bodyLines for some items.
        List<String> cleanedBody = new ArrayList<>(model.bodyLines().size());
        for (String line : model.bodyLines()) {
            if (!isApotheosisLine(line) && !APOTH_SOCKET_MARKER.equals(line))
                cleanedBody.add(line);
        }
        trimTrailingBlanks(cleanedBody);

        // Strip APOTH_SOCKET_MARKER from extraLines.
        // Apotheosis injects it via AddAttributeTooltipsEvent into the attribute section,
        // which lands in extraLines for attribute-carrying items.
        List<Text> cleanedExtra = new ArrayList<>(model.extraLines().size());
        for (Text t : model.extraLines()) {
            if (!APOTH_SOCKET_MARKER.equals(t.getString())) cleanedExtra.add(t);
        }

        // Build affixLines: affix-bullet section + optional socket section.
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

        if (hasSocket) {
            List<String> socketLines = buildSocketLines(stack, altDown);
            if (!socketLines.isEmpty()) {
                newAffixLines.addAll(socketLines);
            }
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
                newAffixLines.isEmpty() ? null : newAffixLines
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

    /** Returns {@code true} if rawLines contains the Apotheosis socket marker sentinel. */
    private static boolean hasSocketMarker(List<Text> rawLines) {
        for (int i = 1; i < rawLines.size(); i++) {
            if (APOTH_SOCKET_MARKER.equals(rawLines.get(i).getString())) return true;
        }
        return false;
    }

    /**
     * Builds the socket summary lines for the AFFIXES tab by reading the item's DataComponents
     * directly from the Minecraft component-type registry — no compile-time Apotheosis dependency.
     *
     * <p>When {@code altDown} is {@code true} and a socket is filled, the gem's bullet-prefixed
     * bonus-effect lines (lines starting with U+2022 •) are appended as indented child lines
     * below the gem name.
     *
     * <p>Returns an empty list if Apotheosis is not loaded or the components are absent.
     *
     * @param stack   the host item stack
     * @param altDown whether Alt is held; activates per-gem description expansion
     */
    @SuppressWarnings("unchecked")
    private static List<String> buildSocketLines(ItemStack stack, boolean altDown) {
        return List.of();
    }

    /**
     * Appends an item-category-specific gem effect line for the given host item.
     *
     * <p>This mirrors Apotheosis' own socket tooltip behavior by constructing a
     * socketed {@code GemInstance} against the host item and reading
     * {@code SocketTooltipRenderer#getSocketDesc}. This ensures only effects that apply
     * to the current equipment category are shown (e.g. sword-only on swords).
     */
    private static void appendGemDescriptions(List<String> target,
                                              ItemStack hostStack,
                                              ItemStack gem,
                                              int slot) {
        try {
            if (appendSocketBonusDescription(target, hostStack, gem, slot)) return;
        } catch (Exception ignored) {
            // Reflection failed; leave socket entry without expanded details.
        }
    }

    private static boolean appendSocketBonusDescription(List<String> target,
                                                        ItemStack hostStack,
                                                        ItemStack gem,
                                                        int slot) throws ReflectiveOperationException {
        Class<?> gemInstanceClass = Class.forName("dev.shadowsoffire.apotheosis.socket.gem.GemInstance");
        Class<?> socketTooltipRendererClass = Class.forName("dev.shadowsoffire.apotheosis.client.SocketTooltipRenderer");

        java.lang.reflect.Method socketed = gemInstanceClass.getMethod("socketed", ItemStack.class, ItemStack.class, int.class);
        Object gemInstance = socketed.invoke(null, hostStack, gem, slot);
        if (gemInstance == null) return false;

        java.lang.reflect.Method getSocketDesc = socketTooltipRendererClass.getMethod("getSocketDesc", gemInstanceClass);
        Object descObj = getSocketDesc.invoke(null, gemInstance);

        String desc = componentToString(descObj);
        if (desc == null) return false;
        desc = desc.trim();
        if (desc.isEmpty() || "Invalid Gem Category".equalsIgnoreCase(desc)) return false;

        if (desc.charAt(0) != BULLET) desc = BULLET + " " + desc;
        target.add("  " + desc);
        return true;
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

    /** Removes trailing blank strings from {@code list} in-place. */
    private static void trimTrailingBlanks(List<String> list) {
        while (!list.isEmpty() && list.get(list.size() - 1).isBlank()) {
            list.remove(list.size() - 1);
        }
    }
}
