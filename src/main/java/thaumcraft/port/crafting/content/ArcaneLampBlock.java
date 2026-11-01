package thaumcraft.port.crafting.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Local lamp illumination and the original redstone off switch. */
public final class ArcaneLampBlock extends CraftingDeviceBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ArcaneLampBlock(Properties properties) {
        super(properties, Block.box(4, 2, 4, 12, 14, 12));
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) {
            boolean lit = !level.hasNeighborSignal(pos);
            if (lit != state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, lit), 3);
        }
    }
}
