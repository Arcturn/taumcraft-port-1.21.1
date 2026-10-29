package thaumcraft.port.world;

/** TC6's original upper bands, with proportional additional attempts in modern negative Y. */
public final class DepositRules {
    public record Band(int minY, int maxYExclusive, int attempts, int budget) {
        public int layers() { return Math.max(0, maxYExclusive - minY); }
    }
    private DepositRules() {}

    public static int legacyCount(int base, int densityPercent) {
        return Math.round(base * (densityPercent / 100.0f));
    }
    public static int scaledCount(int base, int densityPercent, int layers, int legacyLayers) {
        if (layers <= 0) return 0;
        return Math.round(base * (densityPercent / 100.0f) * ((float) layers / legacyLayers));
    }
    public static Band upperOre(int legacyLayers, int minY, int maxYExclusive, int density) {
        int low = Math.max(0, minY), high = Math.min(legacyLayers, maxYExclusive);
        return new Band(low, Math.max(low, high), scaledCount(18, density, high - low, legacyLayers), 0);
    }
    public static Band lowerOre(int legacyLayers, int minY, int maxYExclusive, int density) {
        int high = Math.min(0, maxYExclusive);
        return new Band(minY, Math.max(minY, high), scaledCount(18, density, high - minY, legacyLayers), 0);
    }
    public static int crystalLegacyLayers(int surfaceY) { return Math.max(5, surfaceY - 5); }
    public static Band upperCrystals(int minY, int maxYExclusive, int surfaceY, int density) {
        int original = crystalLegacyLayers(surfaceY), low = Math.max(0, minY);
        int high = Math.min(maxYExclusive, original), layers = Math.max(0, high - low);
        return new Band(low, Math.max(low, high), scaledCount(8, density, layers, original),
                scaledCount(64, density, layers, original));
    }
    public static Band lowerCrystals(int minY, int maxYExclusive, int surfaceY, int density) {
        // A 3x3x3 cluster may reach one block below its center, down to minY itself.
        int low = minY + 1, high = Math.min(0, Math.min(maxYExclusive - 1, surfaceY - 5));
        int layers = Math.max(0, high - low), original = crystalLegacyLayers(surfaceY);
        return new Band(low, Math.max(low, high), scaledCount(8, density, layers, original),
                scaledCount(64, density, layers, original));
    }
}
