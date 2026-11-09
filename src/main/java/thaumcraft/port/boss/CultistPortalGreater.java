package thaumcraft.port.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.infusion.bauble.InfusionBaubles;
import thaumcraft.port.infusion.bauble.ThaumicBannerBlock;
import thaumcraft.port.network.WorldFxPayload;
import thaumcraft.port.world.*;

/** Greater crimson encounter, including its preparation ritual and thirteen-stage escalation. */
public final class CultistPortalGreater extends Monster {
    private final ServerBossEvent bossInfo=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.NOTCHED_6);
    private int stage,stagecounter=200;
    public int pulse;
    public CultistPortalGreater(EntityType<? extends CultistPortalGreater> type,Level level){super(type,level);setNoGravity(true);setPersistenceRequired();xpReward=30;bossInfo.setDarkenScreen(true);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ARMOR,5).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0);}
    public int getStage(){return stage;}public int getStageCounter(){return stagecounter;}
    @Override protected void registerGoals(){}
    @Override public void move(MoverType type,Vec3 delta){}
    @Override public void aiStep(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeAffected(MobEffectInstance effect){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    @Override public void tick(){
        super.tick();
        if(pulse>0)--pulse;
        if(!(level() instanceof ServerLevel server)||!isAlive())return;
        bossInfo.setProgress(getHealth()/getMaxHealth());
        if(stagecounter>0){
            --stagecounter;
            if(stage==0&&stagecounter==160)prepareBanners(server);
            if(stage==0&&stagecounter>20&&stagecounter<150&&stagecounter%13==0)prepareCrate(server);
        }else if(server.getNearestPlayer(this,48)!=null){
            server.broadcastEntityEvent(this,(byte)16);
            if(stage<5){stagecounter=15+random.nextInt(10-stage)-stage;spawnMinion(server);}
            else if(stage==12){stagecounter=50+getTiming()*2+random.nextInt(50);spawnBoss(server);}
            else{int timing=getTiming();stagecounter=timing+random.nextInt(5+timing/3);spawnMinion(server);}
            ++stage;
        }else stagecounter=30+random.nextInt(30);
        if(stage<12)heal(1);
    }
    private void prepareBanners(ServerLevel server){
        if(!server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
        server.broadcastEntityEvent(this,(byte)16);
        for(var direction:Direction.Plane.HORIZONTAL){
            BlockPos pos=blockPosition().offset(-direction.getStepZ()*6,0,direction.getStepX()*6);
            // Never overwrite buildings, liquids or an unloaded neighbouring chunk.
            if(!server.hasChunkAt(pos)||!server.getBlockState(pos).canBeReplaced()||!server.getFluidState(pos).isEmpty())continue;
            int face=switch(direction){case NORTH->8;case SOUTH->0;case WEST->12;default->4;};
            server.setBlock(pos,InfusionBaubles.CRIMSON_BANNER.get().defaultBlockState().setValue(ThaumicBannerBlock.ROTATION,face),3);ritualArc(server,pos);
        }
    }
    private void prepareCrate(ServerLevel server){
        if(!server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
        BlockPos origin=blockPosition(),pos=origin.offset(random.nextInt(5)-random.nextInt(5),0,random.nextInt(5)-random.nextInt(5));
        if(pos.getX()==origin.getX()||pos.getZ()==origin.getZ()||!server.hasChunkAt(pos)||!server.isEmptyBlock(pos))return;
        float chance=random.nextFloat();String rarity=chance<.05f?"rare":chance<.2f?"uncommon":"common";
        server.setBlockAndUpdate(pos,WorldContent.block("loot_crate_"+rarity).defaultBlockState());server.broadcastEntityEvent(this,(byte)16);ritualArc(server,pos);
    }
    private void ritualArc(ServerLevel server,BlockPos pos){WorldFxPayload.send(server,1,Vec3.atCenterOf(pos),position().add(0,getBbHeight()/2,0),0x990000);playSound(PortSounds.get("wandfail"),1,1);}
    public int getTiming(){return level().getEntitiesOfClass(Cultist.class,getBoundingBox().inflate(32),mob->mob.isAlive()&&distanceToSqr(mob)<=32*32).size()*20;}
    private void spawnMinion(ServerLevel server){
        var cultist=(random.nextFloat()>.33?WorldEntities.KNIGHT:WorldEntities.CLERIC).get().create(server);if(cultist==null)return;
        placeSummon(server,cultist);
        if(server.addFreshEntity(cultist)){
            WorldFxPayload.send(server,0,cultist.position().add(0,1,0),cultist.position().add(0,1,0),0xaa0011);cultist.playSound(PortSounds.get("wandfail"),1,1);
            if(stage>12)hurt(damageSources().generic(),5+random.nextInt(5));
        }
    }
    private void spawnBoss(ServerLevel server){
        var cultist=BossContent.CULTIST_LEADER.get().create(server);if(cultist==null)return;
        placeSummon(server,cultist);if(server.addFreshEntity(cultist)){cultist.spawnBurst();cultist.playSound(PortSounds.get("wandfail"),1,1);}
    }
    private void placeSummon(ServerLevel server,PathfinderMob mob){
        mob.moveTo(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat(),0,0);
        mob.finalizeSpawn(server,server.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.MOB_SUMMONED,null);mob.restrictTo(blockPosition(),32);mob.setPersistenceRequired();
    }
    @Override public void playerTouch(Player player){if(!level().isClientSide&&distanceToSqr(player)<3&&player.hurt(damageSources().indirectMagic(this,this),8))playSound(PortSounds.get("zap"),1,(random.nextFloat()-random.nextFloat())*.1f+1);}
    @Override protected float getSoundVolume(){return .75f;}
    @Override public int getAmbientSoundInterval(){return 540;}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return PortSounds.get("monolith");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return PortSounds.get("zap");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return PortSounds.get("shock");}
    @Override protected void dropCustomDeathLoot(ServerLevel server,DamageSource source,boolean recentlyHit){
        super.dropCustomDeathLoot(server,source,recentlyHit);ThaumcraftBoss.dropPearl(this);
    }
    @Override public void die(DamageSource source){if(!level().isClientSide&&!dead)level().explode(this,getX(),getY(),getZ(),2,Level.ExplosionInteraction.NONE);super.die(source);}
    @Override public void handleEntityEvent(byte event){if(event==16)pulse=10;else super.handleEntityEvent(event);}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossInfo.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossInfo.removePlayer(player);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("stage",stage);tag.putInt("stagecounter",stagecounter);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);stage=Math.max(0,tag.getInt("stage"));stagecounter=tag.contains("stagecounter")?Math.max(0,tag.getInt("stagecounter")):200;}
}
