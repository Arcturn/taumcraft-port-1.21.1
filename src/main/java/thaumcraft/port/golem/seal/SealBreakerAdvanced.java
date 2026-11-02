package thaumcraft.port.golem.seal;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.golem.EnumGolemTrait;

public class SealBreakerAdvanced extends SealBreaker {
    public SealBreakerAdvanced() { props = new SealToggle[]{new SealToggle(true,"pmeta","golem.prop.meta"),new SealToggle(false,"psilk","golem.prop.silk")}; }
    @Override public String getKey() { return "thaumcraft:breaker_advanced"; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_breaker_advanced"); }
    @Override public int getFilterSize() { return 9; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.BREAKER,EnumGolemTrait.SMART}; }
}
