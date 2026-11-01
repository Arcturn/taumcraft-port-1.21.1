package thaumcraft.port.crafting.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class HungryChestEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final IItemHandler handler = new InvWrapper(this);

    public HungryChestEntity(BlockPos pos, BlockState state) { super(CraftingContent.HUNGRY_CHEST_ENTITY.get(), pos, state); }
    public IItemHandler itemHandler() { return handler; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(getMaxStackSize(stack)); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.hungry_chest"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return ChestMenu.threeRows(id, inventory, this); }

    public void collect(ItemEntity dropped) {
        if (level == null || level.isClientSide || !dropped.isAlive() || dropped.getItem().isEmpty()) return;
        int before = dropped.getItem().getCount();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, dropped.getItem(), false);
        if (remainder.getCount() == before) return;
        if (remainder.isEmpty()) dropped.discard(); else dropped.setItem(remainder);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, .25f, .9f + level.random.nextFloat() * .2f);
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
