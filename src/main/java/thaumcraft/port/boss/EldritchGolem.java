package thaumcraft.port.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import thaumcraft.port.champion.ChampionModifier;
import thaumcraft.port.champion.ChampionSystem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.warp.entity.EldritchMob;

/** BETA26's two-stage golem: losing its head opens a recharging homing-orb cannon. */
public final class EldritchGolem extends ThaumcraftBoss implements EldritchMob,RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HEADLESS=SynchedEntityData.defineId(EldritchGolem.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BEAM_CHARGE=SynchedEntityData.defineId(EldritchGolem.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHARGING=SynchedEntityData.defineId(EldritchGolem.class,EntityDataSerializers.BOOLEAN);
    private int attackTimer;
    public EldritchGolem(EntityType<? extends EldritchGolem> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return bossAttributes().add(Attributes.MAX_HEALTH,400).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,6);}
    @Override public void generateName(){var modifier=ChampionModifier.byId(ChampionSystem.getModifier(this));if(modifier!=null)setCustomName(Component.translatable("entity.EldritchGolem.name.custom",modifier.displayName()));}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,5,5,24){@Override public boolean canUse(){return isHeadless()&&super.canUse()&&getTarget()!=null&&getTarget().isAlive()&&distanceToSqr(getTarget())>=9;}});
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(HEADLESS,false);data.define(BEAM_CHARGE,0);data.define(CHARGING,false);}
    public boolean isHeadless(){return entityData.get(HEADLESS);}
    public void setHeadless(boolean value){entityData.set(HEADLESS,value);}
    public int getAttackTimer(){return attackTimer;}
    public int getBeamCharge(){return entityData.get(BEAM_CHARGE);}
    public boolean isChargingBeam(){return entityData.get(CHARGING);}
    @Override protected EntityDimensions getDefaultDimensions(Pose pose){return super.getDefaultDimensions(pose).withEyeHeight(isHeadless()?3.33f:3f);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(HEADLESS.equals(key))refreshDimensions();}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){setSpawnTimer(100);return super.finalizeSpawn(level,difficulty,reason,group);}
    @Override public boolean hurt(DamageSource source,float damage){
        if(!level().isClientSide&&!isInvulnerableTo(source)&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&damage>getHealth()&&!isHeadless()){
            setHeadless(true);setSpawnTimer(100);
            double radians=getYRot()*Mth.DEG_TO_RAD;
            level().explode(this,getX()+Math.cos(radians)*.75,getY()+getEyeHeight(),getZ()+Math.sin(radians)*.75,2,false,Level.ExplosionInteraction.NONE);
            return false;
        }
        return super.hurt(source,damage);
    }
    @Override public void tick(){
        if(!level().isClientSide&&getSpawnTimer()>0)heal(2);
        super.tick();
        if(level().isClientSide){BossVisuals.golemTick.accept(this);return;}
        if(isHeadless()){
            if(getBeamCharge()<=0)entityData.set(CHARGING,true);
            if(isChargingBeam()){
                entityData.set(BEAM_CHARGE,Math.min(150,getBeamCharge()+1));level().broadcastEntityEvent(this,(byte)19);
                if(getBeamCharge()>=150)entityData.set(CHARGING,false);
            }
        }
    }
    @Override public void aiStep(){
        super.aiStep();if(attackTimer>0)--attackTimer;
        BlockPos pos=blockPosition();BlockState state=level().getBlockState(pos);
        if(getDeltaMovement().horizontalDistanceSqr()>2.500000277905201E-7&&random.nextInt(5)==0&&level().isClientSide&&!state.isAir()){
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),getX()+(random.nextFloat()-.5)*getBbWidth(),getBoundingBox().minY+.1,getZ()+(random.nextFloat()-.5)*getBbWidth(),4*(random.nextFloat()-.5),.5,4*(random.nextFloat()-.5));
        }
        if(!level().isClientSide&&level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)&&!state.isAir()&&!state.hasBlockEntity()){
            float hardness=state.getDestroySpeed(level(),pos);
            if(hardness>=0&&hardness<=.15f)level().destroyBlock(pos,true,this);
        }
    }
    @Override public boolean doHurtTarget(Entity target){
        if(attackTimer>0||getSpawnTimer()>0)return false;
        attackTimer=10;level().broadcastEntityEvent(this,(byte)4);
        boolean hit=target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*.75f);
        if(hit){target.setDeltaMovement(target.getDeltaMovement().add(0,.2,0));if(isHeadless())target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);}
        return hit;
    }
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor){
        if(level().isClientSide||getSpawnTimer()>0||!isHeadless()||!hasLineOfSight(target)||isChargingBeam()||getBeamCharge()<=0)return;
        entityData.set(BEAM_CHARGE,getBeamCharge()-15-random.nextInt(5));getLookControl().setLookAt(target,30,30);
        var orb=BossContent.GOLEM_ORB.get().create(level());if(orb==null)return;
        orb.setOwner(this);orb.setTarget(target);orb.setRed(false);var look=getLookAngle();orb.setPos(getX()+look.x,getEyeY()-.1,getZ()+look.z);
        orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),.66f,5);
        playSound(PortSounds.get("egattack"),1,1+random.nextFloat()*.1f);level().addFreshEntity(orb);
    }
    @Override public void handleEntityEvent(byte event){if(event==4){attackTimer=10;playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1);}else if(event==19)BossVisuals.charge.accept(this);else super.handleEntityEvent(event);}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.IRON_GOLEM_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.IRON_GOLEM_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.IRON_GOLEM_STEP,1,1);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Headless",isHeadless());tag.putInt("BeamCharge",getBeamCharge());tag.putBoolean("ChargingBeam",isChargingBeam());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setHeadless(tag.getBoolean("Headless"));entityData.set(BEAM_CHARGE,Mth.clamp(tag.getInt("BeamCharge"),-19,150));entityData.set(CHARGING,tag.getBoolean("ChargingBeam"));}
}
