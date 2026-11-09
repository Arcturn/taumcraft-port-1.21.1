package thaumcraft.port.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import org.joml.Vector3f;
import thaumcraft.port.auromancy.effect.FocusEffectContent;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.champion.ChampionModifier;
import thaumcraft.port.champion.ChampionSystem;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.warp.entity.EldritchMob;
import thaumcraft.port.warp.entity.WarpEntities;
import thaumcraft.port.world.Cultist;

/** A shielded eldritch boss whose first broken ward triggers a radial sap-field frenzy. */
public final class EldritchWarden extends ThaumcraftBoss implements RangedAttackMob,EldritchMob {
    private static final String[] TITLES={"Aphoom-Zhah","Basatan","Chaugnar Faugn","Mnomquah","Nyogtha","Oorn","Shaikorth","Rhan-Tegoth","Rhogog","Shudde M'ell","Vulthoom","Yag-Kosha","Yibb-Tstll","Zathog","Zushakon"};
    private static final EntityDataAccessor<Integer> TITLE=SynchedEntityData.defineId(EldritchWarden.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FRENZY_TICKS=SynchedEntityData.defineId(EldritchWarden.class,EntityDataSerializers.INT);
    private final ServerBossEvent shieldInfo=new ServerBossEvent(Component.empty(),BossEvent.BossBarColor.BLUE,BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean fieldFrenzy,lastBlast;
    public float armLiftL,armLiftR;
    public EldritchWarden(EntityType<? extends EldritchWarden> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return bossAttributes().add(Attributes.MAX_HEALTH,400).add(Attributes.MOVEMENT_SPEED,.33).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,4).add(Attributes.MAX_ABSORPTION,264);}
    @Override public void generateName(){var modifier=ChampionModifier.byId(ChampionSystem.getModifier(this));if(modifier!=null)setCustomName(Component.translatable("entity.EldritchWarden.name.custom",getTitle(),modifier.displayName()));}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24){@Override public boolean canUse(){return super.canUse()&&getTarget()!=null&&getTarget().isAlive()&&distanceToSqr(getTarget())>=9;}});
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,1));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Cultist.class,true));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(TITLE,0);data.define(FRENZY_TICKS,0);}
    public String getTitle(){return TITLES[getTitleIndex()];}
    public int getTitleIndex(){return entityData.get(TITLE);}
    public void setTitle(int title){entityData.set(TITLE,Mth.clamp(title,0,TITLES.length-1));}
    public boolean isFieldFrenzy(){return fieldFrenzy;}
    public int getFieldFrenzyCounter(){return entityData.get(FRENZY_TICKS);}
    public float getShieldCapacity(){return (int)(getAttribute(Attributes.MAX_HEALTH).getBaseValue()*.66);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){
        setSpawnTimer(150);setTitle(random.nextInt(TITLES.length));
        getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(getShieldCapacity());setAbsorptionAmount(getAbsorptionAmount()+getShieldCapacity());
        return super.finalizeSpawn(level,difficulty,reason,group);
    }
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);shieldInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);shieldInfo.removePlayer(player);}
    @Override public boolean isEffectiveAi(){return getFieldFrenzyCounter()==0&&super.isEffectiveAi();}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){armLiftL=Math.max(0,armLiftL-.05f);armLiftR=Math.max(0,armLiftR-.05f);BossVisuals.wisp.accept(this);}
        else{
            if(invulnerableTime<=0&&tickCount%25==0&&getAbsorptionAmount()<getShieldCapacity())setAbsorptionAmount(Math.min(getShieldCapacity(),getAbsorptionAmount()+1));
            shieldInfo.setProgress(Mth.clamp(getAbsorptionAmount()/Math.max(1,getShieldCapacity()),0,1));
        }
    }
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide)return;
        for(int l=0;l<4;l++){
            var pos=BlockPos.containing(getX()+(l%2*2-1)*.25,getY(),getZ()+(l/2%2*2-1)*.25);
            if(level().getBlockState(pos).isAir())level().setBlockAndUpdate(pos,FocusEffectContent.SAP.get().defaultBlockState());
        }
        if(getFieldFrenzyCounter()>0){
            if(getFieldFrenzyCounter()==150)teleportHome();
            getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);performFieldFrenzy();
        }
    }
    private void performFieldFrenzy(){
        int counter=getFieldFrenzyCounter();
        if(counter<121&&counter%10==0){
            level().broadcastEntityEvent(this,(byte)17);double radius=(150-counter)/8d;int d=1+counter/8;
            BlockPos center=blockPosition();
            for(int q=0;q<180/d;q++){
                double radians=Math.toRadians(q*2*d);var pos=center.offset((int)(radius*Math.cos(radians)),0,(int)(radius*Math.sin(radians)));
                if(!level().hasChunkAt(pos)||!level().getBlockState(pos).isAir()||!level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),Direction.UP))continue;
                level().setBlockAndUpdate(pos,FocusEffectContent.SAP.get().defaultBlockState());level().scheduleTick(pos,FocusEffectContent.SAP.get(),250+random.nextInt(150));
                if(random.nextFloat()<.3f&&level() instanceof ServerLevel server){
                    Vec3 start=Vec3.atCenterOf(pos),end=position().add(0,getBbHeight()*.5,0);var color=new DustParticleOptions(new Vector3f(.5f+random.nextFloat()*.2f,0,.5f+random.nextFloat()*.2f),1);
                    for(int point=0;point<12;point++){var at=start.lerp(end,point/12d);server.sendParticles(color,at.x,at.y,at.z,1,.05,.05,.05,0);}
                }
            }
            playSound(PortSounds.get("zap"),1,.9f+random.nextFloat()*.1f);
        }
        entityData.set(FRENZY_TICKS,counter-1);
    }
    public boolean teleportHome(){
        if(!(level() instanceof ServerLevel server)||!hasRestriction())return false;
        BlockPos home=getRestrictCenter();var event=NeoForge.EVENT_BUS.post(new EntityTeleportEvent.EnderEntity(this,home.getX(),home.getY(),home.getZ()));
        if(event.isCanceled())return false;
        Vec3 before=position();BlockPos destination=BlockPos.containing(event.getTargetX(),event.getTargetY(),event.getTargetZ());boolean success=false;
        for(int attempt=0;attempt<20;attempt++){
            BlockPos candidate=attempt==0?destination:destination.offset(random.nextInt(8)-random.nextInt(8),0,random.nextInt(8)-random.nextInt(8));
            if(!server.hasChunkAt(candidate)||!server.getBlockState(candidate.below()).isFaceSturdy(server,candidate.below(),Direction.UP))continue;
            setPos(candidate.getX()+.5,candidate.getY()+.1,candidate.getZ()+.5);
            if(server.noCollision(this)&&!server.containsAnyLiquid(getBoundingBox())){success=true;break;}
            setPos(before);
        }
        if(!success){setPos(before);return false;}
        getNavigation().stop();setDeltaMovement(Vec3.ZERO);
        for(int i=0;i<128;i++){
            Vec3 point=before.lerp(position(),i/127d);server.sendParticles(ParticleTypes.PORTAL,point.x,point.y+getBbHeight()*.5,point.z,1,getBbWidth()*.5,getBbHeight()*.5,getBbWidth()*.5,.1);
        }
        playSound(SoundEvents.ENDERMAN_TELEPORT,1,1);return true;
    }
    @Override public boolean isInvulnerableTo(DamageSource source){return getFieldFrenzyCounter()>0||super.isInvulnerableTo(source);}
    @Override public boolean hurt(DamageSource source,float damage){
        if(isInvulnerableTo(source)||source.is(DamageTypes.DROWN)||source.is(DamageTypes.WITHER))return false;
        boolean result=super.hurt(source,damage);
        if(!level().isClientSide&&result&&!fieldFrenzy&&getAbsorptionAmount()<=0){fieldFrenzy=true;entityData.set(FRENZY_TICKS,150);getNavigation().stop();}
        return result;
    }
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor){
        if(level().isClientSide||getSpawnTimer()>0||getFieldFrenzyCounter()>0||!target.isAlive())return;
        if(random.nextFloat()>.2f){
            var orb=WarpEntities.ELDRITCH_ORB.get().create(level());if(orb==null)return;
            orb.setOwner(this);lastBlast=!lastBlast;level().broadcastEntityEvent(this,lastBlast?(byte)16:(byte)15);
            double angle=(getYRot()+(lastBlast?90:180))*Mth.DEG_TO_RAD;
            orb.setPos(getX()-Math.cos(angle)*.5,getEyeY()-.1-.13,getZ()-Math.sin(angle)*.5);
            orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),1,2);
            playSound(PortSounds.get("egattack"),2,1+random.nextFloat()*.1f);level().addFreshEntity(orb);
        }else performSonicAttack(target);
    }
    public void performSonicAttack(LivingEntity target){
        if(level().isClientSide||getSpawnTimer()>0||getFieldFrenzyCounter()>0||!target.isAlive()||!hasLineOfSight(target))return;
        level().broadcastEntityEvent(this,(byte)19);target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER,400,0),this);target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,400,0),this);
        if(target instanceof ServerPlayer player){
            var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
            state.warp(state.permanentWarp(),state.normalWarp(),state.temporaryWarp()+3+random.nextInt(3));storage.setDirty();ResearchProgress.sync(player);
        }
        playSound(PortSounds.get("egscreech"),4,1+random.nextFloat()*.1f);
    }
    @Override public void handleEntityEvent(byte event){
        if(event==15)armLiftL=.5f;else if(event==16)armLiftR=.5f;else if(event==17){armLiftL=.9f;armLiftR=.9f;}else if(event==19)BossVisuals.sonic.accept(this);else super.handleEntityEvent(event);
    }
    @Override public boolean isAlliedTo(Entity other){return other instanceof EldritchMob||super.isAlliedTo(other);}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("egidle");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("egdeath");}
    @Override public int getAmbientSoundInterval(){return 500;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Title",getTitleIndex());tag.putBoolean("FieldFrenzy",fieldFrenzy);tag.putInt("FieldFrenzyTicks",getFieldFrenzyCounter());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setTitle(tag.getInt("Title"));fieldFrenzy=tag.getBoolean("FieldFrenzy");entityData.set(FRENZY_TICKS,Mth.clamp(tag.getInt("FieldFrenzyTicks"),0,150));}
}
