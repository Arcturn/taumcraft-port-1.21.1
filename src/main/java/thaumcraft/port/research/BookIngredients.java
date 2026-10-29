package thaumcraft.port.research;

import com.google.gson.JsonObject;
import java.util.List;

/** Original display alternatives, independent of modern gameplay tag membership. */
public final class BookIngredients {
    private static final JsonObject CATALOG=ResearchCatalog.read("/thaumcraft-port/book-ingredient-variants.json");
    public static List<String> variants(String name,boolean expanded){
        var table=CATALOG.getAsJsonObject(expanded?"ingredients":"raw");
        return table.has(name)?table.getAsJsonArray(name).asList().stream().map(value->value.getAsString()).toList():List.of();
    }
    public static int index(int size,int slot,long milliseconds){return size==0?-1:(int)Math.floorMod(slot+milliseconds/1000,size);}
    public static String resolve(String source,int slot,long milliseconds,boolean expanded){
        if(!source.startsWith("oredict:"))return source;
        String name=source.substring(8).split(";",2)[0];var choices=variants(name,expanded);
        return choices.isEmpty()?"minecraft:air":choices.get(index(choices.size(),slot,milliseconds));
    }
    private BookIngredients(){}
}
