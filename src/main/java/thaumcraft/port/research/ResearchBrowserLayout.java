package thaumcraft.port.research;

/** Coordinate and brightness rules from TC6 GuiResearchBrowser, independent of rendering. */
public final class ResearchBrowserLayout {
    public record Position(double x, double y) {}

    /** Legacy guiMap/curMouse/tempMap state: direct drag, then tick easing toward the clamped target. */
    public static final class Motion {
        private Position current = new Position(0, 0);
        private Position previous = current;
        private Position target = current;

        public Position current() { return current; }

        public void snap(double x, double y) {
            current = new Position(x, y);
            previous = target = current;
        }

        public void drag(double dx, double dy) {
            snap(current.x + dx, current.y + dy);
        }

        public void aim(double x, double y) { target = new Position(x, y); }

        public void tick() {
            previous = current;
            double dx = target.x - current.x, dy = target.y - current.y;
            double fraction = dx * dx + dy * dy < 4 ? 1 : .85;
            current = new Position(current.x + dx * fraction, current.y + dy * fraction);
        }

        public Position interpolate(float partialTick) {
            return new Position(previous.x + (current.x - previous.x) * partialTick,
                    previous.y + (current.y - previous.y) * partialTick);
        }
    }

    public record Bounds(int minimum, int maximum) {
        public double center() { return (minimum + maximum) / 2; }

        public double clamp(double origin, float zoom) {
            double low = minimum * zoom;
            double high = Math.max(low, maximum * zoom - 1);
            return Math.clamp(origin, low, high);
        }

        /** Draw coordinates are floored first; bounds use Java's integer cast, as in the legacy browser. */
        public int display(double interpolated, float zoom) {
            int value = (int) Math.floor(interpolated);
            if (value < minimum * zoom) value = (int) (minimum * zoom);
            if (value >= maximum * zoom) value = (int) (maximum * zoom - 1);
            return value;
        }
    }

    public static Bounds bounds(int minimumGrid, int maximumGrid, int screenExtent) {
        return new Bounds(minimumGrid * 24 - (screenExtent - 32) + 48, maximumGrid * 24 - 24);
    }

    /** The legacy icon center is 24 + grid*24 - mapOrigin, before screen zoom. */
    public static double pan(double origin, int screenExtent, float zoom) {
        return 24 - screenExtent * zoom / 2.0 - origin;
    }

    public static double origin(double pan, int screenExtent, float zoom) {
        return 24 - screenExtent * zoom / 2.0 - pan;
    }

    public static float frameBrightness(boolean complete, boolean canUnlock, long millis) {
        if (complete) return 1;
        if (!canUnlock) return .3f;
        return (float) Math.sin((millis % 600L) / 600.0 * Math.PI * 2) * .25f + .75f;
    }

    private ResearchBrowserLayout() {}
}
