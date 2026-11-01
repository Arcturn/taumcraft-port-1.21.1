package thaumcraft.port.research;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** Original runtime research content. Definitions alone do not imply implemented machines. */
public final class ResearchCatalog {
    // ScanGeneric visits TC6's category registry in insertion order, including gain notifications.
    public static final List<String> CATEGORIES=List.of("BASICS","AUROMANCY","ALCHEMY","ARTIFICE","INFUSION","GOLEMANCY","ELDRITCH");
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
                    strings(o,"parents"),strings(o,"siblings"),strings(o,"meta"),stages(o),strings(o,"icons"),objects(o,"addenda")))!=null)
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
    private static List<JsonObject> stages(JsonObject entry) {
        var stages=objects(entry,"stages");
        // BETA26 no longer registers thaumcraft:metal. Its ResearchManager skips
        // these two stale craft stacks, leaving getCraft() null in the live game.
        // Normalize the shared catalog so the book and server see the same rule.
        // Do not skip other unsupported requirements: those may be unported content.
        String obsolete=switch(entry.get("key").getAsString()){
            case "ESSENTIASMELTERTHAUMIUM"->"thaumcraft:metal;1;2";
            case "ESSENTIASMELTERVOID"->"thaumcraft:metal;1;3";
            default->null;
        };
        if(obsolete!=null)for(var stage:stages)if(stage.has("required_craft")){
            var crafts=stage.getAsJsonArray("required_craft");
            crafts.asList().removeIf(value->value.getAsString().equals(obsolete));
            if(crafts.isEmpty())stage.remove("required_craft");
        }
        return stages;
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
