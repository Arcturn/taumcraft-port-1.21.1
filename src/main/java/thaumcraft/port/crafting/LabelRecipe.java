package thaumcraft.port.crafting;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.essentia.*;

/** Copy a phial's aspect onto a blank label; original CraftingEvents returns the filled phial. */
public final class LabelRecipe extends CustomRecipe {
    public LabelRecipe(CraftingBookCategory category){super(category);}
    @Override public boolean matches(CraftingInput input,Level level){
        int labels=0,phials=0,filledLabels=0;
        for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;
            if(s.is(EssentiaContent.LABEL.get())){labels++;if(EssentiaItem.aspect(s)!=null)filledLabels++;}
            else if(s.is(EssentiaContent.PHIAL.get())&&EssentiaItem.aspect(s)!=null)phials++;else return false;
        }
        return labels==1&&((filledLabels==0&&phials==1)||(filledLabels==1&&phials==0));
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){var output=new ItemStack(EssentiaContent.LABEL.get());for(int i=0;i<input.size();i++)if(input.getItem(i).is(EssentiaContent.PHIAL.get()))output.set(EssentiaContent.ASPECT.get(),EssentiaItem.aspect(input.getItem(i)).id());return output;}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);for(int i=0;i<input.size();i++)if(input.getItem(i).is(EssentiaContent.PHIAL.get()))result.set(i,input.getItem(i).copyWithCount(1));return result;}
    @Override public boolean canCraftInDimensions(int x,int y){return x*y>=1;}
    @Override public RecipeSerializer<?> getSerializer(){return EssentiaContent.LABEL_RECIPE.get();}
}
