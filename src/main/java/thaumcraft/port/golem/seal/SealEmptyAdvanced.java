package thaumcraft.port.golem.seal;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.EnumGolemTrait;

public final class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:empty_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_empty_advanced"); }
    @Override public NonNullList<ItemStack> getInv(int cycle) {
        if (props[4].value && !isBlacklist()) {
            var choices=super.getInv().stream().filter(s->!s.isEmpty()).toList();
            if (!choices.isEmpty()) return NonNullList.withSize(1,choices.get(Math.floorMod(cycle,choices.size())));
        }
        return super.getInv();
    }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{1,3,0,4}; }
    @Override public void setToggle(int index,boolean value) { if(index>=0&&index<props.length)props[index].setValue(value); }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.SMART}; }
}
