package net.sweenus.simplytooltips.api;

/**
 * Legacy integer identifiers for the built-in border patterns.
 *
 * @deprecated Borders are keyed by string now. See
 *             {@link net.sweenus.simplytooltips.client.render.BorderRegistry}, which is also how a mod
 *             registers its own. These constants are kept so existing code compiles and behaves the
 *             same; {@link #keyOf(int)} converts one to the corresponding registry key.
 */
@Deprecated
public final class TooltipBorderStyle {
    public static final int DEFAULT = 0;
    public static final int VINE    = 1;
    public static final int BEE     = 2;
    public static final int BLOSSOM = 3;
    public static final int BUBBLE  = 4;
    public static final int EARTH   = 5;
    public static final int ECHO    = 6;
    public static final int ICE     = 7;
    public static final int LIGHTNING = 8;
    public static final int EMBER   = 9;
    public static final int ENCHANTED = 10;
    public static final int AUTUMN = 11;
    public static final int SOUL = 12;
    public static final int DEEP_DARK = 13;
    public static final int POISON = 14;
    public static final int OCEAN = 15;
    public static final int RUSTIC = 16;
    public static final int HONEY = 17;
    public static final int JADE = 18;
    public static final int WOOD = 19;
    public static final int STONE = 20;
    public static final int IRON = 21;
    public static final int GOLD = 22;
    public static final int DIAMOND = 23;
    public static final int NETHERITE = 24;
    public static final int RUNIC = 25;
    public static final int BLOOD = 26;

    /** Maps a legacy constant to its border pattern key; unknown values map to {@code "none"}. */
    public static String keyOf(int borderStyle) {
        return switch (borderStyle) {
            case VINE      -> "vine";
            case BEE       -> "bee";
            case BLOSSOM   -> "blossom";
            case BUBBLE    -> "bubble";
            case EARTH     -> "earth";
            case ECHO      -> "echo";
            case ICE       -> "ice";
            case LIGHTNING -> "lightning";
            case EMBER     -> "ember";
            case ENCHANTED -> "enchanted";
            case AUTUMN    -> "autumn";
            case SOUL      -> "soul";
            case DEEP_DARK -> "deepdark";
            case POISON    -> "poison";
            case OCEAN     -> "ocean";
            case RUSTIC    -> "rustic";
            case HONEY     -> "honey";
            case JADE      -> "jade";
            case WOOD      -> "wood";
            case STONE     -> "stone";
            case IRON      -> "iron";
            case GOLD      -> "gold";
            case DIAMOND   -> "diamond";
            case NETHERITE -> "netherite";
            case RUNIC     -> "runic";
            case BLOOD     -> "blood";
            default        -> "none";
        };
    }

    private TooltipBorderStyle() {}
}
