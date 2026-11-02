package thaumcraft.port.golem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
public enum EnumGolemTrait {
    SMART, DEFT, CLUMSY, FIGHTER, WHEELED, FLYER, CLIMBER, HEAVY, LIGHT, FRAGILE, REPAIR, SCOUT, ARMORED, BRUTAL, FIREPROOF, BREAKER, HAULER, RANGED, BLASTPROOF;
    public final ResourceLocation icon=ResourceLocation.parse("thaumcraft:textures/misc/golem/tag_"+name().toLowerCase(java.util.Locale.ROOT)+".png");
    public EnumGolemTrait opposite;
    public String getLocalizedName(){return Component.translatable("golem.trait."+name().toLowerCase(java.util.Locale.ROOT)).getString();}
    public String getLocalizedDescription(){return Component.translatable("golem.trait.text."+name().toLowerCase(java.util.Locale.ROOT)).getString();}
    static{CLUMSY.opposite=DEFT;DEFT.opposite=CLUMSY;HEAVY.opposite=LIGHT;LIGHT.opposite=HEAVY;FRAGILE.opposite=ARMORED;ARMORED.opposite=FRAGILE;}
}
