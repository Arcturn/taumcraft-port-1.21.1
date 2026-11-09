package thaumcraft.port.world;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortSounds;

/** TC6 falling crust: a real falling block, without anvil damage or ordinary item drops. */
public final class FallingTaint extends Entity {
    private static final EntityDataAccessor<BlockState> BLOCK=SynchedEntityData.defineId(FallingTaint.class,EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<BlockPos> START=SynchedEntityData.defineId(FallingTaint.class,EntityDataSerializers.BLOCK_POS);
    /** Installed only by the client; the common entity remains safe on a dedicated server. */
    public static Consumer<FallingTaint> LANDING_FX=entity->{};
    public int fallTime;
    private boolean sourceRemoved;

    public FallingTaint(EntityType<? extends FallingTaint> type,Level level){super(type,level);blocksBuilding=true;}
    public FallingTaint(Level level,BlockPos source,BlockPos destination,BlockState state){
        this(WorldEntities.FALLING_TAINT.get(),level);
        setBlockState(state);entityData.set(START,source.immutable());
        setPos(destination.getX()+.5,destination.getY()+.5,destination.getZ()+.5);
        xo=getX();yo=getY();zo=getZ();setDeltaMovement(Vec3.ZERO);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){data.define(BLOCK,Blocks.AIR.defaultBlockState());data.define(START,BlockPos.ZERO);}
    public BlockState getBlockState(){return entityData.get(BLOCK);}
    public BlockState getBlock(){return getBlockState();}
    public void setBlockState(BlockState state){entityData.set(BLOCK,state);}
    public BlockPos getStartPos(){return entityData.get(START);}
    public boolean hasRemovedSource(){return sourceRemoved;}

    /** Source and destination can differ when the upper crust of a column slides sideways. */
    public static boolean startFall(ServerLevel level,BlockPos source,BlockPos destination){
        if(!WorldConfig.TAINT.get()||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ||level.isOutsideBuildHeight(destination)||!loadedAround(level,source)||!loadedAround(level,destination)
                ||!level.getWorldBorder().isWithinBounds(source)||!level.getWorldBorder().isWithinBounds(destination))return false;
        var state=level.getBlockState(source);
        if(!state.is(WorldContent.block("taint_crust"))||!TaintSystem.onlyAdjacentToTaint(level,source)||!canFallBelow(level,destination.below()))return false;
        if(!source.equals(destination)&&!level.isEmptyBlock(destination))return false;
        // Random ticks can select the same still-present source before its first entity tick.
        if(!level.getEntitiesOfClass(FallingTaint.class,new AABB(destination).inflate(1),entity->entity.getStartPos().equals(source)&&!entity.sourceRemoved).isEmpty())return false;
        return level.addFreshEntity(new FallingTaint(level,source,destination,state));
    }

    private static boolean loadedAround(LevelReader level,BlockPos pos){return level.hasChunksAt(pos.getX()-1,pos.getZ()-1,pos.getX()+1,pos.getZ()+1);}
    /** Logs brace nearby crust; deep finite goo supports it, shallow goo and fluids do not. */
    public static boolean canFallBelow(LevelReader level,BlockPos pos){
        if(!loadedAround(level,pos))return false;
        for(BlockPos nearby:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))){
            var state=level.getBlockState(nearby);
            if(state.is(BlockTags.LOGS)||state.is(WorldContent.block("taint_log")))return false;
        }
        var state=level.getBlockState(pos);
        if(state.is(WorldContent.block("flux_goo")))return state.getValue(FluxGooBlock.LEVEL)<4;
        return state.isAir()||state.is(BlockTags.FIRE)||state.is(WorldContent.block("taint_fibre"))||state.canBeReplaced()||state.getBlock() instanceof LiquidBlock;
    }

    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isAttackable(){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean displayFireAnimation(){return false;}
    @Override public SoundSource getSoundSource(){return SoundSource.BLOCKS;}
    @Override public void tick(){
        if(isRemoved())return;
        var state=getBlockState();
        if(state.isAir()){discard();return;}
        if(!level().isClientSide){
            if(!loadedAround(level(),blockPosition()))return;
            if(!sourceRemoved){
                if(!level().hasChunkAt(getStartPos()))return;
                if(level().getBlockState(getStartPos())!=state){discard();return;}
                level().removeBlock(getStartPos(),false);sourceRemoved=true;
            }
        }
        xo=getX();yo=getY();zo=getZ();++fallTime;
        if(!isNoGravity())setDeltaMovement(getDeltaMovement().add(0,-.04,0));
        var next=BlockPos.containing(position().add(getDeltaMovement()));
        if(!level().isClientSide&&!loadedAround(level(),next))return;
        move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.98));
        var pos=blockPosition();
        if(level().isClientSide){if(fallTime==1)LANDING_FX.accept(this);return;}
        // TC6 stored the timer in a byte; retain the complete lifetime across world saves.
        if(fallTime>600||(fallTime>100&&level().isOutsideBuildHeight(pos))){discard();return;}
        boolean onGoo=level().getBlockState(pos.below()).is(WorldContent.block("flux_goo"))&&!canFallBelow(level(),pos.below());
        if(!onGround()&&!onGoo)return;
        setDeltaMovement(getDeltaMovement().multiply(.7,-.5,.7));
        var current=level().getBlockState(pos);
        if(current.is(Blocks.PISTON)||current.is(Blocks.MOVING_PISTON)||current.is(Blocks.PISTON_HEAD))return;
        playSound(PortSounds.get("gore"),.5f,((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);
        level().broadcastEntityEvent(this,(byte)3);
        if(canPlace(pos)&&!canFallBelow(level(),pos.below()))level().setBlockAndUpdate(pos,state);
        discard();
    }
    private boolean canPlace(BlockPos pos){
        if(level().isOutsideBuildHeight(pos)||!level().hasChunkAt(pos)||!level().getWorldBorder().isWithinBounds(pos))return false;
        var current=level().getBlockState(pos);
        return !current.hasBlockEntity()&&(current.is(WorldContent.block("taint_fibre"))||current.is(WorldContent.block("flux_goo"))||current.canBeReplaced());
    }
    @Override public void handleEntityEvent(byte event){if(event==3)LANDING_FX.accept(this);else super.handleEntityEvent(event);}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        tag.put("BlockState",NbtUtils.writeBlockState(getBlockState()));tag.putLong("Old",getStartPos().asLong());tag.putInt("Time",fallTime);tag.putBoolean("SourceRemoved",sourceRemoved);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        setBlockState(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK),tag.getCompound("BlockState")));
        entityData.set(START,BlockPos.of(tag.getLong("Old")));fallTime=Math.max(0,tag.getInt("Time"));sourceRemoved=tag.contains("SourceRemoved")?tag.getBoolean("SourceRemoved"):fallTime>0;
    }
}
