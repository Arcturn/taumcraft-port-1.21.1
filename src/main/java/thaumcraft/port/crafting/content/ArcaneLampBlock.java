package thaumcraft.port.crafting.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.artifice.lamp.LampEntity;

/** TC6 lamps attach to any face; facing is the supporting/essentia input side. */
public final class ArcaneLampBlock extends Block implements EntityBlock {
    public enum Kind { ARCANE, GROWTH, FERTILITY }
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public final Kind kind;
    public ArcaneLampBlock(Properties properties) { this(properties, Kind.ARCANE); }
    public ArcaneLampBlock(Properties properties, Kind kind) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(LIT, kind == Kind.ARCANE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite())
                .setValue(LIT, kind == Kind.ARCANE && !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 2, 4, 12, 14, 12); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LampEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == thaumcraft.port.artifice.lamp.ArtificeLamps.ENTITY.get() ? (world,pos,block,entity) -> ((LampEntity)entity).tick() : null;
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (level.isClientSide) return;
        if (level.isEmptyBlock(pos.relative(state.getValue(FACING)))) { level.destroyBlock(pos, true); return; }
        if (level.getBlockEntity(pos) instanceof LampEntity lamp) lamp.updateLight();
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof LampEntity lamp) lamp.removeLights();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int event, int data) {
        return level.getBlockEntity(pos) instanceof LampEntity lamp && lamp.triggerEvent(event, data);
    }
}
