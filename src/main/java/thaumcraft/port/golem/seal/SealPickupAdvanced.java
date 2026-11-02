package thaumcraft.port.golem.seal;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.golem.EnumGolemTrait;

public final class SealPickupAdvanced extends SealPickup implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:pickup_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_pickup_advanced"); }
    @Override public int[] getGuiCategories() { return new int[]{2,1,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.SMART}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int index, boolean value) { if (index >= 0 && index < props.length) props[index].setValue(value); }
}
