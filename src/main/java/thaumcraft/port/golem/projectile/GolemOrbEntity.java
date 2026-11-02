package thaumcraft.port.golem.projectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import thaumcraft.port.content.PortSounds;
/** TC6 golem homing orb, including deflection and red/blue lifetime/damage. */
public final class GolemOrbEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(GolemOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(GolemOrbEntity.class, EntityDataSerializers.BOOLEAN);
    public GolemOrbEntity(EntityType<? extends GolemOrbEntity> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(TARGET, -1); builder.define(RED, false); }
    public int getTargetId() { return entityData.get(TARGET); }
    public void setTarget(LivingEntity target) { entityData.set(TARGET, target == null ? -1 : target.getId()); }
    public boolean isRed() { return entityData.get(RED); }
    public void setRed(boolean red) { entityData.set(RED, red); }
    @Override protected double getDefaultGravity() { return 0; }
    @Override public void tick() {
        super.tick(); if (tickCount > (isRed() ? 240 : 160)) discard();
        var target = level().getEntity(getTargetId());
        if (target instanceof LivingEntity living && living.isAlive()) {
            double distance = Math.max(.0001, distanceToSqr(target));
            var velocity = getDeltaMovement().add((target.getX() - getX()) / distance * .2,
                    (target.getBoundingBox().minY + target.getBbHeight() * .6 - getY()) / distance * .2,
                    (target.getZ() - getZ()) / distance * .2);
            setDeltaMovement(Mth.clamp(velocity.x, -.25, .25), Mth.clamp(velocity.y, -.25, .25), Mth.clamp(velocity.z, -.25, .25));
        }
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && getOwner() instanceof LivingEntity owner && hit instanceof EntityHitResult entityHit)
            entityHit.getEntity().hurt(damageSources().indirectMagic(this, owner), (float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * (isRed() ? 1 : .6f));
        playSound(PortSounds.get("shock"), 1, 1 + (random.nextFloat() - random.nextFloat()) * .2f);
        if (!level().isClientSide) { level().broadcastEntityEvent(this, (byte)3); discard(); }
    }
    @Override public void handleEntityEvent(byte event) { if (event == 3 && level().isClientSide) thaumcraft.port.golem.GolemVisuals.orbBurst.accept(this); else super.handleEntityEvent(event); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.getEntity() == null) return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9)); playSound(PortSounds.get("zap"), 1, 1 + (random.nextFloat() - random.nextFloat()) * .2f); return true;
    }
    @Override public boolean isPickable() { return true; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("red", isRed()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setRed(tag.getBoolean("red")); }
}
