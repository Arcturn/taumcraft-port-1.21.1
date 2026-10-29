package thaumcraft.port.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Numerically isolated BETA26 ItemThaumometer / FXBlockRunes / world-tag rules. */
final class ScannerVisualRules {
    static final int RUNES_PER_USE = 10;
    static final float SCAN_VOLUME = .5f;
    static final float SCAN_PITCH = 1;
    static final double RUNE_ACCELERATION = -.04 * .03f;

    static Vec3 blockRuneOrigin(BlockPos position) {
        return new Vec3(position.getX() + .5, position.getY() + .75, position.getZ() + .5);
    }
    static Vec3 entityRuneOrigin(Vec3 position, float eyeHeight) {
        return position.add(0, eyeHeight / 2.0f + .5, 0);
    }
    static int entityRuneDuration(float height) { return (int) (height * 15); }
    static float runeOpacity(int age, int lifetime) {
        float threshold = lifetime / 5f;
        return lifetime <= 0 ? 0 : (age <= threshold ? age / threshold : (lifetime - age) / (float) lifetime) / 2;
    }
    static float nextTagScale(float previous, boolean visible) {
        float scale = previous > 0 ? previous - .005f : 0;
        if (visible && scale < .5f) scale += .031f - scale / 10f;
        return scale;
    }
    static float tagHorizontalOffset(int index, int count, float scale) {
        int column = index % 5, row = index / 5, columns = Math.min(5, count - row * 5);
        return (column - columns / 2f + .5f) * scale * 4 * scale;
    }
    static float tagVerticalOffset(int index, float scale) { return index / 5 * scale * 1.05f; }

    record Sparkle(int lifetime, int firstFrame, float[] alphaKeys, float startHalfSize, float endHalfSize) {}
    static Sparkle sparkle(RandomSource random, float size) {
        int first = random.nextFloat() < .2f ? 320 : 512;
        int lifetime = 16 + random.nextInt(4);
        float[] alpha = new float[6 + random.nextInt(lifetime / 3)];
        for (int i = 1; i < alpha.length - 1; i++) alpha[i] = random.nextFloat();
        return new Sparkle(lifetime, first, alpha, size * .1f, size * .2f);
    }
    private ScannerVisualRules() {}
}
