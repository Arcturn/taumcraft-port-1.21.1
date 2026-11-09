package thaumcraft.port.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.world.*;

/** Flying tainted swarm. Its steering and short-range attack follow BETA26. */
public final class TaintSwarm extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Boolean> SUMMONED=SynchedEntityData.defineId(TaintSwarm.class,EntityDataSerializers.BOOLEAN);
    private BlockPos flightTarget;
    private int attackTime;
    public int damBonus;
    public TaintSwarm(EntityType<? extends TaintSwarm> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,2);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(SUMMONED,false);}
    public boolean getIsSummoned(){return entityData.get(SUMMONED);}
    public void setIsSummoned(boolean summoned){entityData.set(SUMMONED,summoned);}
    public void setDamageBonus(int bonus){damBonus=Math.clamp(bonus,0,127);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2+damBonus);}
    @Override public boolean canAttackType(EntityType<?> type){return !TaintedMob.isTaintedType(type)&&super.canAttackType(type);}
    @Override public boolean isAlliedTo(Entity other){return other instanceof TaintedMob||super.isAlliedTo(other);}
    @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof TaintedMob?null:target);}
    @Override public boolean doHurtTarget(Entity target){return !(target instanceof TaintedMob)&&super.doHurtTarget(target);}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override protected Entity.MovementEmission getMovementEmission(){return Entity.MovementEmission.NONE;}
    @Override public float getLightLevelDependentMagicValue(){return 1;}
    @Override protected float getSoundVolume(){return .1f;}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("swarm");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return PortSounds.get("swarmattack");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("swarmattack");}
    @Override public void tick(){super.tick();setDeltaMovement(getDeltaMovement().multiply(1,.6,1));}
    @Override public void aiStep(){super.aiStep();if(!level().isClientSide&&isAlive()&&!isNoAi())updateSwarm();}
    /** Server-side update, exposed so summoned swarms can be checked without random navigation. */
    public void updateSwarm(){
        if(!(level() instanceof ServerLevel level))return;
        if(attackTime>0)attackTime--;
        LivingEntity target=getTarget();
        if(target!=null&&(!target.isAlive()||target instanceof Player player&&(player.isSpectator()||player.getAbilities().invulnerable))){setTarget(null);target=null;}
        if(target==null){
            if(getIsSummoned())hurt(damageSources().generic(),5);
            if(flightTarget!=null&&(!level.hasChunkAt(flightTarget)||!level.isEmptyBlock(flightTarget)||flightTarget.getY()<level.getMinBuildHeight()+1||flightTarget.getY()>level.getHeight(Heightmap.Types.MOTION_BLOCKING,flightTarget.getX(),flightTarget.getZ())+2||!TaintSystem.nearby(level,flightTarget)))flightTarget=null;
            if(flightTarget==null||random.nextInt(30)==0||flightTarget.distSqr(blockPosition())<4){
                var candidate=blockPosition().offset(random.nextInt(7)-random.nextInt(7),random.nextInt(6)-2,random.nextInt(7)-random.nextInt(7));
                flightTarget=level.hasChunkAt(candidate)&&level.getWorldBorder().isWithinBounds(candidate)?candidate:null;
            }
            if(flightTarget!=null)steer(new Vec3(flightTarget.getX()+.5,flightTarget.getY()+.1,flightTarget.getZ()+.5),.015);
            if(!getIsSummoned()){
                Player player=level.getNearestPlayer(this,8);
                if(player!=null&&!player.isSpectator()&&!player.getAbilities().invulnerable)setTarget(player);
            }
        }else{
            steer(target.getEyePosition(),.025);
            if(hasLineOfSight(target))attackEntity(target);
        }
    }
    private void steer(Vec3 destination,double lateral){
        Vec3 difference=destination.subtract(position()),velocity=getDeltaMovement();
        setDeltaMovement(velocity.x+(Math.signum(difference.x)*.5-velocity.x)*lateral,velocity.y+(Math.signum(difference.y)*.7-velocity.y)*.1,velocity.z+(Math.signum(difference.z)*.5-velocity.z)*lateral);
        var movement=getDeltaMovement();float yaw=(float)(Mth.atan2(movement.z,movement.x)*180/Math.PI)-90;
        zza=.1f;setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));setYHeadRot(getYRot());
    }
    public boolean attackEntity(LivingEntity target){
        if(level().isClientSide||target instanceof TaintedMob||!target.isAlive()||attackTime>0||distanceToSqr(target)>=9||target.getBoundingBox().maxY<=getBoundingBox().minY||target.getBoundingBox().minY>=getBoundingBox().maxY)return false;
        if(getIsSummoned())target.lastHurtByPlayerTime=100;
        attackTime=15+random.nextInt(10);Vec3 velocity=target.getDeltaMovement();
        boolean hit=doHurtTarget(target);
        if(hit)target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100),this);
        target.hurtMarked=false;target.setDeltaMovement(velocity);
        playSound(PortSounds.get("swarmattack"),.3f,.9f+random.nextFloat()*.2f);
        return hit;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("summoned",getIsSummoned());tag.putByte("damBonus",(byte)damBonus);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setIsSummoned(tag.getBoolean("summoned"));setDamageBonus(tag.getByte("damBonus"));}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){if(random.nextBoolean())spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);}
}
