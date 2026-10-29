package thaumcraft.port.essentia;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class BrainBoxBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public BrainBoxBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite());}
}
