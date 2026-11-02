package thaumcraft.port.warp;

import thaumcraft.port.alchemy.liquid.AlchemyRules;

/** BETA26's event cadence, severity distribution and counter decay, without world state. */
public final class WarpRules {
    public enum Event {
        NONE, CREEPER, EXPLOSION, UNEASE, VIS_EXHAUST, THAUMARHIA, HUNGER,
        WHISPERS, MIST_SINGLE, BLURRED_VISION, SUN_SCORNED, MINING_FATIGUE,
        INFECTIOUS, NIGHT_VISION, DEATH_GAZE, SPIDERS, STARES, MIST_MEDIUM,
        BLINDNESS, CLARITY, HUNGER_LONG, PORTAL, REAL_SPIDERS, MIST_STRONG
    }

    public static boolean isCheckDue(int ticks, boolean disabled, boolean warded) {
        return ticks > 0 && ticks % 2000 == 0 && !disabled && !warded;
    }

    public static boolean canTrigger(int counter, int totalIncludingGear, int chanceRoll) {
        return AlchemyRules.warpEvent(counter, totalIncludingGear, chanceRoll);
    }

    /** Exclusive random bound, computed with the counter BEFORE it decays. Gear is already in total. */
    public static int severityRange(int totalIncludingGear, int counter) {
        return (int) Math.clamp((2L * totalIncludingGear + counter) / 3, 0, 100);
    }

    public static int remainingCounter(int counter, int gearWarp) {
        return AlchemyRules.remainingCounter(counter, gearWarp);
    }

    public static Event eventForSeverity(int severity) {
        if (severity <= 0) return Event.NONE;
        if (severity <= 4) return Event.CREEPER;
        if (severity <= 8) return Event.EXPLOSION;
        if (severity <= 12) return Event.UNEASE;
        if (severity <= 16) return Event.VIS_EXHAUST;
        if (severity <= 20) return Event.THAUMARHIA;
        if (severity <= 24) return Event.HUNGER;
        if (severity <= 28) return Event.WHISPERS;
        if (severity <= 32) return Event.MIST_SINGLE;
        if (severity <= 36) return Event.BLURRED_VISION;
        if (severity <= 40) return Event.SUN_SCORNED;
        if (severity <= 44) return Event.MINING_FATIGUE;
        if (severity <= 48) return Event.INFECTIOUS;
        if (severity <= 52) return Event.NIGHT_VISION;
        if (severity <= 56) return Event.DEATH_GAZE;
        if (severity <= 60) return Event.SPIDERS;
        if (severity <= 64) return Event.STARES;
        if (severity <= 68) return Event.MIST_MEDIUM;
        if (severity <= 72) return Event.BLINDNESS;
        // This is a single equality in BETA26, not a four-value range: 73..75 give hunger.
        if (severity == 76) return Event.CLARITY;
        if (severity <= 80) return Event.HUNGER_LONG;
        if (severity <= 88) return Event.PORTAL;
        if (severity <= 92) return Event.REAL_SPIDERS;
        return Event.MIST_STRONG;
    }

    private WarpRules() {}
}
