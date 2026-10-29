package thaumcraft.port.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lesser portal: activation, population cap and finite spawning health from TC6. */
public final class CultistPortal extends Monster {
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(CultistPortal.class,EntityDataSerializers.BOOLEAN);
    private int delay=100;
    public CultistPortal(EntityType<? extends CultistPortal> type,Level l){super(type,l);setNoGravity(true);setPersistenceRequired();xpReward=10;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,100).add(Attributes.ARMOR,4).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.ATTACK_DAMAGE,0);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTIVE,false);}
    @Override protected void registerGoals(){}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return thaumcraft.port.content.PortSounds.get("monolith");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return thaumcraft.port.content.PortSounds.get("zap");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return thaumcraft.port.content.PortSounds.get("shock");}
    public boolean active(){return entityData.get(ACTIVE);}
    @Override public void move(MoverType type,Vec3 delta){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){return false;}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        var player=l.players().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&distanceToSqr(p)<=32*32).min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(!active()){if(tickCount%10==0&&player!=null){entityData.set(ACTIVE,true);playSound(thaumcraft.port.content.PortSounds.get("craftstart"),1,1);}return;}
        if(--delay>0)return;delay=50+random.nextInt(50);
        if(player==null||!hasLineOfSight(player))return;
        int cap=switch(l.getDifficulty()){case HARD->6;case NORMAL->4;default->2;};
        if(l.getEntitiesOfClass(Cultist.class,getBoundingBox().inflate(32),LivingEntity::isAlive).size()>=cap)return;
        var mob=(random.nextFloat()>.33?WorldEntities.KNIGHT:WorldEntities.CLERIC).get().create(l);if(mob==null)return;
        mob.moveTo(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat(),random.nextFloat()*360,0);
        mob.finalizeSpawn(l,l.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.MOB_SUMMONED,null);mob.setTarget(player);
        if(l.addFreshEntity(mob)){thaumcraft.port.network.WorldFxPayload.send(l,0,mob.position().add(0,1,0),mob.position().add(0,1,0),0xffffff);mob.playSound(thaumcraft.port.content.PortSounds.get("wandfail"),1,1);hurt(damageSources().generic(),5+random.nextInt(5));}
    }
    @Override public void playerTouch(Player p){if(!level().isClientSide&&distanceToSqr(p)<3)p.hurt(damageSources().indirectMagic(this,this),4);}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){super.die(source);if(!level().isClientSide)level().explode(this,getX(),getY()+1,getZ(),1.5f,Level.ExplosionInteraction.NONE);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("active",active());t.putInt("spawnDelay",delay);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);entityData.set(ACTIVE,t.getBoolean("active"));delay=t.contains("spawnDelay")?Math.max(1,t.getInt("spawnDelay")):100;}
}
