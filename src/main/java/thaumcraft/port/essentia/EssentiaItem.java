package thaumcraft.port.essentia;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumcraft.port.aspect.Aspect;

public final class EssentiaItem extends Item {
    public EssentiaItem(Properties p){super(p);}
    public static Aspect aspect(ItemStack stack){return Aspect.byId(stack.getOrDefault(EssentiaContent.ASPECT.get(),"")).orElse(null);}
    public static ItemStack phial(Aspect a){var stack=new ItemStack(EssentiaContent.PHIAL.get());if(a!=null)stack.set(EssentiaContent.ASPECT.get(),a.id());return stack;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> lines,TooltipFlag flag){
        Aspect a=aspect(stack);if(a!=null)lines.add(Component.literal(a.displayName()).append(stack.is(EssentiaContent.PHIAL.get())?" × 10":"").withStyle(s->s.withColor(a.color())));
    }
}
