package thaumcraft.port.auromancy.focus;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Original caster-facing extension contract, independent of either player's physical hand. */
public interface ICaster {
    float getConsumptionModifier(ItemStack stack, Player player, boolean crafting);
    boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate);
    Item getFocus(ItemStack stack);
    ItemStack getFocusStack(ItemStack stack);
    void setFocus(ItemStack stack, ItemStack focus);
    ItemStack getPickedBlock(ItemStack stack);
}
