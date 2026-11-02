package thaumcraft.port.auromancy.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;

import thaumcraft.port.auromancy.focus.medium.FocusTargeting;

public final class EntitySpellBat extends Mob {
    private static final EntityDataAccessor<CompoundTag> PROGRAM=SynchedEntityData.defineId(EntitySpellBat.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> FRIENDLY=SynchedEntityData.defineId(EntitySpellBat.class,EntityDataSerializers.BOOLEAN);
    private FocusPackage focusPackage;
    private BlockPos flightTarget;
    private int attackTime;
    public EntitySpellBat(EntityType<? extends EntitySpellBat> type,Level level) {super(type,level);xpReward=0;}
    public EntitySpellBat(FocusPackage pack,boolean friendly) {this(AuromancyEntities.BAT.get(),pack.world);setFocusPackage(pack);entityData.set(FRIENDLY,friendly);setHealth(5);}
    public static AttributeSupplier.Builder attributes() {return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.FOLLOW_RANGE,12);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(PROGRAM,new CompoundTag());builder.define(FRIENDLY,false);}
    public void setFocusPackage(FocusPackage pack) {focusPackage=pack;pack.world=level();entityData.set(PROGRAM,pack.serialize());}
    public FocusPackage getFocusPackage() {if(focusPackage==null){focusPackage=new FocusPackage();focusPackage.deserialize(entityData.get(PROGRAM));focusPackage.world=level();}return focusPackage;}
    public LivingEntity getCaster() {return getFocusPackage().getCaster();}
    public boolean isFriendly() {return entityData.get(FRIENDLY);}
    public int getAttackTime() {return attackTime;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {super.onSyncedDataUpdated(key);if(PROGRAM.equals(key))focusPackage=null;}
    @Override protected void registerGoals() { }
    @Override public void tick() {
        super.tick();setDeltaMovement(getDeltaMovement().multiply(1,.6,1));
        if (!level().isClientSide && (tickCount>600 || getCaster()==null)) {discard();return;}
        if (level().isClientSide) FocusMediaVisuals.batTick(this);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (getCaster()==null || !isAlive()) return;
        if (attackTime>0) attackTime--;
        var target=getTarget();
        if (target!=null && (!target.isAlive() || (!isFriendly() && target instanceof Player player && player.isCreative()))) {setTarget(null);target=null;}
        if (target==null) {
            for (var candidate:FocusTargeting.nearby(level(),this,12)) {
                if (candidate instanceof EntitySpellBat bat && bat.getCaster()==getCaster()) continue;
                if (FocusTargeting.friendly(getCaster(),candidate)!=isFriendly()) continue;
                if (!isFriendly() && candidate instanceof Player player && player.isCreative()) continue;
                setTarget(candidate);target=candidate;break;
            }
        }
        double dx,dy,dz;
        if (target==null) {
            if (flightTarget!=null && (!level().isEmptyBlock(flightTarget) || flightTarget.getY()<=level().getMinBuildHeight())) flightTarget=null;
            if (flightTarget==null || random.nextInt(30)==0 || flightTarget.distToCenterSqr(position())<4)
                flightTarget=BlockPos.containing(getX()+random.nextInt(7)-random.nextInt(7),getY()+random.nextInt(6)-2,getZ()+random.nextInt(7)-random.nextInt(7));
            dx=flightTarget.getX()+.5-getX();dy=flightTarget.getY()+.1-getY();dz=flightTarget.getZ()+.5-getZ();
        } else {
            dx=target.getX()-getX();dy=target.getY()+target.getEyeHeight()*.66-getY();dz=target.getZ()-getZ();
            if (attackTime==0 && distanceTo(target)<Math.max(2.5,target.getBbWidth()*1.1) && target.getBoundingBox().maxY>getBoundingBox().minY && target.getBoundingBox().minY<getBoundingBox().maxY && hasLineOfSight(target)) {
                attackTime=40;var midpoint=target.position().add(0,target.getBbHeight()/2,0);
                thaumcraft.port.auromancy.focus.FocusEngine.runFocusPackage(getFocusPackage().copy(getCaster()),new Trajectory[]{new Trajectory(position(),midpoint.subtract(position()))},new HitResult[]{new EntityHitResult(target,midpoint)});
                setHealth(getHealth()-1);playSound(SoundEvents.BAT_HURT,.5f,.9f+random.nextFloat()*.2f);
            }
        }
        var velocity=getDeltaMovement();
        setDeltaMovement(velocity.add((Math.signum(dx)*.5-velocity.x)*.1,(Math.signum(dy)*.7-velocity.y)*.1,(Math.signum(dz)*.5-velocity.z)*.1));
        var movement=getDeltaMovement();float yaw=(float)(Mth.atan2(movement.z,movement.x)*180/Math.PI)-90;
        setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));yBodyRot=getYRot();zza=.5f;
    }
    @Override public boolean isPushable() {return false;}
    @Override public void push(Entity entity) {if(!isFriendly())super.push(entity);}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) {return false;}
    @Override protected float getSoundVolume() {return .1f;}
    @Override public float getVoicePitch() {return super.getVoicePitch()*.95f;}
    @Override public net.minecraft.world.scores.PlayerTeam getTeam() {var caster=getCaster();return caster==null?super.getTeam():caster.getTeam();}
    @Override protected SoundEvent getAmbientSound() {return SoundEvents.BAT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return SoundEvents.BAT_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.BAT_DEATH;}
    @Override public boolean isAlliedTo(Entity entity) {var caster=getCaster();return super.isAlliedTo(entity) || (caster!=null && (entity==caster || caster.isAlliedTo(entity) || entity.isAlliedTo(caster)));}
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.put("pack",getFocusPackage().serialize());tag.putBoolean("friendly",isFriendly());tag.putInt("Age",tickCount);tag.putInt("AttackTime",attackTime);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);var pack=new FocusPackage();pack.deserialize(tag.getCompound("pack"));setFocusPackage(pack);entityData.set(FRIENDLY,tag.getBoolean("friendly"));tickCount=Math.max(0,tag.getInt("Age"));attackTime=Mth.clamp(tag.getInt("AttackTime"),0,40);}
}
