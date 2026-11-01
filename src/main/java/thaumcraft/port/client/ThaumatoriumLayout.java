package thaumcraft.port.client;

/** Original TC6's two-column, three-row recipe window; scroll offsets are rows, not pages. */
final class ThaumatoriumLayout {
    static int maxRow(int size) { return Math.max(0, (size + 1) / 2 - 3); }
    static int clampRow(int row, int size) { return Math.clamp(row, 0, maxRow(size)); }
    static int recipeAt(double x, double y, int row, int size) {
        if (!inside(x, y, 48, 56, 32, 48)) return -1;
        int index = clampRow(row, size) * 2 + (int)(x - 48) / 16 + ((int)(y - 56) / 16) * 2;
        return index < size ? index : -1;
    }
    static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }
    static int preview(long milliseconds, int selectedCount) {
        return selectedCount <= 0 ? -1 : (int)Math.floorMod(milliseconds / 1000, selectedCount);
    }
    static int progress(int stored, int required) {
        return required <= 0 ? 0 : (int)((float)stored / required * 12);
    }
    private ThaumatoriumLayout() {}
}
