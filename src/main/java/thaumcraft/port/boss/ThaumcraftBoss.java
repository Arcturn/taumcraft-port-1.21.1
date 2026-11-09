package thaumcraft.port.boss;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.champion.ChampionBoss;
import thaumcraft.port.warp.entity.EldritchMob;

/** Shared TC6 boss rules: capped hits, enrage, regeneration, threat and encounter scaling. */
public class ThaumcraftBoss extends Monster implements ChampionBoss {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(ThaumcraftBoss.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPAWN_TIMER=SynchedEntityData.defineId(ThaumcraftBoss.class,EntityDataSerializers.INT);
    protected final ServerBossEvent bossInfo=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private final Map<Integer,Integer> threat=new HashMap<>();

    public ThaumcraftBoss(EntityType<? extends ThaumcraftBoss> type,Level level){super(type,level);xpReward=50;bossInfo.setDarkenScreen(true);}
    public static AttributeSupplier.Builder bossAttributes(){return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE,.95).add(Attributes.FOLLOW_RANGE,40);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ANGER,0);builder.define(SPAWN_TIMER,0);}
    public int getAnger(){return entityData.get(ANGER);}
    public void setAnger(int value){entityData.set(ANGER,Math.max(0,value));}
    public int getSpawnTimer(){return entityData.get(SPAWN_TIMER);}
    protected void setSpawnTimer(int value){entityData.set(SPAWN_TIMER,Math.max(0,value));}
    @Override public boolean isEffectiveAi(){return getSpawnTimer()==0&&super.isEffectiveAi();}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();
        if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);
        bossInfo.setProgress(getHealth()/getMaxHealth());
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){
        restrictTo(blockPosition(),24);generateName();bossInfo.setName(getDisplayName());return data;
    }
    @Override public void generateName(){}
    @Override public void setCustomName(Component name){super.setCustomName(name);if(bossInfo!=null)bossInfo.setName(getDisplayName());}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossInfo.removePlayer(player);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean requiresCustomPersistence(){return true;}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public void makeStuckInBlock(BlockState state,Vec3 multiplier){}
    @Override public boolean isAlliedTo(Entity other){return other instanceof EldritchMob||super.isAlliedTo(other);}
    @Override public boolean isInvulnerableTo(DamageSource source){return getSpawnTimer()>0||super.isInvulnerableTo(source);}
    @Override public boolean isPushable(){return getSpawnTimer()==0&&super.isPushable();}
    @Override public void tick(){
        super.tick();
        if(getSpawnTimer()>0)setSpawnTimer(getSpawnTimer()-1);
        if(getAnger()>0)setAnger(getAnger()-1);
        angryParticles(this,getAnger());
        if(!level().isClientSide&&isAlive()){
            if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);
            if(tickCount%30==0)heal(1);
            if(getTarget()!=null&&tickCount%20==0)updateThreat();
            bossInfo.setProgress(getHealth()/getMaxHealth());
        }
    }
    /** Fixes the source's wrong-index threat lookup while retaining its thresholds and buffs. */
    private void updateThreat(){
        threat.entrySet().removeIf(e->{Entity entity=level().getEntity(e.getKey());return !(entity instanceof LivingEntity living)||!living.isAlive()||distanceToSqr(living)>16384;});
        var current=getTarget();int currentThreat=threat.getOrDefault(current.getId(),0),best=currentThreat,players=0;
        LivingEntity next=current;
        for(var entry:threat.entrySet()){
            var entity=(LivingEntity)level().getEntity(entry.getKey());
            if(entity instanceof Player player&&!player.isSpectator()&&!player.isCreative())players++;
            int score=entry.getValue();
            if(score>currentThreat+25&&score>currentThreat*1.1&&score>best&&canAttack(entity)){next=entity;best=score;}
        }
        if(next!=current)setTarget(next);
        float oldMax=getMaxHealth(),health=getHealth();
        var hp=getAttribute(Attributes.MAX_HEALTH);var attack=getAttribute(Attributes.ATTACK_DAMAGE);
        int stacks=Math.min(5,Math.max(0,players-1));
        for(int i=0;i<5;i++){
            var id=ResourceLocation.fromNamespaceAndPath("thaumcraft","boss_party_"+i);
            hp.removeModifier(id);attack.removeModifier(id);
            if(i<stacks){hp.addTransientModifier(new AttributeModifier(id,50,AttributeModifier.Operation.ADD_VALUE));attack.addTransientModifier(new AttributeModifier(id,.5,AttributeModifier.Operation.ADD_VALUE));}
        }
        setHealth(health*getMaxHealth()/oldMax);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(!level().isClientSide){
            if(source.getEntity() instanceof LivingEntity attacker)threat.merge(attacker.getId(),(int)amount,Integer::sum);
            if(amount>35){if(getAnger()==0){enrage(this,source,amount);setAnger(200);}amount=35;}
        }
        return super.hurt(source,amount);
    }
    static void enrage(LivingEntity boss,DamageSource source,float amount){
        boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,(int)(amount/15)));
        boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(amount/10)));
        boss.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,200,(int)(amount/40)));
        if(source.getEntity() instanceof Player player)player.displayClientMessage(Component.translatable("tc.boss.enrage",boss.getDisplayName()),true);
    }
    static void angryParticles(LivingEntity boss,int anger){
        if(boss.level().isClientSide&&anger>0&&boss.getRandom().nextInt(15)==0)
            boss.level().addParticle(ParticleTypes.ANGRY_VILLAGER,boss.getRandomX(1),boss.getY()+boss.getBbHeight()+boss.getRandom().nextFloat()*.5,boss.getRandomZ(1),0,0,0);
    }
    public static ItemStack primordialPearl(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primordial_pearl")));}
    /** Boss reward uses TC6's floating, blast-resistant special item entity. */
    public static void dropPearl(LivingEntity boss){
        var drop=new thaumcraft.port.machine.SpecialItemEntity(boss.level(),boss.getX(),boss.getY()+boss.getBbHeight()/2,boss.getZ(),primordialPearl());
        drop.setDefaultPickUpDelay();drop.setDeltaMovement(0,.1,0);
        if(boss.captureDrops()!=null)boss.captureDrops().add(drop);else boss.level().addFreshEntity(drop);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){dropPearl(this);spawnAtLocation(BossLoot.bag(2),1.5f);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);
        tag.putInt("SpawnTimer",getSpawnTimer());
        if(hasRestriction()){tag.putInt("HomeD",(int)getRestrictRadius());tag.putInt("HomeX",getRestrictCenter().getX());tag.putInt("HomeY",getRestrictCenter().getY());tag.putInt("HomeZ",getRestrictCenter().getZ());}
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);
        setSpawnTimer(tag.getInt("SpawnTimer"));
        if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),tag.getInt("HomeD"));
        bossInfo.setName(getDisplayName());
    }
}
