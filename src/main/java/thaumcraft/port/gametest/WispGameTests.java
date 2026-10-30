package thaumcraft.port.gametest;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.world.Wisp;
import thaumcraft.port.world.WorldEntities;

/** Exercises real flying-mob ticks, line-of-sight, damage and synchronized entity state. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WispGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void retaliationSurvivesRejectedDamageAndDoesNotExpireImmediately(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        var first = target(helper, wisp);
        var second = target(helper, wisp);
        wisp.hurt(helper.getLevel().damageSources().mobAttack(first), 1);
        helper.assertTrue(wisp.getTarget() == first, "Wisp did not retaliate against its attacker");
        helper.assertFalse(wisp.hurt(helper.getLevel().damageSources().mobAttack(second), 1),
                "Fixture did not reject a second hit inside hurt resistance");
        helper.assertTrue(wisp.getTarget() == second, "A rejected hit did not update the original retaliation target");
        helper.assertValueEqual(integer(wisp, "aggression"), 200, "Retaliation did not grant 200 ticks before random retargeting");
        integer(wisp, "aggression", 1);
        for (int i = 0; i < 10; i++) step(wisp, second, 42);
        helper.assertTrue(wisp.getTarget() == second, "Expiry immediately forgot the attacker instead of permitting a random retarget");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void firstZapChargesTwentyTicksThenUsesOriginalCooldown(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        LivingEntity target = target(helper, wisp);
        wisp.setTarget(target);
        for (int i = 1; i < 20; i++) {
            step(wisp, target, 42);
            helper.assertValueEqual(integer(wisp, "attackCounter"), i, "Charge advanced by the wrong number of ticks");
        }
        helper.assertTrue(target.getHealth() == target.getMaxHealth(), "Zap damaged its target before tick twenty");
        step(wisp, target, 42);
        int cooldown = integer(wisp, "attackCounter");
        helper.assertTrue(cooldown >= -20 && cooldown <= -1, "Zap did not reset into the original negative cooldown");
        int interval = 20 - cooldown;
        helper.assertTrue(interval >= 21 && interval <= 40, "Next visible zap was outside the 21–40 tick interval");
        for (int i = 1; i < interval; i++) {
            step(wisp, target, 42);
            helper.assertValueEqual(integer(wisp, "attackCounter"), cooldown + i, "Cooldown fired early or stopped charging");
        }
        step(wisp, target, 42);
        helper.assertTrue(integer(wisp, "attackCounter") < 0, "Second zap did not fire at the original cooldown boundary");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void zapUsesAttackAttributeAndMovementPenaltyWithPhysicalDamage(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        LivingEntity target = target(helper, wisp);
        wisp.setTarget(target);
        wisp.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7);
        assertDamage(helper, wisp, target, new Vec3(.1f, 0, 0), 8);
        assertDamage(helper, wisp, target, new Vec3(0, .1001, 0), 7);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void rangeFreezesChargeAndOcclusionOnlyDecaysPositiveCharge(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        LivingEntity target = target(helper, wisp);
        wisp.setTarget(target);
        target.setPos(wisp.getX() + 16, wisp.getY(), wisp.getZ());
        integer(wisp, "attackCounter", 12);
        step(wisp, target, 42);
        helper.assertValueEqual(integer(wisp, "attackCounter"), 12, "Exact sixteen-block boundary must not charge or decay");
        target.setPos(wisp.getX() + 4, wisp.getY(), wisp.getZ());
        BlockPos wall = BlockPos.containing(wisp.getX() + 2, wisp.getY(), wisp.getZ());
        for (int y = -1; y <= 4; y++) helper.getLevel().setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
        helper.assertFalse(wisp.hasLineOfSight(target), "Occlusion fixture did not block eye-to-eye visibility");
        step(wisp, target, 42);
        helper.assertValueEqual(integer(wisp, "attackCounter"), 11, "Blocked sight did not decay a positive charge by one");
        integer(wisp, "attackCounter", -7);
        step(wisp, target, 42);
        helper.assertValueEqual(integer(wisp, "attackCounter"), -7, "Blocked sight incorrectly consumed a negative cooldown");
        for (int y = -1; y <= 4; y++) helper.getLevel().setBlockAndUpdate(wall.above(y), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void nearbyTargetDoesNotBrakeButDistantTargetAccelerates(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        LivingEntity target = target(helper, wisp);
        wisp.setTarget(target);
        Vec3 origin = wisp.position();
        wisp.setDeltaMovement(.2, 0, 0);
        wisp.getRandom().setSeed(42);
        wisp.tick();
        helper.assertTrue(Math.abs(wisp.getDeltaMovement().x - .2 * .91f) < .00001,
                "Nearby visible target added braking beyond normal FlyingMob drag: " + wisp.getDeltaMovement());
        wisp.setPos(origin);
        wisp.setDeltaMovement(Vec3.ZERO);
        target.setPos(origin.add(12, 0, 0));
        wisp.getRandom().setSeed(42);
        wisp.tick();
        helper.assertTrue(Math.abs(wisp.getDeltaMovement().x - .05) < .00001,
                "Distant target did not receive original .05 horizontal acceleration");
        helper.assertTrue(Math.abs(wisp.getDeltaMovement().y - .7f * .1f * .6f) < .00001,
                "Vertical steering/damping order differs from the original");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void noAiStopsCombatAndInvalidTargetsAreReleased(GameTestHelper helper) {
        Wisp wisp = wisp(helper);
        LivingEntity target = target(helper, wisp);
        wisp.setTarget(target);
        integer(wisp, "attackCounter", 19);
        wisp.setNoAi(true);
        step(wisp, target, 42);
        helper.assertValueEqual(integer(wisp, "attackCounter"), 19, "NoAI wisp still charged or attacked");
        helper.assertTrue(field(wisp, "wander") == null, "NoAI wisp chose a flight waypoint");
        wisp.setNoAi(false);
        var player = GameTestPlayers.create(helper);
        player.setPos(target.position());
        player.setGameMode(GameType.CREATIVE);
        wisp.setTarget(player);
        step(wisp, player, 42);
        helper.assertTrue(wisp.getTarget() == null, "Wisp retained an invulnerable creative player");
        target.setHealth(0);
        wisp.setTarget(target);
        step(wisp, target, 42);
        helper.assertTrue(wisp.getTarget() == null, "Wisp retained a dead target");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aspectRoundTripRetainsUnsetStateAndWispHasOriginalImmunities(GameTestHelper helper) {
        Wisp wisp = new Wisp(WorldEntities.WISP.get(), helper.getLevel());
        var tag = new CompoundTag();
        wisp.addAdditionalSaveData(tag);
        helper.assertValueEqual(tag.getString("aspect"), "", "Uninitialized wisp was silently converted to Aer on save");
        wisp.aspect(Aspect.FIRE);
        wisp.addAdditionalSaveData(tag);
        Wisp loaded = new Wisp(WorldEntities.WISP.get(), helper.getLevel());
        loaded.readAdditionalSaveData(tag);
        helper.assertTrue(loaded.hasAspect() && loaded.aspect() == Aspect.FIRE, "Wisp aspect was not preserved by save/load");
        helper.assertValueEqual(loaded.getMaxSpawnClusterSize(), 2, "Wisp cluster cap differs from TC6");
        helper.assertFalse(loaded.fireImmune(), "Port granted fire immunity absent from original EntityWisp");
        try {
            var decreaseAir = Wisp.class.getDeclaredMethod("decreaseAirSupply", int.class);
            decreaseAir.setAccessible(true);
            helper.assertValueEqual((int) decreaseAir.invoke(loaded, 17), 17, "Original wisp must not consume air underwater");
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        helper.succeed();
    }

    private static Wisp wisp(GameTestHelper helper) {
        // Run above the template's invisible barrier roof, without clearing adjacent tests.
        Wisp wisp = new Wisp(WorldEntities.WISP.get(), helper.getLevel());
        wisp.aspect(Aspect.AIR);
        wisp.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 20, 1))));
        return wisp;
    }
    private static LivingEntity target(GameTestHelper helper, Wisp wisp) {
        var target = EntityType.IRON_GOLEM.create(helper.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        target.setPos(wisp.position().add(4, 0, 0));
        target.setNoAi(true);
        helper.assertTrue(helper.getLevel().noCollision(wisp, wisp.getBoundingBox()),
                "Wisp starts inside an obstacle: " + wisp.position() + " block=" + helper.getLevel().getBlockState(wisp.blockPosition()));
        helper.assertTrue(wisp.hasLineOfSight(target),
                "Fixture has no initial line of sight: " + wisp.getEyePosition() + " -> " + target.getEyePosition());
        return target;
    }
    private static void step(Wisp wisp, LivingEntity target, long seed) {
        wisp.setDeltaMovement(Vec3.ZERO);
        wisp.getRandom().setSeed(seed);
        wisp.tickCount++;
        wisp.tick();
    }
    private static void assertDamage(GameTestHelper helper, Wisp wisp, LivingEntity target, Vec3 motion, int expected) {
        boolean hit = false, missed = false;
        for (int sample = 0; sample < 128 && !(hit && missed); sample++) {
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            target.setDeltaMovement(motion);
            integer(wisp, "attackCounter", 19);
            step(wisp, target, sample);
            float lost = target.getMaxHealth() - target.getHealth();
            if (lost == 0) missed = true;
            else {
                helper.assertTrue(Math.abs(lost - expected) < .0001, "Zap ignored movement or attack attribute: damage=" + lost);
                helper.assertTrue(target.getLastDamageSource().is(DamageTypes.MOB_ATTACK), "Zap used magic damage instead of mob-attack damage");
                hit = true;
            }
        }
        helper.assertTrue(hit && missed, "Original zap should sometimes hit and sometimes miss");
    }
    private static Object field(Wisp wisp, String name) {
        try { Field field = Wisp.class.getDeclaredField(name); field.setAccessible(true); return field.get(wisp); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    private static int integer(Wisp wisp, String name) { return (int) field(wisp, name); }
    private static void integer(Wisp wisp, String name, int value) {
        try { Field field = Wisp.class.getDeclaredField(name); field.setAccessible(true); field.setInt(wisp, value); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    private WispGameTests() {}
}
