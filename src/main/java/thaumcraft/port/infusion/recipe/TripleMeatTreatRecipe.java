package thaumcraft.port.infusion.recipe;

import java.util.HashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** BETA26 RecipeTripleMeatTreat: exactly one sugar and three distinct meat nugget kinds. */
public final class TripleMeatTreatRecipe extends CustomRecipe {
    public TripleMeatTreatRecipe(CraftingBookCategory category){super(category);}
    @Override public boolean matches(CraftingInput input,Level level){
        boolean sugar=false;var meats=new HashSet<String>();
        for(int n=0;n<input.size();n++){
            var stack=input.getItem(n);if(stack.isEmpty())continue;
            if(stack.is(Items.SUGAR)&&!sugar)sugar=true;
            else if(stack.getItem() instanceof InfusionFood.MeatNuggetItem meat&&meats.size()<3&&meats.add(meat.kind())){}
            else return false;
        }
        return sugar&&meats.size()==3;
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){return new ItemStack(InfusionFood.TRIPLE_MEAT_TREAT.get());}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return new ItemStack(InfusionFood.TRIPLE_MEAT_TREAT.get());}
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=4;}
    @Override public RecipeSerializer<?> getSerializer(){return InfusionFood.SERIALIZER.get();}
}
