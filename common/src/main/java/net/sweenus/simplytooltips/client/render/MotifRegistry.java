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
        MOTIFS.put("ice",       new IceMotif());
        MOTIFS.put("lightning", new LightningMotif());
        MOTIFS.put("autumn",    new AutumnMotif());
        MOTIFS.put("soul",      new SoulMotif());
        MOTIFS.put("deepdark",  new DeepDarkMotif());
        MOTIFS.put("poison",    new PoisonMotif());
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

    private MotifRegistry() {}
}
