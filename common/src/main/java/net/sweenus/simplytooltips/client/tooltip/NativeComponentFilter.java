package net.sweenus.simplytooltips.client.tooltip;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blacklist for gathered native tooltip components, matched by implementation class name.
 *
 * <p>Simply Tooltips intercepts the tooltip pipeline <em>after</em> the loader has gathered
 * components, which is what lets Create and most other mods contribute content. The trade-off
 * is that decoration components from mods that draw their own tooltip chrome are picked up as
 * well, and those are redundant inside a Simply Tooltips panel. This filter drops them.
 *
 * <h3>Rules</h3>
 * A rule is either an exact fully-qualified class name, or a package prefix written with a
 * trailing {@code .} or {@code *}:
 * <pre>{@code
 * com.example.tooltip.WidgetComponent   // exact class
 * com.example.tooltip.*                 // every class in that package
 * }</pre>
 *
 * <p>{@link #BUILTIN_BLOCKED_PREFIXES} ships blocked out of the box. Resource packs may add
 * their own rules, or re-enable a built-in one, through the {@code tooltip_components} block
 * of {@code assets/simplytooltips/item_themes/<name>.json} — see
 * {@link net.sweenus.simplytooltips.client.render.ItemThemeRegistry}, which parses those files
 * and calls {@link #setRules(Map)} on every resource reload.
 *
 * <p>Built-in defaults deliberately live here rather than in {@code defaults.json}: a resource
 * pack that replaces {@code defaults.json} to customise themes must not silently resurrect the
 * components those defaults exist to hide.
 *
 * <p>Model-dependent suppression (a component that duplicates a section Simply Tooltips has
 * already built) is not a static class blacklist and stays in its own compat class — see
 * {@link ApotheosisCompat#shouldSuppressNativeComponent}.
 */
public final class NativeComponentFilter {

    /**
     * Package prefixes blocked without any configuration.
     *
     * <p>Legendary Tooltips inserts an {@code ItemModelComponent} (a rotating item render in a
     * framed square) at the head of the gather list, plus — on 1.20.1 — a {@code PaddingComponent}
     * reserving space for it. Both are chrome that a Simply Tooltips panel already provides via
     * its own header icon, so the whole {@code tooltip} package is blocked rather than the two
     * class names: it covers the version difference and any future LT decoration component.
     */
    private static final List<String> BUILTIN_BLOCKED_PREFIXES = List.of(
            "com.anthonyhilyard.legendarytooltips.tooltip."
    );

    /** Data-driven rules: class-name pattern → whether the component may render. */
    private static volatile Map<String, Boolean> rules = Map.of();

    /** Resolved verdict per component class. Cleared whenever {@link #rules} changes. */
    private static final Map<Class<?>, Boolean> VERDICT_CACHE = new ConcurrentHashMap<>();

    private NativeComponentFilter() {}

    /**
     * Replaces the data-driven rule set and invalidates the resolved-verdict cache.
     * Called by {@code ItemThemeRegistry.loadAll} at the end of every resource reload.
     */
    public static void setRules(Map<String, Boolean> newRules) {
        rules = (newRules == null || newRules.isEmpty()) ? Map.of() : Map.copyOf(newRules);
        VERDICT_CACHE.clear();
    }

    /** Number of active data-driven rules; used for the resource-load summary log. */
    public static int ruleCount() {
        return rules.size();
    }

    /**
     * Returns whether {@code component} should be dropped instead of rendered inside the
     * Simply Tooltips panel. Called for every gathered component every frame, so the verdict
     * is cached per class.
     */
    public static boolean isBlocked(Object component) {
        if (component == null) return false;
        return VERDICT_CACHE.computeIfAbsent(component.getClass(), NativeComponentFilter::resolve);
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    /**
     * Walks the class hierarchy so a rule on a shared base class also covers mod subclasses.
     * The first class in the chain with a matching rule decides; nothing matching means visible.
     */
    private static boolean resolve(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Boolean verdict = resolveForName(c.getName());
            if (verdict != null) return verdict;
        }
        return false;
    }

    /**
     * Resolves a single class name against the rule set.
     *
     * <p>Order: exact data-driven rule → longest matching data-driven prefix → built-in prefix.
     * Data-driven rules are consulted first so {@code "enabled": true} can re-enable a built-in.
     *
     * @return {@code true} blocked, {@code false} explicitly allowed, {@code null} no rule matched
     */
    private static Boolean resolveForName(String className) {
        Map<String, Boolean> current = rules;

        Boolean exact = current.get(className);
        if (exact != null) return !exact;

        // Longest matching prefix wins, so a narrow rule beats a broad one regardless of
        // the order the JSON files happened to load in.
        String bestPrefix = null;
        Boolean bestEnabled = null;
        for (Map.Entry<String, Boolean> rule : current.entrySet()) {
            String prefix = asPrefix(rule.getKey());
            if (prefix == null || !className.startsWith(prefix)) continue;
            if (bestPrefix == null || prefix.length() > bestPrefix.length()) {
                bestPrefix = prefix;
                bestEnabled = rule.getValue();
            }
        }
        if (bestEnabled != null) return !bestEnabled;

        for (String prefix : BUILTIN_BLOCKED_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return null;
    }

    /** Converts a prefix pattern to its literal prefix; returns {@code null} for exact names. */
    private static String asPrefix(String pattern) {
        if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 1);
        if (pattern.endsWith("."))  return pattern;
        return null;
    }
}
