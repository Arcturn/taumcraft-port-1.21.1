package thaumcraft.port.warp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.warp.WarpPayload;
import thaumcraft.port.world.Cultist;

/** BETA26's guardian: fading apparition, alternating orb attacks and an occasional withering scream. */
public final class EldritchGuardian extends Monster implements RangedAttackMob,EldritchMob {
    public float armLiftL,armLiftR;
    private boolean lastBlast;
    public EldritchGuardian(EntityType<? extends EldritchGuardian> type,Level level){super(type,level);((GroundPathNavigation)getNavigation()).setCanFloat(true);xpReward=20;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.FOLLOW_RANGE,40)
            .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ARMOR,4);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24){
            @Override public boolean canUse(){if(!super.canUse())return false;var target=getTarget();return target!=null&&target.isAlive()&&distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())>=64;}
        });
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Cultist.class,true));
    }
    @Override public boolean canPickUpLoot(){return false;}
    @Override public boolean hurt(DamageSource source,float damage){return super.hurt(source,source.is(DamageTypeTags.WITCH_RESISTANT_TO)?damage/2:damage);}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){
            if(armLiftL>0)armLiftL-=.05f;if(armLiftR>0)armLiftR-=.05f;
            WarpEntityVisuals.guardianWisp.accept(this);
        }else if(tickCount%100==0&&level().getDifficulty()!=Difficulty.EASY){
            double range=level().getDifficulty()==Difficulty.HARD?576:256;
            for(var player:level().players())if(player instanceof ServerPlayer server&&player.isAlive()&&distanceToSqr(player)<range)WarpPayload.send(server,2);
        }
    }
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("egidle");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("egdeath");}
    @Override public int getAmbientSoundInterval(){return 500;}
    @Override protected float getSoundVolume(){return 1.5f;}
    @Override public boolean isAlliedTo(Entity other){return other instanceof EldritchMob;}
    @Override public boolean removeWhenFarAway(double distance){return !hasRestriction();}
    @Override public boolean checkSpawnRules(LevelAccessor world,MobSpawnType reason){
        return level().getEntitiesOfClass(EldritchGuardian.class,new AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(32,16,32),e->e!=this).isEmpty()&&super.checkSpawnRules(world,reason);
    }
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor){
        if(level().isClientSide||!target.isAlive())return;
        if(random.nextFloat()>.15f){
            var orb=WarpEntities.ELDRITCH_ORB.get().create(level());if(orb==null)return;
            orb.setOwner(this);orb.setPos(getX(),getEyeY()-.1,getZ());lastBlast=!lastBlast;
            level().broadcastEntityEvent(this,lastBlast?(byte)16:(byte)15);
            float angle=(getYRot()+(lastBlast?90:180))%360/180*(float)Math.PI;
            orb.setPos(orb.getX()-Math.cos(angle)*.5,orb.getY(),orb.getZ()-Math.sin(angle)*.5);
            var aim=target.position().add(target.getDeltaMovement().scale(10)).subtract(position()).normalize();
            orb.shoot(aim.x,aim.y,aim.z,1.1f,2);playSound(PortSounds.get("egattack"),2,1+random.nextFloat()*.1f);level().addFreshEntity(orb);
        }else if(hasLineOfSight(target)){
            // Separate status from the inherited two-arm pose: the source sonic packet did not lift the arms.
            level().broadcastEntityEvent(this,(byte)18);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,400,0),this);
            if(target instanceof ServerPlayer player){
                var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
                state.warp(state.permanentWarp(),state.normalWarp(),state.temporaryWarp()+1+level().random.nextInt(3));storage.setDirty();ResearchProgress.sync(player);
            }
            playSound(PortSounds.get("egscreech"),3,1+random.nextFloat()*.1f);
        }
    }
    @Override public void handleEntityEvent(byte event){
        if(event==15)armLiftL=.5f;else if(event==16)armLiftR=.5f;else if(event==17){armLiftL=.9f;armLiftR=.9f;}
        else if(event==18)WarpEntityVisuals.sonic.accept(this);else super.handleEntityEvent(event);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);if(hasRestriction()){var pos=getRestrictCenter();tag.putInt("HomeD",(int)getRestrictRadius());tag.putInt("HomeX",pos.getX());tag.putInt("HomeY",pos.getY());tag.putInt("HomeZ",pos.getZ());}
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),Math.max(0,tag.getInt("HomeD")));}
}
