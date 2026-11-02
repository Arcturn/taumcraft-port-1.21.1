package thaumcraft.port.warp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** The original one-health spider; harmless versions are private 60-second hallucinations. */
public final class MindSpider extends Spider implements EldritchMob {
    private static final EntityDataAccessor<Boolean> HARMLESS=SynchedEntityData.defineId(MindSpider.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER=SynchedEntityData.defineId(MindSpider.class,EntityDataSerializers.STRING);
    public MindSpider(EntityType<? extends MindSpider> type,Level level){super(type,level);xpReward=1;}
    public static AttributeSupplier.Builder attributes(){return Spider.createAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.ATTACK_DAMAGE,1);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(HARMLESS,false);data.define(VIEWER,"");}
    public boolean isHarmless(){return entityData.get(HARMLESS);}
    public void setHarmless(boolean value){entityData.set(HARMLESS,value);}
    public String getViewer(){return entityData.get(VIEWER);}
    public void setViewer(String name){entityData.set(VIEWER,name==null?"":name);}
    public boolean visibleTo(String playerName){return getViewer().isEmpty()||getViewer().equals(playerName);}
    @Override protected int getBaseExperienceReward(){return isHarmless()?0:super.getBaseExperienceReward();}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override protected Entity.MovementEmission getMovementEmission(){return Entity.MovementEmission.NONE;}
    @Override public float getVoicePitch(){return .7f;}
    @Override public boolean doHurtTarget(Entity target){return !isHarmless()&&super.doHurtTarget(target);}
    @Override public void tick(){super.tick();if(!level().isClientSide&&isHarmless()&&tickCount>1200)discard();}
    // BETA26 never rolls spider jockeys or the vanilla hard-difficulty potion bonus.
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){return group;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("harmless",isHarmless());tag.putString("viewer",getViewer());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setHarmless(tag.getBoolean("harmless"));setViewer(tag.getString("viewer"));}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(3,new LeapAtTargetGoal(this,.4f));
        goalSelector.addGoal(4,new MeleeAttackGoal(this,1,true){
            @Override public boolean canUse(){return !mob.isVehicle()&&super.canUse();}
            @Override public boolean canContinueToUse(){if(mob.getLightLevelDependentMagicValue()>=.5f&&mob.getRandom().nextInt(100)==0){mob.setTarget(null);return false;}return super.canContinueToUse();}
        });
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,darkTarget(Player.class));targetSelector.addGoal(3,darkTarget(IronGolem.class));
    }
    private <T extends LivingEntity> NearestAttackableTargetGoal<T> darkTarget(Class<T> type){return new NearestAttackableTargetGoal<T>(this,type,true){
        @Override public boolean canUse(){return getLightLevelDependentMagicValue()<.5f&&super.canUse();}
    };}
}
