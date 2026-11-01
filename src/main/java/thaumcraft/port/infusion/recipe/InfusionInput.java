package thaumcraft.port.infusion.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** One central pedestal and one stack from every occupied surrounding pedestal. */
public record InfusionInput(ItemStack central,List<ItemStack> components) implements RecipeInput {
    public InfusionInput { components=List.copyOf(components); }
    @Override public ItemStack getItem(int index){return index==0?central:components.get(index-1);}
    @Override public int size(){return 1+components.size();}
}
