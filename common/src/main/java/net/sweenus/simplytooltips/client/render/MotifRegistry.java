package net.sweenus.simplytooltips.client.render;

import net.sweenus.simplytooltips.client.render.motif.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps motif key strings to {@link BackgroundMotif} instances.
 * Built-in motifs are registered at class load time.
 * Third-party code may call {@link #register} to add custom motifs.
 */
public class MotifRegistry {

    private static final Map<String, BackgroundMotif> MOTIFS = new HashMap<>();

    static {
        MOTIFS.put("vine",      new VineMotif());
        MOTIFS.put("ember",     new EmberMotif());
        MOTIFS.put("enchanted", new EnchantedMotif());
        MOTIFS.put("bee",       new BeeMotif());
        MOTIFS.put("blossom",   new BlossomMotif());
        MOTIFS.put("bubble",    new BubbleMotif());
        MOTIFS.put("earth",     new EarthMotif());
        MOTIFS.put("echo",      new EchoMotif());
        MOTIFS.put("cosmic",    new CosmicMotif());
        MOTIFS.put("ice",       new IceMotif());
        MOTIFS.put("lightning", new LightningMotif());
        MOTIFS.put("autumn",    new AutumnMotif());
        MOTIFS.put("soul",      new SoulMotif());
        MOTIFS.put("deepdark",  new DeepDarkMotif());
        MOTIFS.put("poison",    new PoisonMotif());
        MOTIFS.put("blood",     new BloodMotif());
        MOTIFS.put("ocean",     new OceanMotif());
        MOTIFS.put("rustic",    new RusticMotif());
        MOTIFS.put("honey",     new HoneyMotif());
        MOTIFS.put("jade",      new JadeMotif());
        MOTIFS.put("wood",      new WoodMotif());
        MOTIFS.put("stone",     new StoneMotif());
        MOTIFS.put("iron",      new IronMotif());
        MOTIFS.put("gold",      new GoldMotif());
        MOTIFS.put("diamond",   new DiamondMotif());
        MOTIFS.put("netherite", new NetheriteMotif());
        MOTIFS.put("runic",     new RunicMotif());
        MOTIFS.put("corrupted_eye", new CorruptedEyeMotif());
        MOTIFS.put("spectral",  new SpectralMotif());
        MOTIFS.put("radiant",   new RadiantMotif());
        MOTIFS.put("candle",    new CandleMotif());
        MOTIFS.put("amethyst",  new AmethystMotif());
        MOTIFS.put("tome",      new TomeMotif());
    }

    /**
     * Returns the motif for {@code key}, or {@code null} if none is registered
     * (rendering will simply skip the motif pass).
     */
    public static BackgroundMotif get(String key) {
        if (key == null) return null;
        return MOTIFS.get(key);
    }

    /** Register a custom motif under the given key. */
    public static void register(String key, BackgroundMotif motif) {
        MOTIFS.put(key, motif);
    }

    /** Every registered motif key, sorted, for menus and validation. */
    public static java.util.List<String> keys() {
        java.util.List<String> keys = new java.util.ArrayList<>(MOTIFS.keySet());
        java.util.Collections.sort(keys);
        return java.util.Collections.unmodifiableList(keys);
    }

    private MotifRegistry() {}
}
