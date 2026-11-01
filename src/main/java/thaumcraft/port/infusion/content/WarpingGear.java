package thaumcraft.port.infusion.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Intrinsic TC6 equipment warp; separate from the additional TC.WARP augment. */
public interface WarpingGear {
    int gearWarp(ItemStack stack, Player player);
}
