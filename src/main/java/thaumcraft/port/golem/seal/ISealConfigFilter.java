package thaumcraft.port.golem.seal;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
public interface ISealConfigFilter {
    NonNullList<ItemStack> getInv();
    NonNullList<Integer> getSizes();
    int getFilterSize();
    ItemStack getFilterSlot(int slot);
    int getFilterSlotSize(int slot);
    void setFilterSlot(int slot, ItemStack stack);
    void setFilterSlotSize(int slot, int count);
    boolean isBlacklist();
    void setBlacklist(boolean value);
    boolean hasStacksizeLimiters();
}
