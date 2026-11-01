package thaumcraft.port.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The original brain upgrade attaches inward to either half of a thaumatorium. */
public final class BrainBoxBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

    public BrainBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction inward = context.getClickedFace().getOpposite();
        BlockState support = context.getLevel().getBlockState(context.getClickedPos().relative(inward));
        BlockState state = defaultBlockState().setValue(FACING, inward);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                && support.getValue(ThaumatoriumBlock.FACING) != context.getClickedFace() ? state : null;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState support = level.getBlockState(pos.relative(state.getValue(FACING)));
        return support.is(EssentiaContent.THAUMATORIUM.get()) || support.is(EssentiaContent.THAUMATORIUM_TOP.get());
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
