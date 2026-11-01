package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class RechargePedestalBlock extends DisplayPedestalBlock {
    public RechargePedestalBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RechargePedestalEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == StructureContent.RECHARGE_ENTITY.get() ? (world, pos, block, entity) -> ((RechargePedestalEntity)entity).tick() : null;
    }
}
