package thaumcraft.port.crafting;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;

/** Original RecipeMagicDust: three distinct aspects, redstone, a bowl and reusable flint. */
public final class SalisMundusRecipe extends CustomRecipe {
    public SalisMundusRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean bowl = false, flint = false, redstone = false;
        Set<Aspect> crystals = new HashSet<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.BOWL) && !bowl) bowl = true;
            else if (stack.is(Items.FLINT) && !flint) flint = true;
            else if (stack.is(Items.REDSTONE) && !redstone) redstone = true;
            else if (stack.getItem() instanceof CrystalItem && crystals.size() < 3
                    && crystals.add(CrystalItem.aspect(stack))) { /* one crystal per aspect */ }
            else return false;
        }
        return bowl && flint && redstone && crystals.size() == 3;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(PortContent.SALIS_MUNDUS.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.is(Items.BOWL) || stack.is(Items.FLINT)) result.set(i, stack.copyWithCount(1));
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 6; }

    @Override
    public RecipeSerializer<?> getSerializer() { return PortContent.SALIS_RECIPE.get(); }
}
