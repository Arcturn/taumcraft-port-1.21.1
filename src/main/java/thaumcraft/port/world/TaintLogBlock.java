package thaumcraft.port.world;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Tainted log keeps its axis and decays to flux goo immediately when its seed disappears. */
public final class TaintLogBlock extends TaintBlock {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.AXIS;
    public TaintLogBlock(Properties properties){super("taint_log",properties);registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.Y));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AXIS);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(AXIS,context.getClickedFace().getAxis());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return RotatedPillarBlock.rotatePillar(state,rotation);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return net.minecraft.world.phys.shapes.Shapes.block();}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!WorldConfig.TAINT.get()||!TaintSystem.neighboursLoaded(level,pos))return;if(!TaintSystem.nearby(level,pos))die(level,pos,state);else TaintSystem.spread(level,pos,false);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moved){
        super.onRemove(state,level,pos,replacement,moved);if(level.isClientSide||replacement.is(this))return;
        for(var candidate:BlockPos.betweenClosed(pos.offset(-4,-4,-4),pos.offset(4,4,4)))if(level.hasChunkAt(candidate)){
            var leaf=level.getBlockState(candidate);if(leaf.is(BlockTags.LEAVES))level.scheduleTick(candidate,leaf.getBlock(),1);
        }
    }
    public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 4;}
    public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 4;}
}
