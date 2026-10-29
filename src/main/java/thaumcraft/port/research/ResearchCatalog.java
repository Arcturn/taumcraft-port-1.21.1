package thaumcraft.port.research;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** Immutable original research content. Definitions alone do not imply implemented machines. */
public final class ResearchCatalog {
    public static final List<String> CATEGORIES=List.of("BASICS","ALCHEMY","AUROMANCY","ARTIFICE","INFUSION","GOLEMANCY","ELDRITCH");
    public record Entry(String key,String title,String category,int x,int y,List<String> parents,
                        List<String> siblings,List<String> meta,List<JsonObject> stages,List<String> icons,List<JsonObject> addenda) {}
    private static final Map<String,Entry> ENTRIES=load();
    private static final JsonObject FORMULAS=read("/thaumcraft-port/research-formulas.json");
    private static Map<String,Entry> load() {
        Map<String,Entry> entries=new LinkedHashMap<>();
        for(String file:List.of("basics","alchemy","auromancy","artifice","infusion","golemancy","eldritch","scans"))
            for(JsonElement element:read("/assets/thaumcraft/research/"+file+".json").getAsJsonArray("entries")) {
                var o=element.getAsJsonObject();var location=o.getAsJsonArray("location");
                String key=o.get("key").getAsString();
                if(entries.put(key,new Entry(key,o.get("name").getAsString(),o.get("category").getAsString(),
                    location==null?0:location.get(0).getAsInt(),location==null?0:location.get(1).getAsInt(),
                    strings(o,"parents"),strings(o,"siblings"),strings(o,"meta"),objects(o,"stages"),strings(o,"icons"),objects(o,"addenda")))!=null)
                    throw new IllegalStateException("Duplicate research "+key);
            }
        return Collections.unmodifiableMap(entries);
    }
    public static JsonObject read(String path) {
        try(var in=new InputStreamReader(Objects.requireNonNull(ResearchCatalog.class.getResourceAsStream(path),path),StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        } catch(IOException e) { throw new IllegalStateException(path,e); }
    }
    public static List<String> strings(JsonObject object,String key) {
        if(!object.has(key))return List.of();
        return object.getAsJsonArray(key).asList().stream().map(JsonElement::getAsString).toList();
    }
    private static List<JsonObject> objects(JsonObject object,String key) {
        if(!object.has(key))return List.of();
        return object.getAsJsonArray(key).asList().stream().map(JsonElement::getAsJsonObject).toList();
    }
    public static Entry get(String key) { return ENTRIES.get(key); }
    public static Collection<Entry> all() { return ENTRIES.values(); }
    public static int observation(String category,Map<Aspect,Integer> aspects) {
        var formula=FORMULAS.getAsJsonObject(category);if(formula==null)return 0;
        double sum=0;for(var e:formula.entrySet())sum+=aspects.getOrDefault(Aspect.valueOf(e.getKey()),0)*e.getValue().getAsDouble()/10;
        return (int)Math.ceil(Math.sqrt(Math.max(0,sum)));
    }
    private ResearchCatalog() {}
}
