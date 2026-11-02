package thaumcraft.port.auromancy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.focus.medium.FocusTargeting;


public final class EntityFocusMine extends SpellEntity {
    private static final EntityDataAccessor<Boolean> ARMED=SynchedEntityData.defineId(EntityFocusMine.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ARMING=SynchedEntityData.defineId(EntityFocusMine.class,EntityDataSerializers.INT);
    private boolean friendly;
    public EntityFocusMine(EntityType<? extends EntityFocusMine> type,Level world) {super(type,world);}
    public EntityFocusMine(FocusPackage pack,Trajectory trajectory,boolean friendly) {
        this(AuromancyEntities.MINE.get(),pack.world);setFocusPackage(pack);this.friendly=friendly;setPos(trajectory.source);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(ARMED,false);builder.define(ARMING,40);}
    public boolean getIsArmed() {return entityData.get(ARMED);}
    public void setIsArmed(boolean armed) {entityData.set(ARMED,armed);}
    public int getArmingTicks() {return entityData.get(ARMING);}
    public boolean isFriendly() {return friendly;}
    @Override public void tick() {
        super.tick();
        if (tickCount>1200 || (!level().isClientSide && getCaster()==null)) {discard();return;}
        // EntityThrowable kept moving after onImpact; the source mine settles by
        // its quarter-strength push-out response instead of freezing in midair.
        var velocity=getDeltaMovement();
        var hit=FocusTargeting.ray(level(),this,tickCount<5?getCaster():null,position(),velocity,velocity.length(),.3);
        if (hit.getType()!=HitResult.Type.MISS && getCaster()!=null && !level().isClientSide) setIsArmed(true);
        setPos(position().add(velocity));setDeltaMovement(velocity.scale(isInWater()?.8:.99).add(0,-.01,0));
        settleOutOfBlock();
        if (!getIsArmed()) return;
        if (getArmingTicks()>0) entityData.set(ARMING,getArmingTicks()-1);
        if (getArmingTicks()>0 || tickCount%5!=0) return;
        if (level().isClientSide) {FocusMediaVisuals.entityTick(this);return;}
        int delay=0;
        for (var candidate:FocusTargeting.nearby(level(),this,1)) {
            if (FocusTargeting.friendly(getCaster(),candidate)!=friendly) continue;
            var midpoint=candidate.position().add(0,candidate.getBbHeight()/2,0);
            SpellExecution.schedule(getFocusPackage().copy(getCaster()),new Trajectory[]{new Trajectory(position(),midpoint.subtract(position()).normalize())},new HitResult[]{new EntityHitResult(candidate,midpoint)},delay++);
        }
        if (delay>0) discard();
    }
    private void settleOutOfBlock() {
        var pos=blockPosition();
        if (!level().getBlockState(pos).isCollisionShapeFullBlock(level(),pos)) return;
        net.minecraft.core.Direction exit=net.minecraft.core.Direction.UP;double nearest=Double.MAX_VALUE;
        for (var direction:net.minecraft.core.Direction.values()) {
            var neighbor=pos.relative(direction);
            if (level().getBlockState(neighbor).isCollisionShapeFullBlock(level(),neighbor)) continue;
            double coordinate=switch(direction.getAxis()){case X -> getX()-pos.getX();case Y -> getY()-pos.getY();case Z -> getZ()-pos.getZ();};
            double distance=direction.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?1-coordinate:coordinate;
            if(distance<nearest){nearest=distance;exit=direction;}
        }
        double impulse=random.nextFloat()*.2+.1;var velocity=getDeltaMovement();
        setDeltaMovement(switch(exit.getAxis()) {case X -> new net.minecraft.world.phys.Vec3(exit.getStepX()*impulse,velocity.y,velocity.z).scale(.25);case Y -> new net.minecraft.world.phys.Vec3(velocity.x,exit.getStepY()*impulse,velocity.z).scale(.25);case Z -> new net.minecraft.world.phys.Vec3(velocity.x,velocity.y,exit.getStepZ()*impulse).scale(.25);});
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putBoolean("armed",getIsArmed());tag.putInt("Arming",getArmingTicks());tag.putBoolean("friendly",friendly);}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);setIsArmed(tag.getBoolean("armed"));entityData.set(ARMING,tag.contains("Arming")?Math.max(0,tag.getInt("Arming")):(getIsArmed()?0:40));friendly=tag.getBoolean("friendly");}
}
