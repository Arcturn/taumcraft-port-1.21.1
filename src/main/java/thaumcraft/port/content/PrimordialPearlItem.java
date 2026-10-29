package thaumcraft.port.content;

import net.minecraft.world.item.*;

/** TC6 leaves a more worn pearl after each craft, until its eighth use. */
public final class PrimordialPearlItem extends Item {
    public PrimordialPearlItem(Properties p){super(p);}
    public boolean hasCraftingRemainingItem(ItemStack stack){return stack.getDamageValue()<7;}
    public ItemStack getCraftingRemainingItem(ItemStack stack){
        if(!hasCraftingRemainingItem(stack))return ItemStack.EMPTY;
        var result=stack.copyWithCount(1);result.setDamageValue(stack.getDamageValue()+1);return result;
    }
    public boolean isRepairable(ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return false;}
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public boolean isFoil(ItemStack stack){return false;}
}
