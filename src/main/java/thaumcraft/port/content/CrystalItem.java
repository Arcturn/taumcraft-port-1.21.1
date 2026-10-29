package thaumcraft.port.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.port.aspect.Aspect;

public final class CrystalItem extends Item {
    public CrystalItem(Properties properties) {
        super(properties.component(PortContent.CRYSTAL_ASPECT.get(), "aer"));
    }

    public static Aspect aspect(ItemStack stack) {
        return Aspect.byId(stack.getOrDefault(PortContent.CRYSTAL_ASPECT.get(), "aer")).orElse(Aspect.AIR);
    }

    public static ItemStack stack(Aspect aspect) {
        ItemStack stack = new ItemStack(PortContent.CRYSTAL_ESSENCE.get());
        stack.set(PortContent.CRYSTAL_ASPECT.get(), aspect.id());
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Aspect aspect = aspect(stack);
        lines.add(Component.literal(aspect.displayName())
                .withStyle(style -> style.withColor(aspect.color())));
    }
}
