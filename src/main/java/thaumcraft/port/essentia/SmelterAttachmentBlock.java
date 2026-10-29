package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class SmelterAttachmentBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public SmelterAttachmentBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(!c.getClickedFace().getAxis().isHorizontal())return null;var s=defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite());return canSurvive(s,c.getLevel(),c.getClickedPos())?s:null;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var q=p.relative(s.getValue(FACING));if(!l.hasChunkAt(q))return true;var support=l.getBlockState(q);return support.getBlock() instanceof SmelterBlock&&support.getValue(SmelterBlock.FACING)!=s.getValue(FACING).getOpposite();}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide&&!canSurvive(s,l,p))l.destroyBlock(p,true);}
}
