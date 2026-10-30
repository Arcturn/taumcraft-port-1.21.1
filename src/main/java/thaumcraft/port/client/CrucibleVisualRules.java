package thaumcraft.port.client;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** BETA26 TileCrucibleRenderer and FXDispatcher parameters, independent of the renderer. */
final class CrucibleVisualRules {
    enum Kind { BUBBLE, BOIL, FROTH, OVERFLOW }
    record Colour(float red, float green, float blue) {}
    record Bubble(int lifetime, float halfSize, float gravity, float movement,
                  int firstFrame, int[] finalFrames, boolean additive, float alpha, boolean collision) {}
    record Pollution(Vec3 offset, Vec3 velocity, int lifetime) {}
    static final int POLLUTION_GRID = 16, POLLUTION_FRAME = 56;
    static final Colour POLLUTION_COLOUR = new Colour(1, .3f, .9f);
    static final float POLLUTION_START_SIZE = .2f, POLLUTION_END_SIZE = .5f, POLLUTION_ALPHA = .5f;
    static final double POLLUTION_SLOWDOWN = 1, POLLUTION_WIND = .001;

    /** FXDispatcher.drawPollutionParticles: a small cloud rising from inside its source block. */
    static Pollution pollution(RandomSource random) {
        var offset = new Vec3(.2f + random.nextFloat() * .6f, .2f + random.nextFloat() * .6f,
                .2f + random.nextFloat() * .6f);
        var velocity = new Vec3((random.nextFloat() - random.nextFloat()) * .005, .02,
                (random.nextFloat() - random.nextFloat()) * .005);
        return new Pollution(offset, velocity, 100 + random.nextInt(60));
    }

    static Colour fluidColour(int total) {
        float recolour = Math.max(0, total) / 500f;
        if (recolour > 0) recolour = .5f + recolour / 2;
        recolour = Math.min(1, recolour);
        return new Colour(1 - recolour / 3, 1 - recolour, 1 - recolour / 2);
    }

    static Bubble bubble(RandomSource random, Kind kind, int strength) {
        return switch (kind) {
            case BUBBLE -> new Bubble(15 + random.nextInt(10), (.3f + random.nextFloat() * .3f) * .1f,
                    -.001f, .002f, 64, new int[]{65,66,66}, true, 1, true);
            case BOIL -> new Bubble((int)(7 + 8 / (random.nextDouble() * .8 + .2)),
                    (.2f + random.nextFloat() * .3f) * .1f, -.025f * strength, .001f,
                    64, new int[]{65,66}, true, 1, true);
            case FROTH -> new Bubble(4 + random.nextInt(3), (.2f + random.nextFloat() * .2f) * .1f,
                    .1f, .001f, 64, new int[]{65,66}, true, 1, true);
            case OVERFLOW -> new Bubble(12 + random.nextInt(12), (.4f + random.nextFloat() * .2f) * .1f,
                    .05f, .001f, 73, new int[]{65,66}, false, .8f, false);
        };
    }

    static int frame(int first, int[] finalFrames, int age, int lifetime) {
        // FXGeneric indexes final frames backwards by remaining life, not elapsed life.
        return finalFrames != null && finalFrames.length > 0 && age > lifetime - finalFrames.length
                ? finalFrames[Math.max(0, lifetime - age)] : first;
    }
    private CrucibleVisualRules() {}
}
