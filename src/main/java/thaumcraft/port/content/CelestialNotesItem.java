package thaumcraft.port.content;
import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
public final class CelestialNotesItem extends Item {
    public CelestialNotesItem(Properties p){super(p);}
    public static ItemStack stack(int kind){var s=new ItemStack(PortContent.CELESTIAL_NOTES.get());s.set(PortContent.CELESTIAL_KIND.get(),Math.clamp(kind,0,12));return s;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){int k=s.getOrDefault(PortContent.CELESTIAL_KIND.get(),0);lines.add(Component.translatable("item.celestial_notes."+(k==0?"sun":k<5?"stars_"+k:"moon_"+(k-4))+".text"));}
}
