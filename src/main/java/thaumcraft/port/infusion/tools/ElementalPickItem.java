package thaumcraft.port.infusion.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;

public final class ElementalPickItem extends PickaxeItem {
    public ElementalPickItem(Properties properties) { super(InfusionTools.ELEMENTAL,properties); }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity target) {
        if (!player.level().isClientSide && (!(target instanceof Player other) || player.canHarmPlayer(other)))
            target.igniteForSeconds(2);
        return false;
    }
}
