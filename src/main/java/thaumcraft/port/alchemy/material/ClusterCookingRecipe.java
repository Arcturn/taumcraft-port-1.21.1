package thaumcraft.port.alchemy.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.PortContent;

/** Optional metals resolve the installed mod's ingot. Each implementation retains vanilla's recipe-type contract. */
public interface ClusterCookingRecipe extends Recipe<SingleRecipeInput> {
    Supplier<RecipeSerializer<ClusterCookingRecipe>> SERIALIZER = PortContent.SERIALIZERS.register("cluster_cooking",Serializer::new);
    MapCodec<ClusterCookingRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ClusterCookingRecipe::input),
            ResourceLocation.CODEC.fieldOf("result_tag").forGetter(ClusterCookingRecipe::outputTag),
            Codec.BOOL.optionalFieldOf("blasting",false).forGetter(ClusterCookingRecipe::blasting)
    ).apply(i,(input,tag,blast)->blast?new Blasting(input,tag):new Smelting(input,tag)));
    Ingredient input();
    ResourceLocation outputTag();
    boolean blasting();
    static void register() { SERIALIZER.getClass(); }
    default ItemStack output() {
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,outputTag()))
                .flatMap(s->s.stream().findFirst()).map(h->new ItemStack(h,2)).orElse(ItemStack.EMPTY);
    }
    final class Smelting extends SmeltingRecipe implements ClusterCookingRecipe {
        private final ResourceLocation outputTag;
        Smelting(Ingredient input,ResourceLocation tag) { super("",CookingBookCategory.MISC,input,ItemStack.EMPTY,1,200);outputTag=tag; }
        public Ingredient input(){return ingredient;}
        public ResourceLocation outputTag(){return outputTag;}
        public boolean blasting(){return false;}
        @Override public ItemStack getResultItem(HolderLookup.Provider registries){return output();}
        @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return output();}
        @Override public boolean matches(SingleRecipeInput input,Level level){return !output().isEmpty()&&super.matches(input,level);}
        @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    }
    final class Blasting extends BlastingRecipe implements ClusterCookingRecipe {
        private final ResourceLocation outputTag;
        Blasting(Ingredient input,ResourceLocation tag) { super("",CookingBookCategory.MISC,input,ItemStack.EMPTY,1,100);outputTag=tag; }
        public Ingredient input(){return ingredient;}
        public ResourceLocation outputTag(){return outputTag;}
        public boolean blasting(){return true;}
        @Override public ItemStack getResultItem(HolderLookup.Provider registries){return output();}
        @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return output();}
        @Override public boolean matches(SingleRecipeInput input,Level level){return !output().isEmpty()&&super.matches(input,level);}
        @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    }
    final class Serializer implements RecipeSerializer<ClusterCookingRecipe> {
        public MapCodec<ClusterCookingRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,ClusterCookingRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
    }
}
