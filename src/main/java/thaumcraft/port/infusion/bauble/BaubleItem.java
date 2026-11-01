package thaumcraft.port.infusion.bauble;

import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class BaubleItem extends Item implements ICurioItem {
    public BaubleItem(Properties properties){super(properties.stacksTo(1));}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return true;}
}
