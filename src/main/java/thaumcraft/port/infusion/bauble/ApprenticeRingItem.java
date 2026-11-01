package thaumcraft.port.infusion.bauble;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.infusion.armor.VisDiscountGear;

public final class ApprenticeRingItem extends BaubleItem implements VisDiscountGear {
    public ApprenticeRingItem(Properties properties){super(properties);}
    @Override public int visDiscount(ItemStack stack,Player player){return 5;}
}
