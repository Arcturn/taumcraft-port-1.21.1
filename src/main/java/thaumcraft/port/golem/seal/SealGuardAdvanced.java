package thaumcraft.port.golem.seal;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.golem.EnumGolemTrait;

public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:guard_advanced"; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_guard_advanced"); }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{2,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER,EnumGolemTrait.SMART}; }
}
