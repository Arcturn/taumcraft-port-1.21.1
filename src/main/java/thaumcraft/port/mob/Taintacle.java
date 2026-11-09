package thaumcraft.port.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.world.*;

/** Rooted tainted tentacle, including the source's ranged small-tentacle summon. */
public class Taintacle extends Monster implements TaintedMob {
    public float flailIntensity=1;
    public Taintacle(EntityType<? extends Taintacle> type,Level level){super(type,level);xpReward=8;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.MOVEMENT_SPEED,0);}
    public boolean isSmall(){return this instanceof TaintacleSmall;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(3,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean isAlliedTo(Entity other){return other instanceof TaintedMob||super.isAlliedTo(other);}
    @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof TaintedMob?null:target);}
    @Override public boolean canAttackType(EntityType<?> type){return !TaintedMob.isTaintedType(type)&&super.canAttackType(type);}
    @Override public void move(MoverType type,Vec3 movement){super.move(type,new Vec3(0,Math.min(0,movement.y),0));}
    public static boolean onTaint(LevelReader level,BlockPos pos){return level.hasChunkAt(pos)&&TaintSystem.tainted(level.getBlockState(pos))||level.hasChunkAt(pos.below())&&TaintSystem.tainted(level.getBlockState(pos.below()));}
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return onTaint(level,blockPosition())&&level.getDifficulty()!=Difficulty.PEACEFUL;}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){if(flailIntensity>1)flailIntensity=Math.max(1,flailIntensity-.01f);return;}
        if(!isAlive()||tickCount%20!=0)return;
        if(!onTaint(level(),blockPosition()))decayDamage(damageSources().starve(),1);
        var target=getTarget();
        if(!isSmall()&&tickCount%40==0&&target!=null&&target.isAlive()&&distanceToSqr(target)>16&&distanceToSqr(target)<256&&hasLineOfSight(target))spawnTentacles(target);
    }
    /** The original damageEntity call bypassed hit cooldown and did not trigger death loot. */
    protected void decayDamage(DamageSource source,float amount){
        // NeoForge's actuallyHurt requires a damage container even outside the normal hurt path.
        damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source,amount));
        try{actuallyHurt(source,amount);}finally{damageContainers.pop();}
    }
    /** Returns the created minion, or null when the target's terrain is unsuitable/unloaded. */
    public TaintacleSmall spawnTentacles(Entity target){return isSmall()?null:summonTentacle(this,target);}
    /** Shared summoning path used by taintacles and the existing taint seeds. */
    public static TaintacleSmall summonTentacle(Mob summoner,Entity target){
        if(!(summoner.level() instanceof ServerLevel level)||!target.isAlive()||target instanceof TaintedMob||!level.hasChunkAt(target.blockPosition()))return null;
        var random=summoner.getRandom();
        boolean eldritch=level.getBiome(target.blockPosition()).is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("thaumcraft:eldritch")));
        if(!eldritch&&!onTaint(level,target.blockPosition()))return null;
        var small=MobContent.TAINTACLE_SMALL.get().create(level);if(small==null)return null;
        small.moveTo(target.getX()+random.nextFloat()-random.nextFloat(),target.getY(),target.getZ()+random.nextFloat()-random.nextFloat(),0,0);
        if(!level.hasChunkAt(small.blockPosition())||!level.getWorldBorder().isWithinBounds(small.getBoundingBox()))return null;
        if(!level.addFreshEntity(small))return null;
        summoner.playSound(PortSounds.get("tentacle"),summoner.getBbHeight()/8,1.3f-summoner.getBbHeight()/10);
        if(eldritch&&level.isEmptyBlock(target.blockPosition())&&WorldConfig.TAINT.get()&&level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)){
            var pos=target.blockPosition();boolean supported=false,loaded=true;
            for(var direction:net.minecraft.core.Direction.values()){
                var q=pos.relative(direction);if(!level.hasChunkAt(q)){loaded=false;break;}
                if(level.getBlockState(q).isFaceSturdy(level,q,direction.getOpposite()))supported=true;
            }
            if(loaded&&supported)level.setBlockAndUpdate(pos,((TaintBlock)WorldContent.block("taint_fibre")).attached(level,pos));
        }
        return small;
    }
    @Override public boolean doHurtTarget(Entity target){
        if(target instanceof TaintedMob)return false;
        level().broadcastEntityEvent(this,(byte)16);playSound(PortSounds.get("tentacle"),getSoundVolume(),getVoicePitch());return super.doHurtTarget(target);
    }
    @Override public void handleEntityEvent(byte event){if(event==16)flailIntensity=3;else super.handleEntityEvent(event);}
    @Override public int getAmbientSoundInterval(){return 200;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.CHORUS_FLOWER_DEATH;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return PortSounds.get("tentacle");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("tentacle");}
    @Override public float getVoicePitch(){return 1.3f-getBbHeight()/10;}
    @Override protected float getSoundVolume(){return getBbHeight()/8;}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);}
}
