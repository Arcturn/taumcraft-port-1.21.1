package thaumcraft.port.auromancy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.focus.medium.FocusTargeting;


public final class EntityFocusProjectile extends SpellEntity {
    private static final EntityDataAccessor<Integer> SPECIAL=SynchedEntityData.defineId(EntityFocusProjectile.class,EntityDataSerializers.INT);
    private Entity target;
    public EntityFocusProjectile(EntityType<? extends EntityFocusProjectile> type,Level world) { super(type,world); }
    public EntityFocusProjectile(FocusPackage pack,float speed,Trajectory path,int special) {
        this(AuromancyEntities.PROJECTILE.get(),pack.world); setFocusPackage(pack); setSpecial(special);
        setPos(path.source.add(path.direction.scale(pack.getCaster().getBbWidth()*2.1)));
        setDeltaMovement(path.direction.normalize().scale(speed));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(SPECIAL,0); }
    public int getSpecial() { return entityData.get(SPECIAL); }
    public void setSpecial(int value) { entityData.set(SPECIAL,Mth.clamp(value,0,3)); }
    @Override public Vec3 getLookAngle() { return getDeltaMovement().normalize(); }
    public static Vec3 reflected(Vec3 velocity,net.minecraft.core.Direction.Axis axis) {
        return switch(axis) { case X -> velocity.multiply(-.9,.9,.9); case Y -> velocity.multiply(.9,-.81,.9); case Z -> velocity.multiply(.9,.9,-.9); };
    }
    @Override public void tick() {
        super.tick();
        if (tickCount>1200 || (!level().isClientSide && getCaster()==null)) {discard(); return;}
        var velocity=getDeltaMovement(); var previous=position();
        if (!level().isClientSide) {
            var hit=FocusTargeting.ray(level(),this,tickCount<5?getCaster():null,previous,velocity,velocity.length(),.3);
            if (hit.getType()!=HitResult.Type.MISS) {
                if (getSpecial()==1 && hit instanceof BlockHitResult block) {
                    var incoming=velocity;velocity=reflected(velocity,block.getDirection().getAxis());
                    setPos(previous.subtract(incoming).add(velocity).subtract(velocity.normalize().scale(.05)));
                    playSound(SoundEvents.SLIME_JUMP,.25f,1);
                    if (velocity.length()<.2) {discard(); return;}
                } else {
                    if (hit instanceof EntityHitResult entity) hit=new EntityHitResult(entity.getEntity(),previous);
                    SpellExecution.schedule(getFocusPackage(),new Trajectory[]{new Trajectory(previous,velocity.normalize())},new HitResult[]{hit},0);
                    discard(); return;
                }
            } else setPos(previous.add(velocity));
        } else setPos(previous.add(velocity));
        double horizontal=velocity.horizontalDistance();
        setYRot((float)(Mth.atan2(velocity.x,velocity.z)*180/Math.PI)); setXRot((float)(Mth.atan2(velocity.y,horizontal)*180/Math.PI));
        setDeltaMovement(velocity.scale(isInWater()?.8:.99).add(0,getSpecial()>1?-.005:-.01,0));
        if (!level().isClientSide && getSpecial()>1) steer();
        if (level().isClientSide) FocusMediaVisuals.entityTick(this);
    }
    private void steer() {
        if (target==null && tickCount%5==0) for (var candidate:FocusTargeting.nearby(level(),this,16)) {
            if (FocusTargeting.visible(this,candidate,16,true) && FocusTargeting.friendly(getCaster(),candidate)==(getSpecial()==3)) {target=candidate;break;}
        }
        if (target!=null) {
            var direction=target.position().add(0,target.getBbHeight()*.6,0).subtract(position()).normalize();
            var movement=getDeltaMovement(); setDeltaMovement(movement.normalize().add(direction.scale(.275)).normalize().scale(movement.length()));
            if (tickCount%5==0 && (!target.isAlive() || !FocusTargeting.visible(this,target,16,true))) target=null;
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("special",getSpecial());}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);setSpecial(tag.getInt("special"));}
}
