package thaumcraft.port.golem.seal;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Ghost item filters never hold real inventory and serialize modern item components. */
public abstract class SealFiltered implements ISeal,ISealGui,ISealConfigFilter {
    protected NonNullList<ItemStack> filter=NonNullList.withSize(getFilterSize(),ItemStack.EMPTY);
    protected NonNullList<Integer> filterSize=NonNullList.withSize(getFilterSize(),0);
    protected boolean blacklist=true;
    @Override public void readCustomNBT(CompoundTag tag,HolderLookup.Provider registries){
        filter=NonNullList.withSize(getFilterSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,filter,registries);
        for(int i=0;i<filter.size();i++)filter.set(i,filter.get(i).copyWithCount(filter.get(i).isEmpty()?0:1));
        blacklist=tag.getBoolean("bl");filterSize=NonNullList.withSize(getFilterSize(),0);
        for(var value:tag.getList("Sizes",Tag.TAG_COMPOUND)){var entry=(CompoundTag)value;int slot=entry.getByte("Slot")&255;if(slot<filterSize.size())setFilterSlotSize(slot,entry.getInt("Size"));}
    }
    @Override public void writeCustomNBT(CompoundTag tag,HolderLookup.Provider registries){
        ContainerHelper.saveAllItems(tag,filter,registries);tag.putBoolean("bl",blacklist);var sizes=new ListTag();
        for(int i=0;i<filterSize.size();i++)if(filterSize.get(i)>0){var value=new CompoundTag();value.putByte("Slot",(byte)i);value.putInt("Size",filterSize.get(i));sizes.add(value);}tag.put("Sizes",sizes);
    }
    public int[] getGuiCategories(){return new int[]{0};}
    public int getFilterSize(){return 1;}
    public NonNullList<ItemStack> getInv(){return filter;}
    public NonNullList<Integer> getSizes(){return filterSize;}
    public ItemStack getFilterSlot(int slot){return filter.get(slot);}
    public int getFilterSlotSize(int slot){return filterSize.get(slot);}
    public void setFilterSlot(int slot,ItemStack stack){filter.set(slot,stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1));}
    public void setFilterSlotSize(int slot,int count){filterSize.set(slot,Math.max(0,count));}
    public boolean isBlacklist(){return blacklist;}
    public void setBlacklist(boolean value){blacklist=value;}
    public boolean hasStacksizeLimiters(){return false;}
}
