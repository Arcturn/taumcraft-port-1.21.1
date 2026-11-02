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
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class HungryChestEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final IItemHandler handler = new InvWrapper(this);
    private final ChestLidController lid = new ChestLidController();
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override protected void onOpen(Level world,BlockPos pos,BlockState state) { sound(SoundEvents.CHEST_OPEN); }
        @Override protected void onClose(Level world,BlockPos pos,BlockState state) { sound(SoundEvents.CHEST_CLOSE); }
        @Override protected void openerCountChanged(Level world,BlockPos pos,BlockState state,int oldCount,int count) { world.blockEvent(pos,state.getBlock(),1,count); }
        @Override protected boolean isOwnContainer(Player player) { return player.containerMenu instanceof ChestMenu menu && menu.getContainer()==HungryChestEntity.this; }
    };

    public HungryChestEntity(BlockPos pos, BlockState state) { super(CraftingContent.HUNGRY_CHEST_ENTITY.get(), pos, state); }
    public IItemHandler itemHandler() { return handler; }
    public float openness(float partialTick) { return lid.getOpenness(partialTick); }
    public int openCount() { return openers.getOpenerCount(); }
    public void tick() {
        if(level==null) return;
        if(level.isClientSide) lid.tickLid();
        else if(level.getGameTime()%5==0) openers.recheckOpeners(level,worldPosition,getBlockState());
    }
    private void sound(net.minecraft.sounds.SoundEvent sound) { if(level!=null) level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,.5f,.9f+level.random.nextFloat()*.1f); }
    @Override public void startOpen(Player player) { if(!isRemoved() && !player.isSpectator() && level!=null && !level.isClientSide) openers.incrementOpeners(player,level,worldPosition,getBlockState()); }
    @Override public void stopOpen(Player player) { if(!isRemoved() && !player.isSpectator() && level!=null && !level.isClientSide) openers.decrementOpeners(player,level,worldPosition,getBlockState()); }
    @Override public boolean triggerEvent(int event,int data) { if(event==1) { lid.shouldBeOpen(data>0); return true; } return super.triggerEvent(event,data); }
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
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag=saveWithoutMetadata(registries);tag.putInt("OpenCount",openers.getOpenerCount());return tag;
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        lid.shouldBeOpen(tag.getInt("OpenCount")>0);
    }
}
