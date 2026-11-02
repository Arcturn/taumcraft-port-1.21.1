package thaumcraft.port.golem.seal;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.golem.EnumGolemTrait;

public final class SealFillAdvanced extends SealFill implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:fill_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_fill_advanced"); }
    @Override public int[] getGuiCategories() { return new int[]{1,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.SMART}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int index, boolean value) { if (index >= 0 && index < props.length) props[index].setValue(value); }
}
