package thaumcraft.port.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** All supported dust conversions share the original two-stage ritual. */
public final class SalisMundusItem extends Item {
    public SalisMundusItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Crafting tables otherwise open their vanilla menu before Item.useOn is reached.
        return SalisMundusRituals.start(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return SalisMundusRituals.start(context);
    }
}
