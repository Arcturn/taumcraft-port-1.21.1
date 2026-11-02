package thaumcraft.port.crafting.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class HungryChestBlock extends CraftingDeviceBlock implements EntityBlock {
    public HungryChestBlock(Properties properties) { super(properties, Block.box(1, 0, 1, 15, 14, 15)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HungryChestEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CraftingContent.HUNGRY_CHEST_ENTITY.get() ? (world,pos,block,entity) -> ((HungryChestEntity)entity).tick() : null;
    }
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int value) {
        return level.getBlockEntity(pos) instanceof HungryChestEntity chest && chest.triggerEvent(id,value);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof HungryChestEntity chest) serverPlayer.openMenu(chest);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity dropped && dropped.isAlive()
                && level.getBlockEntity(pos) instanceof HungryChestEntity chest) chest.collect(dropped);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof HungryChestEntity chest) {
            Containers.dropContents(level, pos, chest);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HungryChestEntity chest ? AbstractContainerMenu.getRedstoneSignalFromContainer(chest) : 0;
    }
}
