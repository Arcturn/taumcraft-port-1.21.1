package thaumcraft.port.research;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.UnaryOperator;

/** Presentation-only catalog lookup and the original per-recipe research gates. */
public final class BookRecipeCatalog {
    public record Reference(String recipe,int variant) {public boolean locked(){return variant==-99;}}
    private static final JsonObject RECIPES=ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
    private static final Map<String,String> ALIASES=Map.ofEntries(Map.entry("salismundusfake","salis_mundus"),Map.entry("nitor","crucible_nitor"),Map.entry("alumentum","crucible_alumentum"),Map.entry("wardedjar","warded_jar"),Map.entry("jarvoid","void_jar"),Map.entry("stonearcane","arcane_stone"),Map.entry("brickarcane","arcane_stone_bricks"),Map.entry("thaumatorium","thaumatorium"),Map.entry("smelter","smelter_basic"));
    private static String normalize(String s){return s.toLowerCase(Locale.ROOT).replace("_","");}
    public static List<JsonObject> recipes(String key){
        if(key==null)return List.of();
        if(RECIPES.has(key))return List.of(RECIPES.getAsJsonObject(key));
        String name=key.substring(key.indexOf(':')+1);
        for(var e:RECIPES.entrySet())if(e.getKey().equalsIgnoreCase(key))return List.of(e.getValue().getAsJsonObject());
        String wanted=normalize(ALIASES.getOrDefault(name.toLowerCase(Locale.ROOT),name));List<JsonObject> out=new ArrayList<>();
        for(var e:RECIPES.entrySet())if(normalize(e.getKey().substring(e.getKey().indexOf(':')+1)).equals(wanted)||e.getValue().getAsJsonObject().has("book_group")&&e.getValue().getAsJsonObject().get("book_group").getAsString().equalsIgnoreCase(key))out.add(e.getValue().getAsJsonObject());
        return List.copyOf(out);
    }
    public static boolean unlocked(JsonObject recipe,ResearchState state){return !recipe.has("research")||state.knows(recipe.get("research").getAsString());}
    public static List<JsonObject> recipes(String key,ResearchState state){return recipes(key).stream().filter(r->unlocked(r,state)).toList();}
    public static String output(JsonObject recipe){
        if(!recipe.has("result"))return "thaumcraft:textures/aspects/_unknown.png";
        var result=recipe.getAsJsonObject("result");
        if(result.has("legacy"))return result.get("legacy").getAsString();
        return result.get(result.has("id")?"id":"item").getAsString()+(result.has("count")?";"+result.get("count").getAsInt():"");
    }
    /** ItemStack.isItemEqual in 1.12.2 compares the item and damage, ignoring count and NBT. */
    public static String itemIdentity(String source){var parts=source.split(";",4);return parts[0]+";"+(parts.length>2?parts[2]:"0");}
    /** The index is in the filtered display list, so hidden variants cannot shift a drilldown. */
    public static int variant(String key,String source,ResearchState state,UnaryOperator<String> signature){
        String wanted=signature.apply(source);var variants=recipes(key,state);
        for(int i=0;i<variants.size();i++){
            var recipe=variants.get(i);
            // BluePrint has no item output in the original findRecipePage; its fallback
            // display block must not make an ordinary ingredient link to a construction.
            if(recipe.get("type").getAsString().contains("blueprint"))continue;
            if(signature.apply(output(recipe)).equals(wanted))return i;
        }
        return -1;
    }
    public static Reference reference(String source,ResearchState state,UnaryOperator<String> signature){
        if(source==null)return null;
        // Original getCraftingRecipeKey scans all entries, then applies each recipe's own gate.
        for(var entry:ResearchCatalog.all()){
            for(var section:entry.stages())for(String key:ResearchCatalog.strings(section,"recipes")){
                var all=recipes(key);
                boolean group=all.size()>1||all.stream().anyMatch(r->r.has("book_group")&&r.get("book_group").getAsString().equalsIgnoreCase(key));
                if(group){int variant=variant(key,source,state,signature);if(variant>=0)return new Reference(key,variant);}
                else if(!all.isEmpty()){
                    var recipe=all.getFirst();
                    if(!recipe.get("type").getAsString().contains("blueprint")&&signature.apply(output(recipe)).equals(signature.apply(source)))
                        return new Reference(key,unlocked(recipe,state)?0:-99);
                }
            }
        }
        return null;
    }
    private BookRecipeCatalog(){}
}
