package thaumcraft.port.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.IngredientMatching;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.research.ResearchCatalog;

/** Use synchronized recipe costs/results while retaining TC6's illustrative dynamic central inputs. */
public final class InfusionBookRecipes {
    private static final Map<String,ResourceLocation> IDS=ids();
    private static final Map<JsonObject,ResourceLocation> SOURCE_IDS=sources();
    private static final Map<JsonObject,Cached> CACHE=new IdentityHashMap<>();
    private static RecipeManager manager;
    private record Cached(InfusionRecipe recipe,JsonObject display) {}

    public static JsonObject live(String key,JsonObject original){
        var mc=Minecraft.getInstance();if(mc.level==null||!original.has("type")||!original.get("type").getAsString().contains("infusion"))return original;
        var recipes=mc.level.getRecipeManager();if(manager!=recipes){manager=recipes;CACHE.clear();}
        // Resolve each captured variant before a shared recipe tab/group key.
        var id=SOURCE_IDS.get(original);if(id==null)id=IDS.get(normalize(key));
        if(id==null)return original;
        var holder=recipes.byKey(id).orElse(null);
        if(holder==null||!(holder.value() instanceof InfusionRecipe recipe))return original;
        var cached=CACHE.get(original);if(cached!=null&&cached.recipe()==recipe)return cached.display();
        ItemStack central=central(original);
        // Generic runic/enchantment recipes need the original illustrative equipment.
        // Concrete inputs instead follow the synchronized predicate after a data-pack change.
        if(!recipe.input().isEmpty()&&!IngredientMatching.test(recipe.input(),central)){
            var choices=IngredientMatching.getItems(recipe.input());central=choices.length==0?ItemStack.EMPTY:choices[0].copy();
        }
        if(central.isEmpty())return original;
        if(original.has("runic_before"))InfusionAugments.setRunic(central,original.get("runic_before").getAsInt());
        var r=original.deepCopy();r.addProperty("live_infusion",true);r.addProperty("research",recipe.research());
        var ingredients=new JsonArray();var stacks=new ArrayList<ItemStack>();int position=0;
        for(var ingredient:recipe.components(central)){
            var choices=IngredientMatching.getItems(ingredient);var options=new JsonArray();for(var stack:choices)options.add(legacy(stack));
            if(options.isEmpty()){
                var old=original.getAsJsonArray("ingredients");
                ingredients.add(old!=null&&position<old.size()?old.get(position).deepCopy():new JsonPrimitive("minecraft:air"));
                stacks.add(ItemStack.EMPTY);
            }else{ingredients.add(options.size()==1?options.get(0):options);stacks.add(choices[0].copy());}
            position++;
        }
        r.add("ingredients",ingredients);var aspects=new JsonObject();recipe.aspects(null,central,stacks).forEach((aspect,amount)->aspects.addProperty(aspect.id(),amount));r.add("aspects",aspects);
        r.addProperty("instability",recipe.instability(null,central,stacks));r.add("input",legacy(central));
        var result=recipe.assemble(new InfusionInput(central,stacks),mc.level.registryAccess());if(!result.isEmpty())r.add("result",legacy(result));
        // Recipe synchronization replaces the recipe object and invalidates this cached preview.
        CACHE.put(original,new Cached(recipe,r));return r;
    }
    private static Map<String,ResourceLocation> ids(){
        var result=new HashMap<String,ResourceLocation>();
        var coverage=ResearchCatalog.read("/thaumcraft-port/infusion-crafting-coverage.json");
        for(String section:List.of("recipe_mapping","fake_recipe_mapping"))for(var entry:coverage.getAsJsonObject(section).entrySet()){
            var id=ResourceLocation.parse(entry.getValue().getAsString());
            result.put(normalize(entry.getKey()),id);result.put(normalize(id.toString()),id);
        }
        return Map.copyOf(result);
    }
    private static Map<JsonObject,ResourceLocation> sources(){
        var result=new HashMap<JsonObject,ResourceLocation>();
        for(var entry:ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes").entrySet()){
            var id=IDS.get(normalize(entry.getKey()));if(id!=null)result.put(entry.getValue().getAsJsonObject(),id);
        }
        return Map.copyOf(result);
    }
    private static String normalize(String key){return key.toLowerCase(Locale.ROOT).replace("_","");}
    private static ItemStack central(JsonObject original){
        var input=original.get("input");if(input==null)return ItemStack.EMPTY;
        if(input.isJsonPrimitive())return BookAssets.stack(input.getAsString());
        if(!input.isJsonObject())return ItemStack.EMPTY;
        var object=input.getAsJsonObject();if(object.has("legacy"))return BookAssets.stack(object.get("legacy").getAsString());
        String property=object.has("id")?"id":"item";if(!object.has(property))return ItemStack.EMPTY;
        return BookAssets.stack(object.get(property).getAsString()+";"+(object.has("count")?object.get("count").getAsInt():1)+";0");
    }
    private static JsonObject legacy(ItemStack stack){var entry=new JsonObject();entry.addProperty("legacy",source(stack));return entry;}
    private static String source(ItemStack stack){
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var custom=stack.get(DataComponents.CUSTOM_DATA);var tag=custom==null?new CompoundTag():custom.copyTag();
        // TC6's found/crafted amulets share an ID but have distinct labels and models.
        String aspect=stack.get(PortContent.CRYSTAL_ASPECT.get());int meta=id.equals("thaumcraft:amulet_vis")?1:0,amount=1;
        if(aspect==null){aspect=stack.get(EssentiaContent.ASPECT.get());if(aspect!=null){meta=1;amount=stack.is(EssentiaContent.PHIAL.get())?10:1;}}
        if(aspect!=null){
            var value=new CompoundTag();value.putString("key",aspect);value.putInt("amount",amount);
            var aspects=new ListTag();aspects.add(value);tag.put("Aspects",aspects);
        }
        var potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion!=null&&potion.potion().isPresent())tag.putString("Potion",potion.potion().get().unwrapKey().orElseThrow().location().toString());
        String base=id+";"+stack.getCount()+";"+meta;return tag.isEmpty()?base:base+";"+tag;
    }
    private InfusionBookRecipes(){}
}
