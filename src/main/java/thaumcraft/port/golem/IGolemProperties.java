package thaumcraft.port.golem;

import java.util.Set;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.part.GolemAddon;
import thaumcraft.port.golem.part.GolemArm;
import thaumcraft.port.golem.part.GolemHead;
import thaumcraft.port.golem.part.GolemLeg;
import thaumcraft.port.golem.part.GolemMaterial;

public interface IGolemProperties {
    public Set<EnumGolemTrait> getTraits();

    public boolean hasTrait(EnumGolemTrait var1);

    public long toLong();

    public ItemStack[] generateComponents();

    public void setMaterial(GolemMaterial var1);

    public GolemMaterial getMaterial();

    public void setHead(GolemHead var1);

    public GolemHead getHead();

    public void setArms(GolemArm var1);

    public GolemArm getArms();

    public void setLegs(GolemLeg var1);

    public GolemLeg getLegs();

    public void setAddon(GolemAddon var1);

    public GolemAddon getAddon();

    public void setRank(int var1);

    public int getRank();
}
