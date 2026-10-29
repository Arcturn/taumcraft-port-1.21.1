package thaumcraft.port.crafting;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.machine.MachineContent;

public record CrucibleRecipe(String research,Ingredient catalyst,Map<String,Integer> aspects,ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final Codec<String> ASPECT=Codec.STRING.validate(s->Aspect.byId(s).isPresent()?DataResult.success(s):DataResult.error(()->"Unknown aspect: "+s));
    public static final MapCodec<CrucibleRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.optionalFieldOf("research","").forGetter(CrucibleRecipe::research),
        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
        Codec.unboundedMap(ASPECT,Codec.intRange(1,500)).fieldOf("aspects").forGetter(CrucibleRecipe::aspects),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrucibleRecipe::result)).apply(i,CrucibleRecipe::new));
    public CrucibleRecipe{aspects=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(aspects));result=result.copy();}
    public boolean matches(SingleRecipeInput input,Level level){return catalyst.test(input.item());}
    public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return result.copy();}
    public ItemStack getResultItem(HolderLookup.Provider registries){return result;}
    public boolean canCraftInDimensions(int x,int y){return x*y>=1;}
    public RecipeSerializer<?> getSerializer(){return MachineContent.CRUCIBLE_SERIALIZER.get();}
    public RecipeType<?> getType(){return MachineContent.CRUCIBLE_RECIPE.get();}
    public static final class Serializer implements RecipeSerializer<CrucibleRecipe> {
        public MapCodec<CrucibleRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,CrucibleRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
    }
}
