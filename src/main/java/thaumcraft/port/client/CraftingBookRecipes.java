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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.research.ResearchCatalog;

/** The book displays the same synchronized recipes that the two crafting machines execute. */
public final class CraftingBookRecipes {
    private static final Map<String,ResourceLocation> IDS=ids();
    private static final Map<JsonObject,ResourceLocation> SOURCE_IDS=sources();
    private static final Map<JsonObject,Cached> CACHE=new IdentityHashMap<>();
    private static RecipeManager manager;
    private record Cached(Recipe<?> recipe,JsonObject display) {}

    public static JsonObject live(String key,JsonObject original){
        var mc=Minecraft.getInstance();if(mc.level==null)return original;
        String type=original.has("type")?original.get("type").getAsString():"";
        if(!type.contains("arcane")&&!type.contains("crucible"))return original;
        var recipes=mc.level.getRecipeManager();
        if(manager!=recipes){manager=recipes;CACHE.clear();}
        // Grouped pages share their tab key; their individual source objects identify the variant.
        var id=SOURCE_IDS.get(original);
        if(id==null)id=IDS.get(normalize(key));
        if(id==null)id=ResourceLocation.tryParse(key.toLowerCase(Locale.ROOT));
        if(id==null)return original;
        var holder=recipes.byKey(id).orElse(null);if(holder==null)return original;
        var recipe=holder.value();var cached=CACHE.get(original);
        if(cached!=null&&cached.recipe()==recipe)return cached.display();
        JsonObject display;
        if(recipe instanceof CrucibleRecipe crucible)display=crucible(original,crucible);
        else if(recipe instanceof ArcaneRecipe arcane)display=arcane(original,arcane);
        else return original;
        // A recipe sync/reload replaces the recipe instance, invalidating this entry automatically.
        CACHE.put(original,new Cached(recipe,display));return display;
    }

    private static Map<String,ResourceLocation> ids(){
        var ids=new HashMap<String,ResourceLocation>();
        var mapping=ResearchCatalog.read("/thaumcraft-port/crafting-recipe-coverage.json").getAsJsonObject("recipe_mapping");
        for(var entry:mapping.entrySet()){
            var id=ResourceLocation.parse(entry.getValue().getAsString());
            ids.put(normalize(entry.getKey()),id);ids.put(normalize(id.toString()),id);
        }
        return Map.copyOf(ids);
    }

    private static Map<JsonObject,ResourceLocation> sources(){
        var sources=new HashMap<JsonObject,ResourceLocation>();
        var catalog=ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        for(var entry:catalog.entrySet()){
            var id=IDS.get(normalize(entry.getKey()));
            if(id!=null)sources.put(entry.getValue().getAsJsonObject(),id);
        }
        return sources;
    }

    private static String normalize(String key){return key.toLowerCase(Locale.ROOT).replace("_","");}

    private static JsonObject crucible(JsonObject original,CrucibleRecipe recipe){
        var display=original.deepCopy();display.addProperty("type","thaumcraft:crucible");
        display.addProperty("research",recipe.research());display.add("result",legacy(recipe.result()));
        display.add("catalyst",ingredient(recipe.catalyst(),original.get("catalyst")));
        display.add("aspects",amounts(recipe.aspects()));return display;
    }

    private static JsonObject arcane(JsonObject original,ArcaneRecipe recipe){
        var display=original.deepCopy();display.addProperty("type","thaumcraft:arcane");
        display.addProperty("research",recipe.research());display.addProperty("vis",recipe.vis());
        display.add("crystals",amounts(recipe.crystals()));display.add("result",legacy(recipe.result()));
        display.remove("pattern");display.remove("key");display.remove("ingredient");display.remove("ingredients");
        var ingredients=recipe.pattern().ingredients();
        if(recipe.shapeless()){
            var entries=new JsonArray();int slot=0;
            for(var value:ingredients)if(!value.isEmpty())entries.add(ingredient(value,fallback(original,slot++)));
            display.add("ingredients",entries);
        }else{
            var pattern=new JsonArray();var keys=new JsonObject();int width=recipe.pattern().width();
            for(int y=0;y<recipe.pattern().height();y++){
                var row=new StringBuilder();
                for(int x=0;x<width;x++){
                    int slot=y*width+x;var value=ingredients.get(slot);
                    if(value.isEmpty()){row.append(' ');continue;}
                    char symbol=(char)('A'+slot);row.append(symbol);
                    keys.add(String.valueOf(symbol),ingredient(value,fallback(original,slot)));
                }
                pattern.add(row.toString());
            }
            display.add("pattern",pattern);display.add("key",keys);
        }
        return display;
    }

    private static JsonObject amounts(Map<String,Integer> amounts){
        var json=new JsonObject();amounts.forEach(json::addProperty);return json;
    }

    private static JsonElement ingredient(Ingredient ingredient,JsonElement fallback){
        var options=new JsonArray();for(var stack:thaumcraft.port.crafting.IngredientMatching.getItems(ingredient))if(!stack.isEmpty())options.add(legacy(stack));
        // Optional ore tags can be empty when the supplying mod is absent.
        if(options.isEmpty())return fallback==null?new JsonPrimitive("minecraft:air"):fallback.deepCopy();
        return options.size()==1?options.get(0):options;
    }

    private static JsonElement fallback(JsonObject original,int slot){
        if(original.has("ingredients")){
            var ingredients=original.getAsJsonArray("ingredients");return slot<ingredients.size()?ingredients.get(slot):null;
        }
        if(!original.has("pattern")||!original.has("key"))return original.get("ingredient");
        var keys=original.getAsJsonObject("key");
        for(var row:original.getAsJsonArray("pattern")){
            String line=row.getAsString();if(slot<line.length())return keys.get(String.valueOf(line.charAt(slot)));slot-=line.length();
        }
        return null;
    }

    private static JsonObject legacy(ItemStack stack){
        var entry=new JsonObject();entry.addProperty("legacy",source(stack));return entry;
    }

    private static String source(ItemStack stack){
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var custom=stack.get(DataComponents.CUSTOM_DATA);var tag=custom==null?new CompoundTag():custom.copyTag();
        String aspect=stack.get(PortContent.CRYSTAL_ASPECT.get());int meta=0,amount=1;
        if(aspect==null){aspect=stack.get(EssentiaContent.ASPECT.get());if(aspect!=null){meta=1;amount=stack.is(EssentiaContent.PHIAL.get())?10:1;}}
        if(aspect!=null){
            var value=new CompoundTag();value.putString("key",aspect);value.putInt("amount",amount);
            var aspects=new ListTag();aspects.add(value);tag.put("Aspects",aspects);
        }
        var potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion!=null&&potion.potion().isPresent())tag.putString("Potion",potion.potion().get().unwrapKey().orElseThrow().location().toString());
        String base=id+";"+stack.getCount()+";"+meta;return tag.isEmpty()?base:base+";"+tag;
    }

    private CraftingBookRecipes(){}
}
