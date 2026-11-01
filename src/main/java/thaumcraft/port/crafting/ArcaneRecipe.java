package thaumcraft.port.crafting;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.machine.MachineContent;

/** Data-pack recipe; actual payment and research checks belong to the workbench transaction. */
public record ArcaneRecipe(String research,int vis,Map<String,Integer> crystals,ShapedRecipePattern pattern,ItemStack result,boolean shapeless,int sourceOrder) implements Recipe<CraftingInput> {
    private static final Codec<String> PRIMAL=Codec.STRING.validate(s->java.util.Set.of("aer","terra","ignis","aqua","ordo","perditio").contains(s)?DataResult.success(s):DataResult.error(()->"Not a primal aspect: "+s));
    public static final MapCodec<ArcaneRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.optionalFieldOf("research","").forGetter(ArcaneRecipe::research),
        Codec.intRange(0,10000).fieldOf("vis").forGetter(ArcaneRecipe::vis),
        Codec.unboundedMap(PRIMAL,Codec.intRange(1,64)).optionalFieldOf("crystals",Map.of()).forGetter(ArcaneRecipe::crystals),
        ShapedRecipePattern.MAP_CODEC.forGetter(ArcaneRecipe::pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ArcaneRecipe::result),
        Codec.BOOL.optionalFieldOf("shapeless",false).forGetter(ArcaneRecipe::shapeless),
        Codec.INT.optionalFieldOf("source_order",Integer.MAX_VALUE).forGetter(ArcaneRecipe::sourceOrder)).apply(i,ArcaneRecipe::new));
    public ArcaneRecipe(String research,int vis,Map<String,Integer> crystals,ShapedRecipePattern pattern,ItemStack result,boolean shapeless){this(research,vis,crystals,pattern,result,shapeless,Integer.MAX_VALUE);}
    public ArcaneRecipe {crystals=Map.copyOf(crystals);result=result.copy();}
    @Override public boolean matches(CraftingInput input,Level level){
        if(result.is(thaumcraft.port.essentia.EssentiaContent.TUBES.get("tube_buffer").get().asItem()))for(int i=0;i<input.size();i++)if(input.getItem(i).is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())&&thaumcraft.port.essentia.EssentiaItem.aspect(input.getItem(i))!=null)return false;
        if(!shapeless)return input.width()==pattern.width()&&input.height()==pattern.height()&&(matchesPattern(input,false)||matchesPattern(input,true));
        var ingredients=pattern.ingredients().stream().filter(i->!i.isEmpty()).toList();
        var stacks=new java.util.ArrayList<ItemStack>();for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty())stacks.add(input.getItem(i));
        return stacks.size()==ingredients.size()&&matchUnordered(ingredients,stacks,0,0);
    }
    private boolean matchesPattern(CraftingInput input,boolean mirror){
        for(int y=0;y<pattern.height();y++)for(int x=0;x<pattern.width();x++){
            var ingredient=pattern.ingredients().get((mirror?pattern.width()-1-x:x)+y*pattern.width());
            if(!IngredientMatching.test(ingredient,input.getItem(x+y*input.width())))return false;
        }
        return true;
    }
    private static boolean matchUnordered(java.util.List<Ingredient> ingredients,java.util.List<ItemStack> stacks,int index,int used){
        if(index==ingredients.size())return true;
        for(int i=0;i<stacks.size();i++)if((used&(1<<i))==0&&IngredientMatching.test(ingredients.get(index),stacks.get(i))&&matchUnordered(ingredients,stacks,index+1,used|(1<<i)))return true;return false;
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){
        var output=result.copy();
        if(output.is(thaumcraft.port.essentia.EssentiaContent.VOID_JAR.get().asItem()))for(int i=0;i<input.size();i++){
            // ShapedArcaneVoidJar copied the source jar's entire NBT, including its name.
            // Copy the component patch rather than the normal jar's item defaults.
            var stack=input.getItem(i);if(stack.is(thaumcraft.port.essentia.EssentiaContent.JAR.get().asItem()))return stack.transmuteCopy(output.getItem(),output.getCount());
        }
        return output;
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return result;}
    @Override public boolean canCraftInDimensions(int x,int y){return shapeless?x*y>=pattern.ingredients().stream().filter(i->!i.isEmpty()).count():x>=pattern.width()&&y>=pattern.height();}
    @Override public NonNullList<Ingredient> getIngredients(){return pattern.ingredients();}
    @Override public RecipeSerializer<?> getSerializer(){return MachineContent.ARCANE_SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return MachineContent.ARCANE_RECIPE.get();}
    public static final class Serializer implements RecipeSerializer<ArcaneRecipe> {
        public MapCodec<ArcaneRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,ArcaneRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
    }
}
