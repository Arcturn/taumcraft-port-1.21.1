package thaumcraft.port.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.world.WorldEntities;
import top.theillusivec4.curios.api.CuriosApi;

/** Shared TC6 EntityUtils/CasterManager rules; cosmetic Curios never grant effects. */
public final class RevealingEquipment {
    private static boolean goggles(ItemStack stack) { return stack.getItem() instanceof GogglesItem; }

    /** TC6 checks the main hand for popup goggles, but deliberately not the off hand. */
    public static boolean hasGoggles(Player player) {
        return player != null && (goggles(player.getMainHandItem()) || wornCount(player) > 0);
    }

    /** The legacy IRevealer contract also accepts the off hand. */
    public static boolean hasRevealer(Player player) {
        return player != null && (hasGoggles(player) || goggles(player.getOffhandItem()));
    }

    private static int wornCount(Player player) {
        int count = 0;
        for (ItemStack stack : player.getArmorSlots()) if (goggles(stack)) count++;
        return count + CuriosApi.getCuriosInventory(player).map(inventory -> {
            int curios = 0;
            for (var handler : inventory.getCurios().values()) {
                var stacks = handler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++)
                    if (inventory.isSlotActive(handler.getIdentifier(), slot) && goggles(stacks.getStackInSlot(slot))) curios++;
            }
            return curios;
        }).orElse(0);
    }

    /** Fraction of vis saved, including the original exhaustion penalty (which can be negative). */
    public static float visDiscount(Player player) {
        if (player == null) return 0;
        int discount = wornCount(player) * GogglesItem.VIS_DISCOUNT;
        var ordinary = player.getEffect(WorldEntities.VIS_EXHAUST);
        var infectious = player.getEffect(WorldEntities.INFECTIOUS);
        if (ordinary != null || infectious != null) {
            int amplifier = Math.max(ordinary == null ? 0 : ordinary.getAmplifier(),
                    infectious == null ? 0 : infectious.getAmplifier());
            discount -= (amplifier + 1) * 10;
        }
        return discount / 100.0f;
    }

    /** Original arcane workbench truncation, e.g. 50 vis with goggles costs 47. */
    public static int discountedVis(int baseVis, Player player) {
        return (int)(baseVis * (1.0f - visDiscount(player)));
    }

    private RevealingEquipment() {}
}
