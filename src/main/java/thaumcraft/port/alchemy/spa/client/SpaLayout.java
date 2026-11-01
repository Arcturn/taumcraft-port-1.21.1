package thaumcraft.port.alchemy.spa.client;

/** Pixel coordinates from TC6 GuiSpa, shared by painting, tooltips and input. */
final class SpaLayout {
    static boolean toggle(double x, double y) { return inside(x, y, 89, 35, 8, 8); }
    static boolean toggleTooltip(double x, double y) { return inside(x, y, 88, 34, 10, 10); }
    static boolean fluidTooltip(double x, double y) { return inside(x, y, 104, 10, 10, 55); }
    static int coveredPixels(int amount) { return (int)(48 - 48f * Math.clamp(amount, 0, 5000) / 5000); }
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x < left + width && y < top + height;
    }
    private SpaLayout() {}
}
