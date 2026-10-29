package thaumcraft.port.content;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.research.ResearchCatalog;

/** Hidden GUI items carrying the original TC6 artwork, not collectible content. */
public final class ResearchDisplayItem extends Item {
    public record Variant(String source,String model,int tint){}
    private final List<Variant> variants;

    private ResearchDisplayItem(Properties properties,JsonObject definition) {
        super(properties);
        variants=definition.getAsJsonArray("variants").asList().stream().map(v->{
            var json=v.getAsJsonObject();return new Variant(json.get("source").getAsString(),json.get("model").getAsString(),json.has("tint")?json.get("tint").getAsInt():0xffffff);
        }).toList();
        if(variants.isEmpty())throw new IllegalArgumentException("Research icon without artwork");
    }
    public List<Variant> variants(){return variants;}
    public int frame(long millis){return frame(variants.size(),millis);}
    public static int frame(int size,long millis){
        return thaumcraft.port.research.ResearchIconTiming.frame(size,millis);
    }
    public int colour(int layer,long millis){return layer==0?0xff000000|variants.get(frame(millis)).tint():0xffffffff;}
    public static String forResearch(String key){return "research_display_research_"+(key.startsWith("!")?"scan_"+key.substring(1):key).toLowerCase(Locale.ROOT);}
    public static Map<String,DeferredItem<ResearchDisplayItem>> register(DeferredRegister.Items items) {
        var result=new LinkedHashMap<String,DeferredItem<ResearchDisplayItem>>();
        for(var entry:ResearchCatalog.read("/thaumcraft-port/research-display-icons.json").entrySet()) {
            JsonObject definition=entry.getValue().getAsJsonObject();
            result.put(entry.getKey(),items.registerItem(entry.getKey(),properties->new ResearchDisplayItem(properties,definition)));
        }
        return Collections.unmodifiableMap(result);
    }
}
