package thaumcraft.port.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class CondenserBlock extends Block implements EntityBlock {
    public static final BooleanProperty ENABLED=BlockStateProperties.ENABLED;
    public CondenserBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(ENABLED,!context.getLevel().hasNeighborSignal(context.getClickedPos()));}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(!level.isClientSide){boolean enabled=!level.hasNeighborSignal(pos);if(state.getValue(ENABLED)!=enabled)level.setBlock(pos,state.setValue(ENABLED,enabled),3);}
        if(level.getBlockEntity(pos) instanceof CondenserEntity condenser)condenser.invalidateLattice();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CondenserEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==CondenserContent.ENTITY.get()?(l,p,s,be)->((CondenserEntity)be).tick():null;}
}
