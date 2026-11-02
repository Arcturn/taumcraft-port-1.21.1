package thaumcraft.port.artifice.tool;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.research.ResearchProgress;

/** The source item displays its owner's three warp pools while held in either hand. */
public final class SanityCheckerItem extends Item {
    public SanityCheckerItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(entity instanceof ServerPlayer player&&player.tickCount%20==0&&(selected||player.getOffhandItem()==stack))ResearchProgress.sync(player);}
}
