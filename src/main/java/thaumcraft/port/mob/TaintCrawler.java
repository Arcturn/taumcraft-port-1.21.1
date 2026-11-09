package thaumcraft.port.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.world.*;

/** Small ground predator that leaves taint fibre along suitable surfaces. */
public final class TaintCrawler extends Monster implements TaintedMob {
    public TaintCrawler(EntityType<? extends TaintCrawler> type,Level level){super(type,level);xpReward=3;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,.275).add(Attributes.ATTACK_DAMAGE,2);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(3,new RandomStrollGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean canAttackType(EntityType<?> type){return !TaintedMob.isTaintedType(type)&&super.canAttackType(type);}
    @Override public boolean isAlliedTo(Entity other){return other instanceof TaintedMob||super.isAlliedTo(other);}
    @Override public void setTarget(LivingEntity target){super.setTarget(target instanceof TaintedMob?null:target);}
    @Override protected Entity.MovementEmission getMovementEmission(){return Entity.MovementEmission.NONE;}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.SILVERFISH_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SILVERFISH_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SILVERFISH_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.SILVERFISH_STEP,.15f,1);}
    @Override public float getVoicePitch(){return .7f;}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){return group;}
    @Override public void tick(){if(!level().isClientSide&&isAlive()&&tickCount%40==0)leaveFibre();super.tick();}
    /** BETA26 checked a fresh BlockPos by reference, so this intentionally runs even when stationary. */
    public void leaveFibre(){
        if(!(level() instanceof ServerLevel level)||!WorldConfig.TAINT.get()||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
        var pos=blockPosition();var state=level.getBlockState(pos);
        if(state.is(BlockTags.LEAVES)||!state.getFluidState().isEmpty()||TaintSystem.tainted(state)||state.hasBlockEntity()||!(state.canBeReplaced()||state.getBlock() instanceof net.minecraft.world.level.block.BushBlock))return;
        boolean support=false;
        for(var direction:Direction.values()){
            var neighbour=pos.relative(direction);if(!level.hasChunkAt(neighbour))return;
            var neighbourState=level.getBlockState(neighbour);
            if(!TaintSystem.tainted(neighbourState)&&neighbourState.isFaceSturdy(level,neighbour,direction.getOpposite()))support=true;
        }
        if(support)level.setBlockAndUpdate(pos,((TaintBlock)WorldContent.block("taint_fibre")).attached(level,pos));
    }
    @Override public boolean doHurtTarget(Entity target){
        if(target instanceof TaintedMob||!super.doHurtTarget(target))return false;
        int strength=level().getDifficulty()==Difficulty.NORMAL?3:level().getDifficulty()==Difficulty.HARD?6:0;
        if(target instanceof LivingEntity living&&strength>0&&random.nextInt(strength+1)>2)living.addEffect(new MobEffectInstance(WorldEntities.TAINT,strength*20),this);
        return true;
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){if(random.nextInt(8)==0)spawnAtLocation(CrystalItem.stack(Aspect.FLUX),getBbHeight()/2);}
}
