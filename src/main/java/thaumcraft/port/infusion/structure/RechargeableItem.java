package thaumcraft.port.infusion.structure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** TC6 IRechargable contract; charge is vis stored on the individual item stack. */
public interface RechargeableItem {
    int maxCharge(ItemStack stack, LivingEntity wearer);
    default ChargeDisplay chargeDisplay(ItemStack stack, LivingEntity wearer) { return ChargeDisplay.NORMAL; }
    enum ChargeDisplay { NEVER, NORMAL, PERIODIC }
}
