package thaumcraft.port.auromancy.firebat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** The wild BETA26 fire bat, distinct from the temporary focus-created spell bat. */
public final class FireBat extends Monster {
    private static final EntityDataAccessor<Boolean> HANGING=SynchedEntityData.defineId(FireBat.class,EntityDataSerializers.BOOLEAN);
    private BlockPos flightTarget;
    private int attackTime;
    public int damageBonus;
    public FireBat(EntityType<? extends FireBat> type,Level level){super(type,level);setHanging(true);xpReward=5;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,1);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(HANGING,false);}
    @Override public net.minecraft.network.chat.Component getName(){return hasCustomName()?getCustomName():net.minecraft.network.chat.Component.translatable("entity.Firebat.name");}
    public boolean hanging(){return entityData.get(HANGING);}
    public void setHanging(boolean value){entityData.set(HANGING,value);}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override protected void checkFallDamage(double y,boolean onGround,BlockState state,BlockPos pos){}
    @Override protected float getSoundVolume(){return .1f;}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.95f;}
    @Override protected SoundEvent getAmbientSound(){return hanging()&&random.nextInt(4)!=0?null:SoundEvents.BAT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.BAT_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.BAT_DEATH;}
    @Override public void aiStep(){if(isInWaterRainOrBubble())hurt(damageSources().drown(),1);super.aiStep();}
    @Override public void tick(){
        super.tick();
        if(hanging()){setDeltaMovement(0,0,0);setPos(getX(),Mth.floor(getY())+1-getBbHeight(),getZ());}
        else setDeltaMovement(getDeltaMovement().multiply(1,.6f,1));
    }
    @Override protected void customServerAiStep(){
        super.customServerAiStep();if(attackTime>0)attackTime--;
        var pos=blockPosition();var ceiling=pos.above();var target=getTarget();
        if(hanging()){
            if(!level().getBlockState(ceiling).isRedstoneConductor(level(),ceiling))wake(pos);
            else {
                if(random.nextInt(200)==0)yHeadRot=random.nextInt(360);
                if(level().getNearestPlayer(this,4)!=null)wake(pos);
            }
        }else{
            double dx,dy,dz;
            if(target==null){
                if(flightTarget!=null&&(!level().isEmptyBlock(flightTarget)||flightTarget.getY()<level().getMinBuildHeight()+1))flightTarget=null;
                if(flightTarget==null||random.nextInt(30)==0||flightTarget.distToCenterSqr(position())<4)
                    flightTarget=new BlockPos((int)getX()+random.nextInt(7)-random.nextInt(7),(int)getY()+random.nextInt(6)-2,(int)getZ()+random.nextInt(7)-random.nextInt(7));
                dx=flightTarget.getX()+.5-getX();dy=flightTarget.getY()+.1-getY();dz=flightTarget.getZ()+.5-getZ();
            }else{dx=target.getX()-getX();dy=target.getY()+target.getEyeHeight()*.66f-getY();dz=target.getZ()-getZ();}
            var motion=getDeltaMovement();
            setDeltaMovement(motion.add((Math.signum(dx)*.5-motion.x)*.1f,(Math.signum(dy)*.7f-motion.y)*.1f,(Math.signum(dz)*.5-motion.z)*.1f));
            motion=getDeltaMovement();float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)-90;
            zza=.5f;setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));
            if(target==null&&random.nextInt(100)==0&&level().getBlockState(ceiling).isRedstoneConductor(level(),ceiling))setHanging(true);
        }
        if(target==null)setTarget(level().getNearestPlayer(this,12));
        else if(!target.isAlive())setTarget(null);
        else if(isAlive()&&hasLineOfSight(target))attack(target);
        if(getTarget() instanceof Player player&&player.isCreative())setTarget(null);
    }
    private void wake(BlockPos pos){setHanging(false);level().levelEvent(null,1025,pos,0);}
    private void attack(LivingEntity target){
        if(attackTime>0||distanceTo(target)>=Math.max(2.5f,target.getBbWidth()*1.1f)
                ||target.getBoundingBox().maxY<=getBoundingBox().minY||target.getBoundingBox().minY>=getBoundingBox().maxY)return;
        attackTime=20+random.nextInt(20);
        if(random.nextInt(10)==0){
            target.invulnerableTime=0;
            level().explode(this,getX(),getY(),getZ(),1.5f,false,Level.ExplosionInteraction.NONE);
            discard();
        }
        playSound(SoundEvents.BAT_HURT,.5f,.9f+random.nextFloat()*.2f);doHurtTarget(target);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source)||source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION))return false;
        if(!level().isClientSide&&hanging())setHanging(false);
        return super.hurt(source,amount);
    }
    public static boolean canSpawn(EntityType<FireBat> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        // EntityFireBat replaces EntityMob's darkness predicate with true after this extra 0..6 roll.
        return level.getDifficulty()!=Difficulty.PEACEFUL&&level.getMaxLocalRawBrightness(pos)<=random.nextInt(7)
                &&Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("hang",hanging());tag.putByte("damBonus",(byte)damageBonus);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setHanging(tag.getBoolean("hang"));damageBonus=tag.getByte("damBonus");}
}
