package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public abstract class DisplayPedestalBlock extends Block implements EntityBlock {
    protected DisplayPedestalBlock(Properties properties) { super(properties); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        interact(level, pos, player, stack); return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        interact(level, pos, player, ItemStack.EMPTY); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    protected void interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof DisplayPedestalEntity pedestal)) return;
        boolean inserted = false;
        if (pedestal.isEmpty() && !held.isEmpty() && pedestal.canPlaceItem(0, held)) {
            pedestal.setItem(0, held.copyWithCount(1)); held.shrink(1); inserted = true;
            player.getInventory().setChanged();
        } else if (!pedestal.isEmpty()) {
            Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), pedestal.removeItem(0, pedestal.getMaxStackSize()));
        } else return;
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f,
                ((level.random.nextFloat() - level.random.nextFloat()) * .7f + 1) * (inserted ? 1.6f : 1.5f));
    }
    @Override protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int event, int parameter) {
        return level.getBlockEntity(pos) instanceof DisplayPedestalEntity pedestal && pedestal.triggerEvent(event, parameter);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof DisplayPedestalEntity pedestal) Containers.dropContents(level, pos, pedestal);
        super.onRemove(state, level, pos, next, moving);
    }
}
