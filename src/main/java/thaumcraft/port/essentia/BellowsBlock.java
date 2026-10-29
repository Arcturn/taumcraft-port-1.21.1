package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class BellowsBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED=BlockStateProperties.ENABLED;
    public BellowsBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite()).setValue(ENABLED,!c.getLevel().hasNeighborSignal(c.getClickedPos()));}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide){boolean enabled=!l.hasNeighborSignal(p);if(s.getValue(ENABLED)!=enabled)l.setBlock(p,s.setValue(ENABLED,enabled),3);}}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BellowsEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==EssentiaContent.BELLOWS_ENTITY.get()?(w,p,st,e)->((BellowsEntity)e).tick():null;}
}
