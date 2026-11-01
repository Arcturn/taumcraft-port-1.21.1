package thaumcraft.port.infusion.structure;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** The single exposed altar slot; all faces support inventory automation as in TC6. */
public abstract class DisplayPedestalEntity extends BlockEntity implements WorldlyContainer {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    protected DisplayPedestalEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    public void setInventorySlotContentsFromInfusion(int slot, ItemStack stack) { setItem(slot, stack); }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack removed = ContainerHelper.removeItem(items, slot, amount); if (!removed.isEmpty()) sync(); return removed; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(getMaxStackSize(stack)); sync(); }
    @Override public void clearContent() { items.clear(); sync(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && (stack.isEmpty() || isEmpty()); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        items.getFirst().limitSize(getMaxStackSize(items.getFirst()));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean triggerEvent(int event, int parameter) {
        if (event == 5 || event == 11 || event == 12) {
            if (level != null && level.isClientSide) StructureEffects.blockEvent(this, event, parameter);
            return true;
        }
        return super.triggerEvent(event, parameter);
    }
}
