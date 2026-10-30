package thaumcraft.port.client;

import java.util.Random;

/** Screen-space timing and motion from TC6 HudHandler.renderKnowledgeGains. */
final class KnowledgeGainAnimation {
    record Flash(float size, float rotation, float green, float blue, int frame) {}
    record Frame(float x, float y, float size, float rotation, Flash flash) {}

    static Frame frame(int width, int height, int max, int progress, float partialTick, long seed) {
        Random random = new Random(seed);
        float size = 16;
        float x = width / 4 + random.nextInt(32);
        float y = height / 3 + random.nextInt(32);
        float elapsed = 0;
        if (progress < max * .66f) {
            float q = (progress - partialTick) / (max * .66f);
            size *= q;
            float motion = (float) Math.sin(q * Math.PI - Math.PI / 2) * .5f + .5f;
            y *= motion;
            x *= (float) Math.sin(motion * Math.PI * .5);
        } else {
            elapsed = max - progress + partialTick;
            float pulse = elapsed / (max * .33f);
            float motion = (float) Math.sin(pulse * Math.PI * 2 - Math.PI / 2) * .5f + 1.5f;
            if (pulse < .5f) size *= pulse * 2;
            size *= motion;
        }
        float xx = width - 12 + random.nextInt(8) - x;
        float yy = height - 12 + random.nextInt(8) - y;
        // UtilsFX's centered quad has its UVs rotated by 90 degrees. These are
        // ordinary upright GUI blits, hence the additional +90 degree correction.
        float rotation = 90 - (84 + random.nextInt(12));
        Flash flash = null;
        if (progress > max * .9f) {
            float phase = elapsed / (max * .1f);
            flash = flash(random, 64 * pulse(phase), rotation);
        }
        if (progress < max * .1f) {
            float phase = 1 - (progress - partialTick) / (max * .1f);
            flash = flash(random, 32 * pulse(phase), rotation);
        }
        return new Frame(xx, yy, size, rotation, flash);
    }

    private static float pulse(float phase) {
        return (float) Math.sin(phase * Math.PI * 2 - Math.PI / 2) * .25f + .25f;
    }

    private static Flash flash(Random random, float size, float rotation) {
        float angle = rotation - random.nextInt(360);
        float green = (189 + random.nextInt(67)) / 255f;
        float blue = (64 + random.nextInt(192)) / 255f;
        return new Flash(size, angle, green, blue, 320 + random.nextInt(16));
    }

    private KnowledgeGainAnimation() {}
}
