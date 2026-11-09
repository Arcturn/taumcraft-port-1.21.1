package thaumcraft.port.boss;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;
import thaumcraft.port.champion.ChampionSystem;
import thaumcraft.port.mob.Taintacle;
import thaumcraft.port.warp.entity.EldritchMob;

/** Giant taintacle intentionally receives ordinary champion health/damage bonuses, as in TC6. */
public class GiantTaintacle extends Taintacle implements EldritchMob {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(GiantTaintacle.class,EntityDataSerializers.INT);
    protected final ServerBossEvent bossInfo=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    public GiantTaintacle(EntityType<? extends GiantTaintacle> type,Level level){super(type,level);xpReward=20;bossInfo.setDarkenScreen(true);}
    public static AttributeSupplier.Builder attributes(){return Taintacle.attributes().add(Attributes.MAX_HEALTH,175).add(Attributes.ATTACK_DAMAGE,9);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ANGER,0);}
    public int getAnger(){return entityData.get(ANGER);}
    public void setAnger(int value){entityData.set(ANGER,Math.max(0,value));}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){ChampionSystem.makeChampion(this,true);bossInfo.setName(getDisplayName());return data;}
    @Override public void tick(){super.tick();if(getAnger()>0)setAnger(getAnger()-1);ThaumcraftBoss.angryParticles(this,getAnger());if(!level().isClientSide&&isAlive()){if(tickCount%30==0)heal(1);bossInfo.setProgress(getHealth()/getMaxHealth());}}
    @Override public boolean hurt(DamageSource source,float amount){if(!level().isClientSide&&amount>35){if(getAnger()==0){ThaumcraftBoss.enrage(this,source,amount);setAnger(200);}amount=35;}return super.hurt(source,amount);}
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean requiresCustomPersistence(){return true;}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossInfo.setName(getDisplayName());bossInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossInfo.removePlayer(player);}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){
        if(level.getEntitiesOfClass(GiantTaintacle.class,getBoundingBox().inflate(48),other->other!=this&&other.isAlive()&&distanceToSqr(other)<=48*48).isEmpty())ThaumcraftBoss.dropPearl(this);
    }
}
