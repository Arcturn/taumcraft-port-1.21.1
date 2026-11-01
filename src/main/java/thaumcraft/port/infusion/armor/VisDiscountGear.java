package thaumcraft.port.infusion.armor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/** Percentage points, as in TC6 IVisDiscountGear. */
public interface VisDiscountGear { int visDiscount(ItemStack stack, Player player); }
