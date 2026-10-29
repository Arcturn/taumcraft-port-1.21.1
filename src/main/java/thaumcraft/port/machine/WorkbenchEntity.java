package thaumcraft.port.machine;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WorkbenchEntity extends BlockEntity implements Container,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(15,ItemStack.EMPTY);
    public WorkbenchEntity(BlockPos pos,BlockState state){super(MachineContent.WORKBENCH_ENTITY.get(),pos,state);}
    public int getContainerSize(){return items.size();}
    public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    public ItemStack getItem(int i){return items.get(i);}
    public ItemStack removeItem(int i,int n){var item=ContainerHelper.removeItem(items,i,n);if(!item.isEmpty())setChanged();return item;}
    public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    public void setItem(int i,ItemStack stack){items.set(i,stack);stack.limitSize(getMaxStackSize(stack));setChanged();}
    public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    public void clearContent(){items.clear();setChanged();}
    public Component getDisplayName(){return Component.translatable("block.thaumcraft.arcane_workbench");}
    public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new WorkbenchMenu(id,inventory,this,worldPosition);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items.clear();ContainerHelper.loadAllItems(tag,items,registries);}
}
