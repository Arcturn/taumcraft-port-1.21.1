package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class StabilizerBlock extends Block implements EntityBlock, InfusionStabilizer {
    public StabilizerBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StabilizerEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return !level.isClientSide && type == StructureContent.STABILIZER_ENTITY.get() ? (world, pos, block, entity) -> ((StabilizerEntity)entity).tick() : null; }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) InlayNetwork.update(level, pos.relative(face));
    }
    @Override public float stabilization(Level level, BlockPos pos) { return .25f; }
}
