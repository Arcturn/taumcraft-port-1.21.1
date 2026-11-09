package thaumcraft.port.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.level.Level;

/** BETA26 anger grows on hits, cools only above one, and is deliberately transient. */
public final class GiantBrainyZombie extends BrainyZombie {
    private static final EntityDataAccessor<Float> ANGER=SynchedEntityData.defineId(GiantBrainyZombie.class,EntityDataSerializers.FLOAT);
    private float bodyScale=1;
    public GiantBrainyZombie(EntityType<? extends GiantBrainyZombie> type,Level level){super(type,level);xpReward=15;}
    public static AttributeSupplier.Builder attributes(){return BrainyZombie.attributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,7);}
    @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(2,new LeapAtTargetGoal(this,.4f));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(ANGER,0f);}
    public float getAnger(){return entityData.get(ANGER);}
    public void setAnger(float anger){entityData.set(ANGER,anger);}
    @Override public boolean hurt(DamageSource source,float damage){setAnger(Math.min(2,getAnger()+.1f));return super.hurt(source,damage);}
    @Override public void aiStep(){
        super.aiStep();
        if(getAnger()>1){setAnger(getAnger()-.002f);bodyScale=1+getAnger();refreshDimensions();}
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7+(getAnger()-1)*5);
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose){
        // The original updates collision size only while anger > 1, unlike its always-scaled model.
        float scale=bodyScale>0?bodyScale:1;
        return super.getDefaultDimensions(pose).scale(scale).withEyeHeight(1.74f*(1+getAnger())-(isBaby()?.81f:0));
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(ANGER.equals(key))refreshDimensions();}
}
