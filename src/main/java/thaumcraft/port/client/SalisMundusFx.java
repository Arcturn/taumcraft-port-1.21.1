package thaumcraft.port.client;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.network.SalisMundusFxPayload;

/** Client presentation only: block changes, dust payment and sounds remain server-owned. */
public final class SalisMundusFx {
    public static void receive(SalisMundusFxPayload payload) {
        var game = Minecraft.getInstance();
        var level = game.level;
        if (level == null || game.player == null || !payload.valid()
                || game.player.position().distanceToSqr(payload.hit()) > 64 * 64) return;
        if (payload.phase() == SalisMundusFxPayload.START) {
            Entity user = level.getEntity(payload.playerId());
            if (user != null) handSparkles(level, handPosition(user, payload.hand()), Vec3.atCenterOf(payload.target()));
            var coated = new ArrayList<>(payload.blocks());
            if (coated.size() == 2) {
                BlockPos below = coated.stream().min(Comparator.comparingInt(BlockPos::getY)).orElseThrow().below();
                // The original thaumatorium trigger also sparkles its unchanged crucible.
                if (level.getBlockState(below).is(MachineContent.CRUCIBLE.get())) coated.add(below);
            }
            for (BlockPos block : coated) blockSparkles(level, block, payload.hit());
        } else if (payload.phase() == SalisMundusFxPayload.FINISH) {
            boolean fancy = payload.blocks().size() == 1;
            for (BlockPos block : payload.blocks()) bamf(level, Vec3.atCenterOf(block), fancy);
        }
    }

    private static Vec3 handPosition(Entity player, InteractionHand hand) {
        float yaw = (player.getYRot() + (hand == InteractionHand.MAIN_HAND ? 0 : 180)) * Mth.DEG_TO_RAD;
        return new Vec3(player.getX() - Mth.cos(yaw) * .3,
                player.getBoundingBox().minY + player.getBbHeight() / 2 + .25,
                player.getZ() - Mth.sin(yaw) * .3).add(player.getViewVector(1).scale(.3));
    }

    private static void handSparkles(ClientLevel level, Vec3 hand, Vec3 target) {
        var random = level.random;
        Vec3 movement = target.subtract(hand).scale(1.0 / 6);
        for (int i = 0; i < 50; i++) {
            boolean floaty = i < 50 / 3;
            float green = (189 + random.nextInt(67)) / 255f;
            float blue = (64 + random.nextInt(192)) / 255f;
            var velocity = movement.add(random.nextGaussian() * .05,
                    random.nextGaussian() * .05 + (floaty ? .05 : .15), random.nextGaussian() * .05);
            SalisMundusParticle particle = create(hand, velocity);
            if (particle != null) particle.sparkle(.5f, 1, green, blue, random.nextInt(5),
                    floaty ? .3f + random.nextFloat() * .5f : .85f, floaty ? .2f : .5f);
        }
    }

    private static void blockSparkles(ClientLevel level, BlockPos block, Vec3 hit) {
        var shape = level.getBlockState(block).getShape(level, block);
        if (shape.isEmpty()) return;
        var bounds = shape.bounds();
        int count = (int) (bounds.getSize() * 20) * 2;
        var random = level.random;
        for (Direction face : Direction.values()) {
            BlockPos adjacent = block.relative(face);
            var state = level.getBlockState(adjacent);
            if (state.isSolidRender(level, adjacent) || state.isFaceSturdy(level, adjacent, face.getOpposite())) continue;
            for (int i = 0; i < count; i++) {
                double x = .5 + face.getStepX() * .51;
                double y = .5 + face.getStepY() * .51;
                double z = .5 + face.getStepZ() * .51;
                if (face.getStepX() == 0) x += random.nextGaussian() * .6;
                if (face.getStepY() == 0) y += random.nextGaussian() * .6;
                if (face.getStepZ() == 0) z += random.nextGaussian() * .6;
                Vec3 point = new Vec3(block.getX() + Mth.clamp(x, bounds.minX, bounds.maxX),
                        block.getY() + Mth.clamp(y, bounds.minY, bounds.maxY),
                        block.getZ() + Mth.clamp(z, bounds.minZ, bounds.maxZ));
                float green = (189 + random.nextInt(67)) / 255f;
                float blue = (64 + random.nextInt(192)) / 255f;
                int delay = (int) (random.nextInt(5) + point.distanceTo(hit) * 16);
                float size = .4f + (float) random.nextGaussian() * .1f;
                SalisMundusParticle particle = create(point, new Vec3(0, .0025, 0));
                if (particle != null) particle.sparkle(size, 1, green, blue, delay, 1, .01f);
            }
        }
    }

    private static void bamf(ClientLevel level, Vec3 center, boolean fancy) {
        bamf(level, center, fancy, null);
    }

    static void bamf(ClientLevel level, Vec3 center, boolean fancy, Direction direction) {
        var random = level.random;
        // The original loops re-sample their random upper bound each iteration.
        for (int i = 0; i < 8 + random.nextInt(3); i++) {
            Vec3 velocity = new Vec3(signed(random, .05f, .05f), signed(random, .05f, .05f), signed(random, .05f, .05f));
            if (direction != null) velocity = velocity.add(direction.getStepX() * .1, direction.getStepY() * .1, direction.getStepZ() * .1);
            SalisMundusParticle particle = create(center.add(velocity.scale(2)), velocity.scale(.5));
            if (particle != null) particle.smoke(tint(random, .5f), tint(random, .1f), tint(random, .6f));
        }
        if (fancy) {
            for (int i = 0; i < 2 + random.nextInt(3); i++) {
                Vec3 velocity = new Vec3(signed(random, .025f, .025f), signed(random, .025f, .025f), signed(random, .025f, .025f));
                SalisMundusParticle particle = create(center.add(velocity.scale(2)), velocity);
                if (particle != null) particle.mote(15 + random.nextInt(10));
            }
            SalisMundusParticle particle = create(center, Vec3.ZERO);
            if (particle != null) particle.flash();
        }
        for (int i = 0; i < (fancy ? 2 : 0) + random.nextInt(3); i++) {
            float red = (.9f + random.nextFloat() * .1f + .5f) / 2;
            float green = .1f;
            float blue = (.5f + random.nextFloat() * .1f + .6f) / 2;
            Vec3 velocity = new Vec3(signed(random, .0025f, .005f), signed(random, .0025f, .005f), signed(random, .0025f, .005f));
            if (direction != null) velocity = velocity.add(direction.getStepX() * .025, direction.getStepY() * .025, direction.getStepZ() * .025);
            SalisMundusParticle particle = create(center.add(velocity.scale(5)), velocity);
            if (particle != null) particle.wisp(i, red, green, blue);
        }
    }

    private static float signed(RandomSource random, float base, float range) {
        return (base + random.nextFloat() * range) * (random.nextBoolean() ? -1 : 1);
    }
    private static float tint(RandomSource random, float colour) {
        return Mth.clamp(colour * (1 + (float) random.nextGaussian() * .1f), 0, 1);
    }
    private static SalisMundusParticle create(Vec3 position, Vec3 velocity) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("salis_mundus"),
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        return particle instanceof SalisMundusParticle dust ? dust : null;
    }
    private SalisMundusFx() {}
}
