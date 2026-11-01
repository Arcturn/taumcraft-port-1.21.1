package thaumcraft.port.infusion.recipe;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.effect.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;

/** Original foods used by the sustenance charm; nuggets are Infernal Furnace bonus outputs. */
public final class InfusionFood {
    public static final List<String> KINDS=List.of("beef","chicken","pork","fish","rabbit","mutton");
    public static final Map<String,DeferredItem<MeatNuggetItem>> NUGGETS=new LinkedHashMap<>();
    public static final DeferredItem<Item> TRIPLE_MEAT_TREAT=PortContent.ITEMS.registerSimpleItem("triple_meat_treat",new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(6).saturationModifier(.8f).alwaysEdible().effect(()->new MobEffectInstance(MobEffects.REGENERATION,100,0),.66f).build()));
    public static final Supplier<RecipeSerializer<TripleMeatTreatRecipe>> SERIALIZER=PortContent.SERIALIZERS.register("triple_meat_treat",()->new SimpleCraftingRecipeSerializer<>(TripleMeatTreatRecipe::new));
    static {for(String kind:KINDS)NUGGETS.put(kind,PortContent.ITEMS.registerItem("chunk_"+kind,p->new MeatNuggetItem(kind,p.food(new FoodProperties(1,.6f,false,.5f,Optional.empty(),List.of())))));}
    public static final class MeatNuggetItem extends Item {
        private final String kind;
        private MeatNuggetItem(String kind,Properties properties){super(properties);this.kind=kind;}
        public String kind(){return kind;}
    }
    /** ConfigRecipes' raw-input bonus map; the Infernal Furnace rolls this at 0.33 per bellows pass. */
    public static final float SMELTING_BONUS_CHANCE=.33f;
    public static ItemStack smeltingBonus(ItemStack raw){
        String kind=raw.is(Items.BEEF)?"beef":raw.is(Items.CHICKEN)?"chicken":raw.is(Items.PORKCHOP)?"pork":raw.is(Items.COD)||raw.is(Items.SALMON)||raw.is(Items.TROPICAL_FISH)||raw.is(Items.PUFFERFISH)?"fish":raw.is(Items.RABBIT)?"rabbit":raw.is(Items.MUTTON)?"mutton":null;
        return kind==null?ItemStack.EMPTY:new ItemStack(NUGGETS.get(kind).get());
    }
    public static void register(){}
    private InfusionFood(){}
}
