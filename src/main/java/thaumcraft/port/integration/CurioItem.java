package thaumcraft.port.integration;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CurioItem extends Item implements ICurioItem {
    public CurioItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
}
