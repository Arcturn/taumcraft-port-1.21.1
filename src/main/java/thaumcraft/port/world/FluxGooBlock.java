package thaumcraft.port.world;

import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.warp.WarpEffects;

/** Eight-quanta finite goo. The TC6 viscosity of6000 gives a30-tick scheduled update. */
public final class FluxGooBlock extends Block {
    public static final IntegerProperty LEVEL=IntegerProperty.create("level",0,7);
    public static final BooleanProperty COVERED=BooleanProperty.create("covered");
    public static final int TICK_DELAY=30;
    public FluxGooBlock(Properties properties){super(properties.noCollission().noOcclusion().randomTicks());registerDefaultState(stateDefinition.any().setValue(LEVEL,7).setValue(COVERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LEVEL,COVERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return stateFor(context.getLevel(),context.getClickedPos(),8);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(0,0,0,16,state.getValue(COVERED)?16:(state.getValue(LEVEL)+1)*16d/9d,16);}
    @Override protected boolean canBeReplaced(BlockState state,BlockPlaceContext context){return state.getValue(LEVEL)<4;}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){return direction==Direction.UP?state.setValue(COVERED,neighbour.is(this)):state;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){
        boolean covered=level.getBlockState(pos.above()).is(this);if(state.getValue(COVERED)!=covered)level.setBlock(pos,state.setValue(COVERED,covered),2);
        level.scheduleTick(pos,this,TICK_DELAY);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbour,BlockPos neighbourPos,boolean moved){level.scheduleTick(pos,this,TICK_DELAY);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){advance(level,pos,random);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){advance(level,pos,random);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(random.nextInt(44)<=state.getValue(LEVEL))TaintVisualHooks.GOO_BUBBLE.accept(level,pos);}

    /** Slime emergence and evaporation precede finite flow on both scheduled and random ticks. */
    public void advance(ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!level.hasChunkAt(pos))return;
        var state=level.getBlockState(pos);if(!state.is(this))return;int meta=state.getValue(LEVEL);
        if(meta>=2&&level.isEmptyBlock(pos.above())&&random.nextInt(50)==0){
            var slime=WorldEntities.SLIME.get().create(level);
            if(slime!=null){slime.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);slime.setSize(meta>=6?2:1,true);if(level.addFreshEntity(slime)){level.removeBlock(pos,false);slime.playSound(PortSounds.get("gore"),1,1);}}
            return;
        }
        if(random.nextInt(4)==0){
            if(meta==0){
                if(random.nextBoolean()){AuraSystem.pollute(level,pos,1,true);level.removeBlock(pos,false);}
                else level.setBlockAndUpdate(pos,((TaintBlock)WorldContent.block("taint_fibre")).attached(level,pos));
            }else{level.setBlock(pos,state.setValue(LEVEL,meta-1),2);AuraSystem.pollute(level,pos,1,true);}
            return;
        }
        flow(level,pos,random);
    }

    /** Shares a finite amount with lower neighbours; never creates water-style source blocks. */
    public void flow(ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!level.hasChunkAt(pos))return;
        var state=level.getBlockState(pos);if(!state.is(this))return;
        int units=state.getValue(LEVEL)+1,original=units;BlockPos below=pos.below();
        if(level.isOutsideBuildHeight(below)){level.removeBlock(pos,false);return;}
        if(receivable(level,below)){
            int there=amount(level,below),moved=Math.min(units,8-there);
            if(moved>0){displace(level,below);put(level,below,there+moved);level.scheduleTick(below,this,TICK_DELAY);units-=moved;}
            if(units==0){level.removeBlock(pos,false);return;}
        }
        if(units==1){if(units!=original)put(level,pos,units);return;}
        var destinations=new ArrayList<BlockPos>();int total=units;
        for(var direction:Direction.Plane.HORIZONTAL){
            BlockPos next=pos.relative(direction);if(!receivable(level,next))continue;
            int there=amount(level,next);if(there<units-1){destinations.add(next);total+=there;}
        }
        if(destinations.isEmpty()){if(units!=original)put(level,pos,units);return;}
        int count=destinations.size()+1,each=total/count,remainder=total%count;
        for(BlockPos next:destinations){
            int share=each;
            if(remainder==count||remainder>1&&random.nextInt(count-remainder)!=0){share++;remainder--;}
            if(share!=amount(level,next)){displace(level,next);put(level,next,share);level.scheduleTick(next,this,TICK_DELAY);}
            count--;
        }
        put(level,pos,each+(remainder>0?1:0));
    }
    private int amount(ServerLevel level,BlockPos pos){var state=level.getBlockState(pos);return state.is(this)?state.getValue(LEVEL)+1:0;}
    private boolean receivable(ServerLevel level,BlockPos pos){
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);return state.getFluidState().isEmpty()&&!state.hasBlockEntity()&&(state.is(this)||state.isAir()||state.canBeReplaced()||state.is(WorldContent.block("taint_fibre")));
    }
    private void displace(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(!state.isAir()&&!state.is(this)&&!state.is(Blocks.SNOW))Block.dropResources(state,level,pos);
    }
    private BlockState stateFor(BlockGetter level,BlockPos pos,int units){return defaultBlockState().setValue(LEVEL,units-1).setValue(COVERED,level.getBlockState(pos.above()).is(this));}
    private void put(ServerLevel level,BlockPos pos,int units){
        if(units<=0)level.removeBlock(pos,false);
        else level.setBlock(pos,stateFor(level,pos,units),2);
        // Finite-flow updates use flag2; keep the lower block's covered render state synchronized too.
        BlockPos below=pos.below();if(!level.isOutsideBuildHeight(below)){
            var lower=level.getBlockState(below);if(lower.is(this)&&lower.getValue(COVERED)!=(units>0))level.setBlock(below,lower.setValue(COVERED,units>0),2);
        }
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        int meta=state.getValue(LEVEL);
        if(entity instanceof ThaumicSlime slime){
            if(!level.isClientSide&&slime.getSize()<meta&&level.random.nextBoolean()){
                slime.setSize(slime.getSize()+1,true);
                if(meta>1)level.setBlock(pos,state.setValue(LEVEL,meta-1),2);else level.removeBlock(pos,false);
            }
        }else{
            double factor=1-(meta+1)/8d;entity.setDeltaMovement(entity.getDeltaMovement().multiply(factor,1,factor));
            if(!level.isClientSide&&entity instanceof LivingEntity living)
                living.addEffect(WarpEffects.uncurable(new MobEffectInstance(WorldEntities.VIS_EXHAUST,600,meta/3,true,true)));
        }
    }
}
