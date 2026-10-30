package thaumcraft.port.client;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.machine.CrucibleEntity;

/** The original continuous froth and block-event splashes, with no server particle spam. */
public final class CrucibleFx {
    /** PacketFXPollute presentation, also usable after the source block has been removed. */
    public static void pollution(BlockPos pos, int count) {
        var game = Minecraft.getInstance();
        var level = game.level;
        if (level == null || game.player == null || count <= 0
                || game.player.position().distanceToSqr(Vec3.atLowerCornerOf(pos)) > 32 * 32) return;
        for (int i = 0; i < Math.min(40, count); i++) {
            var preset = CrucibleVisualRules.pollution(level.random);
            var point = Vec3.atLowerCornerOf(pos).add(preset.offset());
            var velocity = preset.velocity();
            // Reuse the unmodified TC6 atlas and alpha particle layer.
            var particle = game.particleEngine.createParticle(PortParticles.get("salis_mundus"),
                    point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
            if (particle instanceof SalisMundusParticle smoke) smoke.pollution(preset);
        }
        // Periodic leaks have no sound in TC6; manual emptying separately sends event 2.
    }

    public static void tick(CrucibleEntity be) {
        if (!(be.getLevel() instanceof ClientLevel level) || be.state.water() <= 0) return;
        var random = level.random;
        var pos = be.getBlockPos();
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (be.state.boiling()) {
            bubble(level, new Vec3(x + .2 + random.nextFloat() * .6, y + be.state.fluidHeight(),
                    z + .2 + random.nextFloat() * .6), CrucibleVisualRules.Kind.FROTH, 1, .5f, .5f, .7f);
            if (be.state.total() > 500) for (int i = 0; i < 2; i++) {
                overflow(level, x, y + 1, z + random.nextFloat());
                overflow(level, x + 1, y + 1, z + random.nextFloat());
                overflow(level, x + random.nextFloat(), y + 1, z);
                overflow(level, x + random.nextFloat(), y + 1, z + 1);
            }
        }
        if (random.nextInt(6) == 0 && !be.state.aspects().isEmpty()) {
            int colour = randomColour(be, level);
            double px = x + (5 + random.nextInt(22)) / 32f + .015625f;
            double pz = z + (5 + random.nextInt(22)) / 32f + .015625f;
            coloured(level, new Vec3(px, y + .05 + be.state.fluidHeight(), pz),
                    CrucibleVisualRules.Kind.BUBBLE, 1, colour);
        }
    }

    public static boolean blockEvent(CrucibleEntity be, int id, int strength) {
        if (!(be.getLevel() instanceof ClientLevel level)) return false;
        var pos = be.getBlockPos();
        if (id == 99 || id == 1) {
            Vec3 centre = Vec3.atCenterOf(pos).add(0, id == 99 ? .75 : 1, 0);
            SalisMundusFx.bamf(level, centre, true, Direction.UP);
            level.playLocalSound(centre.x, centre.y, centre.z, PortSounds.get("poof"), SoundSource.BLOCKS,
                    .4f, 1 + (float)level.random.nextGaussian() * .05f, false);
            if (id == 99) spillSound(level, Vec3.atCenterOf(pos));
            return true;
        }
        if (id == 2) {
            spillSound(level, Vec3.atCenterOf(pos));
            // Original event calls crucibleBoil ten times, each of which emits two bubbles.
            for (int i = 0; i < 20; i++) {
                var random = level.random;
                var point = new Vec3(pos.getX() + .2 + random.nextFloat() * .6,
                        pos.getY() + .1 + be.state.fluidHeight(), pos.getZ() + .2 + random.nextFloat() * .6);
                coloured(level, point, CrucibleVisualRules.Kind.BOIL, strength, randomColour(be, level));
            }
            return true;
        }
        return false;
    }

    private static int randomColour(CrucibleEntity be, ClientLevel level) {
        if (be.state.aspects().isEmpty()) return 0xffffff;
        var aspects = new ArrayList<Aspect>(be.state.aspects().keySet());
        return aspects.get(level.random.nextInt(aspects.size())).color();
    }
    private static void spillSound(ClientLevel level, Vec3 centre) {
        level.playLocalSound(centre.x, centre.y, centre.z, PortSounds.get("spill"), SoundSource.BLOCKS, .2f, 1, false);
    }
    private static void overflow(ClientLevel level, double x, double y, double z) {
        bubble(level, new Vec3(x,y,z), CrucibleVisualRules.Kind.OVERFLOW, 1, .25f, 0, .75f);
    }
    private static void coloured(ClientLevel level, Vec3 point, CrucibleVisualRules.Kind kind, int strength, int colour) {
        bubble(level, point, kind, strength, ((colour >> 16) & 255) / 255f,
                ((colour >> 8) & 255) / 255f, (colour & 255) / 255f);
    }
    private static void bubble(ClientLevel level, Vec3 point, CrucibleVisualRules.Kind kind, int strength,
                               float red, float green, float blue) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("crucible_bubble"),
                point.x, point.y, point.z, 0, kind == CrucibleVisualRules.Kind.BOIL ? .002 : 0, 0);
        if (particle instanceof SalisMundusParticle bubble)
            bubble.crucible(CrucibleVisualRules.bubble(level.random, kind, strength), red, green, blue);
    }
    private CrucibleFx() {}
}
