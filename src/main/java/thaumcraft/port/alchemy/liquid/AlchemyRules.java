package thaumcraft.port.alchemy.liquid;

/** Original arithmetic kept separate from registration for deterministic regression checks. */
public final class AlchemyRules {
    public static int wardDuration(int permanentWarp) {
        return Math.min(32000, 200000 / Math.max(1, (int)Math.sqrt(Math.max(0, permanentWarp))));
    }
    public static int soapStrength(boolean ward, boolean bath) { return 1 + (ward ? 1 : 0) + (bath ? 1 : 0); }
    public static boolean finishedWashing(int remaining) { return 100 - remaining > 95; }
    public static boolean warpEvent(int counter, int warp, int roll) {
        return counter > 0 && warp > 0 && roll <= Math.sqrt(counter);
    }
    public static int remainingCounter(int counter, int gearWarp) {
        return Math.max(0, (int)(counter - Math.max(5, Math.sqrt(counter) * 2 - gearWarp * 2)));
    }
    private AlchemyRules() {}
}
