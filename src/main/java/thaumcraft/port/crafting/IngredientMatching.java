package thaumcraft.port.crafting;

import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.*;

/** Empty-tag display barriers are not actual members of an ingredient tag. */
public final class IngredientMatching {
    public static boolean test(Ingredient ingredient,ItemStack stack) {
        if(stack==null)return false;
        if(ingredient.isEmpty())return stack.isEmpty();
        var custom=ingredient.getCustomIngredient();
        if(custom instanceof CompoundIngredient c)return c.children().stream().anyMatch(i->test(i,stack));
        if(custom instanceof IntersectionIngredient c)return c.children().stream().allMatch(i->test(i,stack));
        if(custom instanceof DifferenceIngredient c)return test(c.base(),stack)&&!test(c.subtracted(),stack);
        if(custom instanceof BlockTagIngredient c){
            for(var block:BuiltInRegistries.BLOCK.getTagOrEmpty(c.getTag()))if(!stack.isEmpty()&&stack.is(block.value().asItem()))return true;
            return false;
        }
        // Component predicates and other custom ingredients retain their own semantics.
        if(custom!=null)return ingredient.test(stack);
        for(var value:ingredient.getValues()){
            if(value instanceof Ingredient.TagValue tag&&stack.is(tag.tag()))return true;
            if(value instanceof Ingredient.ItemValue item&&stack.is(item.item().getItem()))return true;
        }
        return false;
    }
    /** Representative stacks suitable for recipe pages, without unavailable tag placeholders. */
    public static ItemStack[] getItems(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).filter(stack->test(ingredient,stack)).toArray(ItemStack[]::new);
    }
    private IngredientMatching(){}
}
