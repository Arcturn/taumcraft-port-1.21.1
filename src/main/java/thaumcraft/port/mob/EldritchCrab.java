package thaumcraft.port.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.warp.entity.EldritchMob;
import thaumcraft.port.world.Cultist;

/** The original breakable shell and airborne, biting passenger attack. */
public final class EldritchCrab extends Monster implements EldritchMob {
    private static final EntityDataAccessor<Boolean> HELM=SynchedEntityData.defineId(EldritchCrab.class,EntityDataSerializers.BOOLEAN);
    private static final byte BREAK_HELM=60;
    private int attackTime;
    public EldritchCrab(EntityType<? extends EldritchCrab> type,Level level){super(type,level);xpReward=6;((GroundPathNavigation)getNavigation()).setCanFloat(true);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63f));goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Cultist.class,true));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(HELM,false);}
    public boolean hasHelm(){return entityData.get(HELM);}
    public void setHelm(boolean value){entityData.set(HELM,value);}
    public int getAttackTime(){return attackTime;}
    public int getRiding(){return getVehicle()==null?-1:getVehicle().getId();}
    @Override public boolean canPickUpLoot(){return false;}
    @Override protected ResourceKey<LootTable> getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
    @Override public int getArmorValue(){return hasHelm()?5:0;}
    @Override public boolean isAlliedTo(Entity other){return other instanceof EldritchCrab;}
    @Override public boolean canBeAffected(MobEffectInstance effect){return !effect.is(MobEffects.POISON)&&super.canBeAffected(effect);}
    @Override public Vec3 getVehicleAttachmentPoint(Entity vehicle){return new Vec3(0,-.5,0);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){
        setHelm(level.getDifficulty()==Difficulty.HARD||random.nextFloat()<.33f);
        if(group==null){
            Holder<MobEffect> effect=null;
            if(level.getDifficulty()==Difficulty.HARD&&level.getRandom().nextFloat()<.1f*difficulty.getSpecialMultiplier())effect=switch(level.getRandom().nextInt(5)){case 0,1->MobEffects.MOVEMENT_SPEED;case 2->MobEffects.DAMAGE_BOOST;case 3->MobEffects.REGENERATION;default->MobEffects.INVISIBILITY;};
            group=new CrabGroup(effect);
        }
        if(group instanceof CrabGroup crabGroup&&crabGroup.effect()!=null)addEffect(new MobEffectInstance(crabGroup.effect(),Integer.MAX_VALUE));
        return super.finalizeSpawn(level,difficulty,reason,group);
    }
    @Override public void tick(){
        super.tick();--attackTime;if(tickCount<20)fallDistance=0;
        if(level().isClientSide)return;
        var target=getTarget();
        if(!isPassenger()&&target!=null&&!target.isVehicle()&&!onGround()&&!hasHelm()&&target.isAlive()&&getY()-target.getY()>=target.getBbHeight()/2&&distanceToSqr(target)<4)startRiding(target,true);
        if(isPassenger()&&!isRemoved()&&attackTime<=0){attackTime=10+random.nextInt(10);doHurtTarget(getVehicle());if(random.nextFloat()<.2f)stopRiding();}
    }
    @Override public boolean doHurtTarget(Entity target){if(!super.doHurtTarget(target))return false;playSound(PortSounds.get("crabclaw"),1,.9f+level().random.nextFloat()*.2f);return true;}
    @Override public boolean hurt(DamageSource source,float damage){
        boolean result=super.hurt(source,damage);
        if(hasHelm()&&getHealth()/getMaxHealth()<=.5f){setHelm(false);level().broadcastEntityEvent(this,BREAK_HELM);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3);}
        return result;
    }
    @Override public void handleEntityEvent(byte event){
        if(event!=BREAK_HELM){super.handleEntityEvent(event);return;}
        playSound(SoundEvents.ITEM_BREAK,.8f,.8f+random.nextFloat()*.4f);
        for(int i=0;i<5;i++)level().addParticle(new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(MobArmor.CHEST.get())),getX(),getEyeY(),getZ(),random.nextGaussian()*.02,.05+random.nextDouble()*.05,random.nextGaussian()*.02);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){
        super.dropCustomDeathLoot(level,source,recentlyHit);
        if(recentlyHit&&(random.nextInt(3)==0||random.nextInt(1+BrainyZombie.looting(source))>0))spawnAtLocation(Items.ENDER_PEARL);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("helm",hasHelm());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setHelm(tag.getBoolean("helm"));if(!hasHelm())getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3);}
    @Override public int getAmbientSoundInterval(){return 160;}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("crabtalk");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GENERIC_HURT;}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("crabdeath");}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.SPIDER_STEP,.15f,1);}
    private record CrabGroup(Holder<MobEffect> effect) implements SpawnGroupData {}
}
