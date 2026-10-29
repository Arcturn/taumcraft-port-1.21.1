package thaumcraft.port.data;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.advancements.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import thaumcraft.port.research.*;

/** Actual registered criterion/advancement codecs, tree layout and persistent vanilla progress, without a server. */
public final class AdvancementFixtures {
    public static int validate(ResourceManager resources,RegistryOps<JsonElement> ops)throws Exception{
        int checks=0;var holders=new ArrayList<AdvancementHolder>();var researches=new HashSet<String>();var categories=new HashSet<String>();
        var displayDefinitions=ResearchCatalog.read("/thaumcraft-port/research-display-icons.json");
        var empty=new ResearchState();var completed=new ResearchState();completed.completeCatalog();completed.flag("!gotthaumonomicon");
        for(var resource:resources.listResources("advancement",id->id.getNamespace().equals("thaumcraft")&&id.getPath().endsWith(".json")).entrySet()){
            JsonObject json;try(var reader=resource.getValue().openAsReader()){json=JsonParser.parseReader(reader).getAsJsonObject();}
            var decoded=Advancement.CODEC.parse(ops,json).getOrThrow();
            String path=resource.getKey().getPath();var id=ResourceLocation.fromNamespaceAndPath("thaumcraft",path.substring("advancement/".length(),path.length()-5));
            holders.add(new AdvancementHolder(id,decoded));
            require(decoded.display().isPresent()&&!decoded.display().get().getIcon().isEmpty(),"Every advancement has a registered, nonempty icon: "+id);checks++;
            var icon=decoded.display().orElseThrow().getIcon();
            require(icon.getItem() instanceof thaumcraft.port.content.ResearchDisplayItem&&!icon.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA),"Original research icon uses its own registered model: "+id);checks++;
            String itemId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.getItem()).getPath();
            require(displayDefinitions.has(itemId)&&displayDefinitions.getAsJsonObject(itemId).get("advancement").getAsString().equals(id.toString()),"Original icon belongs to this advancement: "+id);checks++;
            require(decoded.criteria().size()==1,"One server-owned research condition: "+id);checks++;
            var criterion=decoded.criteria().values().iterator().next();
            require(criterion.trigger()==ResearchAdvancements.RESEARCH.get()&&criterion.triggerInstance() instanceof ResearchAdvancements.Instance,"Actual registered research trigger: "+id);checks++;
            var condition=(ResearchAdvancements.Instance)criterion.triggerInstance();
            require(!condition.matches(empty)&&condition.matches(completed),"Unstarted progress fails, completed saved progress succeeds: "+id);checks++;
            if(!condition.research().isEmpty()&&!condition.research().equals("!gotthaumonomicon"))require(researches.add(condition.research()),"Duplicate advancement for research");
            if(!condition.category().isEmpty())require(categories.add(condition.category()),"Duplicate category");
            var progress=new AdvancementProgress();progress.update(decoded.requirements());
            require(!progress.isDone()&&progress.grantProgress("completed")&&progress.isDone()&&!progress.grantProgress("completed"),"Vanilla grants once: "+id);checks++;
            var saved=AdvancementProgress.CODEC.encodeStart(JsonOps.INSTANCE,progress).getOrThrow();
            var restored=AdvancementProgress.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();restored.update(decoded.requirements());
            require(restored.isDone()&&!restored.grantProgress("completed"),"Vanilla advancement progress survives saving: "+id);checks++;
        }
        require(researches.equals(ResearchCatalog.all().stream().map(ResearchCatalog.Entry::key).collect(java.util.stream.Collectors.toSet())),"All original research records represented, including hidden scans");checks++;
        require(categories.equals(new HashSet<>(ResearchCatalog.CATEGORIES))&&holders.size()==156,"Seven categories, 148 entries and one root");checks++;
        require(displayDefinitions.size()==156&&thaumcraft.port.content.PortContent.RESEARCH_DISPLAY_ICONS.size()==156,"All original research display items registered");checks++;
        int variants=0,cycling=0;
        for(var entry:ResearchCatalog.all()) {
            var item=thaumcraft.port.content.PortContent.RESEARCH_DISPLAY_ICONS.get(thaumcraft.port.content.ResearchDisplayItem.forResearch(entry.key())).get();
            require(item.variants().stream().map(thaumcraft.port.content.ResearchDisplayItem.Variant::source).toList().equals(entry.icons()),"Every original icon variant retained in its source order: "+entry.key());checks++;
            for(int frame=0;frame<entry.icons().size();frame++) {
                require(item.frame(frame*1000L)==frame&&item.frame(frame*1000L+999)==frame,"Original one-second icon cycle: "+entry.key());checks++;
            }
            variants+=entry.icons().size();if(entry.icons().size()>1)cycling++;
        }
        require(variants==165&&cycling==8,"Original catalog has 165 icons across 148 entries, eight cycling entries");checks++;
        var crystals=thaumcraft.port.content.PortContent.RESEARCH_DISPLAY_ICONS.get("research_display_research_crystalfarmer").get();
        int[] originalCrystalColours={0xffff7e,16734721,3986684,5685248,14013676,0x404040};
        for(int frame=0;frame<6;frame++) {
            require(crystals.colour(0,frame*1000L)==(0xff000000|originalCrystalColours[frame])&&crystals.colour(1,frame*1000L)==0xffffffff,"Original crystal-planter tint at frame "+frame);checks++;
        }
        var tree=new AdvancementTree();tree.addAll(holders);
        require(tree.nodes().size()==holders.size(),"No missing parents or advancement graph cycles");checks++;
        int roots=0;for(var root:tree.roots()){TreeNodePosition.run(root);roots++;}
        require(roots==1,"Exactly one Thaumcraft advancement tab");checks++;
        for(var holder:holders){var display=holder.value().display().orElseThrow();require(Float.isFinite(display.getX())&&Float.isFinite(display.getY()),"Minecraft computes valid tree layout");checks++;}
        for(String invalid:List.of("{}","{\"research\":\"missing\"}","{\"category\":\"missing\"}","{\"research\":\"FIRSTSTEPS\",\"category\":\"BASICS\"}")){
            require(ResearchAdvancements.Instance.CODEC.parse(ops,JsonParser.parseString(invalid)).error().isPresent(),"Malformed criterion fails closed");checks++;
        }
        return checks;
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private AdvancementFixtures(){}
}
