package net.sweenus.simplytooltips.client.studio;

/**
 * Where the item-id suggestion rows sit.
 */
public final class SuggestionGeometry {

    private static final int ROW_H = StudioTheme.ROW_H;

    public static int height(int count) {
        return count * ROW_H + 2;
    }

    public static int rowY(int listY, int index) {
        return listY + 1 + index * ROW_H;
    }

    public static int indexAt(int listY, int count, double y) {
        if (count <= 0) return -1;
        double relative = y - (listY + 1);
        if (relative < 0.0) return -1;
        int index = (int) (relative / ROW_H);
        return index < count ? index : -1;
    }

    public static boolean contains(int listX, int listY, int listW, int count, double x, double y) {
        return x >= listX && x < listX + listW && indexAt(listY, count, y) >= 0;
    }

    private SuggestionGeometry() {}
}
