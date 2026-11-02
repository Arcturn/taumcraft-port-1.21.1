package thaumcraft.port.artifice.furnace;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Optional metals resolve their output from the active common ingot tag, like BETA26's ore dictionary. */
public final class NuggetCompressionRecipe extends CustomRecipe {
    public NuggetCompressionRecipe(CraftingBookCategory category){super(category);}
    private ItemStack result(CraftingInput input){
        if(input.width()!=3||input.height()!=3)return ItemStack.EMPTY;
        for(var entry:InfernalFurnaceContent.NUGGETS.entrySet()){
            boolean matches=true;for(int n=0;n<9;n++)if(!input.getItem(n).is(entry.getValue().get())){matches=false;break;}
            if(!matches)continue;
            var tag=BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","ingots/"+entry.getKey())));
            if(tag.isPresent()&&tag.get().size()>0)return new ItemStack(tag.get().get(0));
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput input,Level level){return !result(input).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider lookup){return result(input);}
    @Override public boolean canCraftInDimensions(int width,int height){return width>=3&&height>=3;}
    @Override public RecipeSerializer<?> getSerializer(){return InfernalFurnaceContent.COMPRESSION.get();}
}
