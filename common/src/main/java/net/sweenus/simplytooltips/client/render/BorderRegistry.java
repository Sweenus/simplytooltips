package net.sweenus.simplytooltips.client.render;

import net.sweenus.simplytooltips.client.render.border.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps border pattern key strings to {@link BorderPattern} instances.
 * Built-in patterns are registered at class load time.
 * Third-party code may call {@link #register} to add custom border patterns.
 *
 * <p>The built-ins share their keys with {@link MotifRegistry} on purpose: a theme that only sets
 * {@code motif} still gets the border it always had, because {@code border} falls back to {@code motif}.
 */
public class BorderRegistry {

    private static final Map<String, BorderPattern> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("vine",      new VineBorder());
        PATTERNS.put("bee",       new BeeBorder());
        PATTERNS.put("honey",     new HoneyBorder());
        PATTERNS.put("blossom",   new BlossomBorder());
        PATTERNS.put("bubble",    new BubbleBorder());
        PATTERNS.put("earth",     new EarthBorder());
        PATTERNS.put("echo",      new EchoBorder());
        PATTERNS.put("ice",       new IceBorder());
        PATTERNS.put("lightning", new LightningBorder());
        PATTERNS.put("ember",     new EmberBorder());
        PATTERNS.put("enchanted", new EnchantedBorder());
        PATTERNS.put("autumn",    new AutumnBorder());
        PATTERNS.put("soul",      new SoulBorder());
        PATTERNS.put("deepdark",  new DeepDarkBorder());
        PATTERNS.put("poison",    new PoisonBorder());
        PATTERNS.put("blood",     new BloodBorder());
        PATTERNS.put("ocean",     new OceanBorder());
        PATTERNS.put("rustic",    new RusticBorder());
        PATTERNS.put("jade",      new JadeBorder());
        PATTERNS.put("wood",      new WoodBorder());
        PATTERNS.put("stone",     new StoneBorder());
        PATTERNS.put("iron",      new IronBorder());
        PATTERNS.put("gold",      new GoldBorder());
        PATTERNS.put("diamond",   new DiamondBorder());
        PATTERNS.put("netherite", new NetheriteBorder());
        PATTERNS.put("runic",     new RunicBorder());
        PATTERNS.put("corrupted_eye", new CorruptedEyeBorder());
        PATTERNS.put("spectral",  new SpectralBorder());
        PATTERNS.put("radiant",   new RadiantBorder());
        PATTERNS.put("candle",    new CandleBorder());
        PATTERNS.put("amethyst",  new AmethystBorder());
        PATTERNS.put("tome",      new TomeBorder());
    }

    /**
     * Returns the pattern for {@code key}, or {@code null} if none is registered
     * (the decoration pass is simply skipped, leaving the plain themed outline).
     */
    public static BorderPattern get(String key) {
        if (key == null) return null;
        return PATTERNS.get(key);
    }

    /** True if {@code key} names a registered pattern. */
    public static boolean has(String key) {
        return key != null && PATTERNS.containsKey(key);
    }

    /** Register a custom border pattern under the given key. */
    public static void register(String key, BorderPattern pattern) {
        PATTERNS.put(key, pattern);
    }

    private BorderRegistry() {}
}
