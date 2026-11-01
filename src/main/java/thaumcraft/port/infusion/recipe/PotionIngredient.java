package thaumcraft.port.infusion.recipe;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** TC6's relaxed Potion NBT predicate allows additional custom effects and color. */
public record PotionIngredient(Holder<Potion> potion) implements ICustomIngredient {
    public static final MapCodec<PotionIngredient> CODEC=Potion.CODEC.fieldOf("potion").xmap(PotionIngredient::new,PotionIngredient::potion);
    @Override public boolean test(ItemStack stack){
        return stack.is(Items.POTION)&&stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().filter(p->p.is(potion)).isPresent();
    }
    @Override public Stream<ItemStack> getItems(){return Stream.of(PotionContents.createItemStack(Items.POTION,potion));}
    @Override public boolean isSimple(){return false;}
    @Override public IngredientType<?> getType(){return InfusionRecipes.POTION_INGREDIENT.get();}
}
