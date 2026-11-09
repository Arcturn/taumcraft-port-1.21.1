package thaumcraft.port.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.mob.TaintedMob;

/** Rooted taint source; prime seeds differ in health, attack, area, experience and crystal rolls. */
public final class TaintSeed extends Monster implements TaintedMob {
    public int boost;
    public float attackAnim;

    public TaintSeed(EntityType<? extends TaintSeed> type,Level level){super(type,level);xpReward=prime()?12:8;setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(boolean prime){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,prime?150:75).add(Attributes.ATTACK_DAMAGE,prime?7:4).add(Attributes.MOVEMENT_SPEED,0);}
    public boolean prime(){return getType()==WorldEntities.PRIME.get();}
    public int area(){return prime()?2:1;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1,false));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean isAlliedTo(Entity other){return other instanceof TaintedMob||super.isAlliedTo(other);}
    @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof TaintedMob?null:target);}
    @Override public boolean canAttackType(EntityType<?> type){return !TaintedMob.isTaintedType(type)&&super.canAttackType(type);}
    @Override public boolean doHurtTarget(Entity target){
        if(target instanceof TaintedMob)return false;
        level().broadcastEntityEvent(this,(byte)16);playSound(PortSounds.get("tentacle"),getSoundVolume(),getVoicePitch());return super.doHurtTarget(target);
    }
    @Override public void handleEntityEvent(byte event){if(event==16)attackAnim=.5f;else super.handleEntityEvent(event);}
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){
        double radius=TaintSystem.radius()*.8;Vec3 center=Vec3.atCenterOf(blockPosition());
        return level.getDifficulty()!=Difficulty.PEACEFUL&&checkSpawnObstruction(level())
                &&level().getEntitiesOfClass(TaintSeed.class,new AABB(center,center).inflate(radius),other->other!=this&&other.isAlive()).isEmpty();
    }
    @Override public boolean checkSpawnObstruction(LevelReader level){return !level.containsAnyLiquid(getBoundingBox())&&level.isUnobstructed(this);}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return true;}
    @Override public void moveRelative(float speed,Vec3 direction){}
    @Override public void move(MoverType type,Vec3 movement){super.move(type,new Vec3(0,Math.min(0,movement.y),0));}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){attackAnim*=.75f;if(attackAnim<.001f)attackAnim=0;return;}
        if(!(level() instanceof ServerLevel server)||!isAlive())return;
        int area=area();boolean second=tickCount%20==0;
        if(WorldConfig.TAINT.get()&&(boost>0||second)){
            var aura=AuraSystem.get(server,blockPosition());float saturation=boost>0?1:aura==null||aura.base()==0?0:aura.flux()/aura.base();
            if(boost>0)--boost;
            if(saturation<=0){hurt(damageSources().starve(),.5f);AuraSystem.pollute(server,blockPosition(),.1f,false);}
            else TaintSystem.spread(server,blockPosition().offset(random.nextInt(area*6+1)-area*3,random.nextInt(area*2+1)-area,random.nextInt(area*6+1)-area*3),true);
        }
        if(second){
            var target=getTarget();
            if(target!=null&&target.isAlive()&&distanceToSqr(target)<area*256&&hasLineOfSight(target))spawnTentacles(target);
            // EntityUtils.getEntitiesInRange used a cube around the block centre, not the mob's body.
            Vec3 center=Vec3.atCenterOf(blockPosition());
            for(var entity:server.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(area*4),entity->entity!=this))
                entity.addEffect(new MobEffectInstance(WorldEntities.TAINT,100,area-1,false,true));
        }
    }
    public thaumcraft.port.mob.TaintacleSmall spawnTentacles(Entity target){return thaumcraft.port.mob.Taintacle.summonTentacle(this,target);}
    @Override public int getAmbientSoundInterval(){return 200;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.CHORUS_FLOWER_DEATH;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return PortSounds.get("tentacle");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("tentacle");}
    @Override public float getVoicePitch(){return 1.3f-getBbHeight()/10;}
    @Override protected float getSoundVolume(){return getBbHeight()/8;}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){
        spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);
        if(prime()){if(random.nextBoolean())spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);if(random.nextBoolean())spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("boost",boost);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);boost=Math.max(0,Math.min(10000,tag.getInt("boost")));}
}
