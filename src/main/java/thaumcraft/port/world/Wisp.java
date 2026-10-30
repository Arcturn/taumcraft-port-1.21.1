package thaumcraft.port.world;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.network.WispZapPayload;

/** TC6's neutral-until-provoked flying monster and its probabilistic zap attack. */
public final class Wisp extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.STRING);
    private BlockPos wander;
    private int aggression, attackCounter;

    public Wisp(EntityType<? extends Wisp> type, Level level) {
        super(type, level);
        xpReward = 5;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 22).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 16).add(Attributes.MOVEMENT_SPEED, .25);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ASPECT, "");
    }
    public boolean hasAspect() { return Aspect.byId(entityData.get(ASPECT)).isPresent(); }
    public Aspect aspect() { return Aspect.byId(entityData.get(ASPECT)).orElse(Aspect.AIR); }
    public void aspect(Aspect aspect) { entityData.set(ASPECT, aspect.id()); }
    @Override protected void registerGoals() {}
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public int getMaxSpawnClusterSize() { return 2; }
    @Override protected float getSoundVolume() { return .25f; }
    @Override protected SoundEvent getAmbientSound() { return PortSounds.get("wisplive"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.LAVA_EXTINGUISH; }
    @Override protected SoundEvent getDeathSound() { return PortSounds.get("wispdead"); }

    @Override public boolean hurt(DamageSource source, float amount) {
        // The original retaliates even when the damage itself is rejected by hurt resistance.
        if (source.getEntity() instanceof LivingEntity attacker) {
            setTarget(attacker);
            aggression = 200;
        }
        return super.hurt(source, amount);
    }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) WispVisualBridge.death.accept(this);
    }

    public static boolean canSpawn(EntityType<Wisp> type, ServerLevelAccessor level, MobSpawnType reason,
                                   BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getBrightness(LightLayer.SKY, pos) <= random.nextInt(32)
                && level.getMaxLocalRawBrightness(pos) <= random.nextInt(8)
                && level.getLevel().getEntitiesOfClass(Wisp.class, new AABB(pos).inflate(16)).size() < 8;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) WispVisualBridge.tick.accept(this);
        if (level() instanceof ServerLevel server && isEffectiveAi()) {
            if (server.getDifficulty() == Difficulty.PEACEFUL) {
                discard();
                return;
            }
            if (!hasAspect()) {
                boolean primal = random.nextInt(10) != 0;
                var pool = Arrays.stream(Aspect.values()).filter(aspect -> aspect.isPrimal() == primal).toList();
                aspect(pool.get(random.nextInt(pool.size())));
            }
            // In TC6 onLivingUpdate runs after normal flying travel, before onUpdate damps Y.
            updateFlight(server);
            updateTarget(server);
            updateAttack(server);
        }
        setDeltaMovement(getDeltaMovement().multiply(1, (double) .6f, 1));
    }

    private void updateFlight(ServerLevel server) {
        LivingEntity target = getTarget();
        boolean visible = target != null && hasLineOfSight(target);
        Vec3 destination = null;
        if (target == null || !visible) {
            if (wander != null && (!server.isEmptyBlock(wander) || wander.getY() < server.getMinBuildHeight() + 1
                    || wander.getY() > server.getHeight(Heightmap.Types.MOTION_BLOCKING, wander.getX(), wander.getZ()) + 8))
                wander = null;
            if (wander == null || random.nextInt(30) == 0 || position().distanceToSqr(Vec3.atCenterOf(wander)) < 4)
                wander = new BlockPos((int) getX() + random.nextInt(7) - random.nextInt(7),
                        (int) getY() + random.nextInt(6) - 2, (int) getZ() + random.nextInt(7) - random.nextInt(7));
            destination = new Vec3(wander.getX() + .5, wander.getY() + .1, wander.getZ() + .5);
            zza = .15f;
        } else if (distanceToSqr(target) > 128) {
            destination = target.position().add(0, target.getEyeHeight() * .66f, 0);
            zza = .5f;
        }
        // A visible target inside sqrt(128) blocks does not add a braking acceleration.
        if (destination != null) {
            Vec3 difference = destination.subtract(position());
            Vec3 velocity = getDeltaMovement();
            setDeltaMovement(velocity.add((Math.signum(difference.x) * .5 - velocity.x) * .1f,
                    (Math.signum(difference.y) * .7f - velocity.y) * .1f,
                    (Math.signum(difference.z) * .5 - velocity.z) * .1f));
            setYRot((float) (Math.atan2(getDeltaMovement().z, getDeltaMovement().x) * 180 / Math.PI) - 90);
        }
    }

    private void updateTarget(ServerLevel server) {
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player player
                && (player.getAbilities().invulnerable || player.isSpectator()))) setTarget(null);
        aggression--;
        // Expiry permits the next random retarget; it does not immediately forget an attacker.
        if (random.nextInt(1000) == 0 && (getTarget() == null || aggression-- <= 0)) {
            var nearest = server.players().stream().filter(player -> player.isAlive()
                            && !player.getAbilities().invulnerable && !player.isSpectator() && distanceToSqr(player) <= 256)
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(nearest);
            if (nearest != null) aggression = 50;
        }
    }

    private void updateAttack(ServerLevel server) {
        LivingEntity target = getTarget();
        // Outside the attack radius TC6 freezes its counter, including the negative cooldown.
        if (!isAlive() || target == null || distanceToSqr(target) >= 256) return;
        setYRot(-(float) Math.atan2(target.getX() - getX(), target.getZ() - getZ()) * 180 / (float) Math.PI);
        yBodyRot = getYRot();
        if (hasLineOfSight(target)) {
            if (++attackCounter == 20) {
                playSound(PortSounds.get("zap"), 1, 1.1f);
                WispZapPayload.send(server, this, target);
                Vec3 motion = target.getDeltaMovement();
                boolean moving = Math.abs(motion.x) > .1f || Math.abs(motion.y) > .1f || Math.abs(motion.z) > .1f;
                if (random.nextFloat() < (moving ? .4f : .66f))
                    target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + (moving ? 0 : 1));
                attackCounter = -20 + random.nextInt(20);
            }
        } else if (attackCounter > 0) attackCounter--;
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (hasAspect()) spawnAtLocation(CrystalItem.stack(aspect()));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("aspect", entityData.get(ASPECT));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ASPECT, tag.getString("aspect"));
    }
}
