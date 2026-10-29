package thaumcraft.port.data;

import com.google.gson.*;
import com.mojang.serialization.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.data.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.world.*;

/** Runs via NeoForge's supported data entry point: no server, world or EULA acceptance. */
@net.neoforged.fml.common.EventBusSubscriber(modid="thaumcraft",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class WorldDataChecks implements DataProvider {
    private final GatherDataEvent event;
    private final PackOutput packOutput;
    private WorldDataChecks(GatherDataEvent event,PackOutput output){this.event=event;this.packOutput=output;}
    @net.neoforged.bus.api.SubscribeEvent
    public static void gather(GatherDataEvent event){event.createProvider(output->new WorldDataChecks(event,output));}
    @Override public String getName(){return "Thaumcraft registry, worldgen, loot and structure validation";}
    @Override public CompletableFuture<?> run(CachedOutput output){
        try{
            ResourceManager resources=event.getResourceManager(PackType.SERVER_DATA);
            var base=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var world=RegistryDataLoader.load(resources,base,RegistryDataLoader.WORLDGEN_REGISTRIES);
            var registries=new RegistryAccess.ImmutableRegistryAccess(Stream.concat(base.registries(),world.registries()));
            bindTags(BuiltInRegistries.ITEM,resources);
            bindTags(BuiltInRegistries.BLOCK,resources);
            bindTags(BuiltInRegistries.FLUID,resources);
            var ops=RegistryOps.create(JsonOps.INSTANCE,registries);
            int loot=decode(resources,"loot_table",LootTable.DIRECT_CODEC,ops),recipes=decode(resources,"recipe",Recipe.CODEC,ops);
            int templates=0;
            for(var entry:resources.listResources("structure",id->id.getNamespace().equals("thaumcraft")&&id.getPath().endsWith(".nbt")).entrySet()){
                try(var in=entry.getValue().open()){
                    var tag=NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());var template=new StructureTemplate();template.load(BuiltInRegistries.BLOCK.asLookup(),tag);
                    if(entry.getKey().getPath().contains("mound")&&(template.getSize().getX()!=19||tag.getList("blocks",Tag.TAG_COMPOUND).size()<2441))throw new IllegalStateException("Broken mound "+entry.getKey());
                    templates++;
                }
            }
            var placed=world.registryOrThrow(Registries.PLACED_FEATURE);
            var deposits=placed.getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,WorldContent.id("deposits")));
            var plants=placed.getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,WorldContent.id("vegetation")));
            var portals=placed.getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,WorldContent.id("lesser_portals")));
            var biomes=world.registryOrThrow(Registries.BIOME).stream().toList();
            FeatureSorter.buildFeaturesPerStep(biomes,biome->{
                List<HolderSet<PlacedFeature>> steps=new ArrayList<>(biome.getGenerationSettings().features());
                while(steps.size()<11)steps.add(HolderSet.direct());
                for(int step:new int[]{4,6,9}){var features=new ArrayList<>(steps.get(step).stream().toList());features.add(step==4?portals:step==6?deposits:plants);steps.set(step,HolderSet.direct(features));}
                return steps;
            },true);
            for(var item:PortContent.ITEMS.getEntries()){
                var id=item.getId();if(!event.getResourceManager(PackType.CLIENT_RESOURCES).getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"models/item/"+id.getPath()+".json")).isPresent())throw new IllegalStateException("Missing item model: "+id);
            }
            JsonObject report=new JsonObject();report.addProperty("items",PortContent.ITEMS.getEntries().size());report.addProperty("blocks",PortContent.BLOCKS.getEntries().size());
            report.addProperty("tree_fixture_checks",TreeFixtures.validate(resources));
            report.addProperty("leaf_color_fixture_checks",LeafColorFixtures.validate(event.getResourceManager(PackType.CLIENT_RESOURCES)));
            report.addProperty("machine_fixture_checks",MachineFixtures.validate(resources,registries,ops));
            report.addProperty("essentia_fixture_checks",EssentiaFixtures.validate(resources,registries,ops));
            report.addProperty("essentia_extension_fixture_checks",EssentiaExtensionFixtures.validate(resources,registries,ops));
            report.addProperty("jar_bellows_fixture_checks",JarBellowsFixtures.validate(resources,registries,ops));
            report.addProperty("presentation_fixture_checks",PresentationFixtures.validate(event.getResourceManager(PackType.CLIENT_RESOURCES)));
            report.addProperty("theory_fixture_checks",thaumcraft.port.theory.TheoryFixtures.validate(registries));
            report.addProperty("advancement_fixture_checks",AdvancementFixtures.validate(resources,ops));
            report.addProperty("audit_fixture_checks",AuditFixtures.validate(registries));
            report.addProperty("aspect_fixture_checks",AspectFixtures.validate(resources,registries,ops));
            report.addProperty("scan_fixture_checks",ScanFixtures.validate(registries));
            report.addProperty("loot_tables_decoded",loot);report.addProperty("recipes_decoded",recipes);report.addProperty("templates_decoded",templates);report.addProperty("worldgen_registries_loaded",true);report.addProperty("feature_order_cycles",false);report.addProperty("server_started",false);
            var inventory=ContentSnapshot.capture(registries,resources);
            return CompletableFuture.allOf(
                DataProvider.saveStable(output,report,packOutput.getOutputFolder().resolve("thaumcraft-validation.json")),
                DataProvider.saveStable(output,inventory,packOutput.getOutputFolder().resolve("thaumcraft-content-snapshot.json")));
        }catch(Exception e){return CompletableFuture.failedFuture(e);}
    }
    private static <T> int decode(ResourceManager resources,String folder,Codec<T> codec,RegistryOps<JsonElement> ops)throws Exception{
        int n=0;for(var entry:resources.listResources(folder,id->id.getNamespace().equals("thaumcraft")&&id.getPath().endsWith(".json")).entrySet()){
            try(var in=entry.getValue().openAsReader()){var result=codec.parse(ops,JsonParser.parseReader(in));if(result.error().isPresent())throw new IllegalStateException(entry.getKey()+": "+result.error().orElseThrow().message());n++;}
        }return n;
    }
    private static <T> void bindTags(Registry<T> registry,ResourceManager resources){
        var loader=new net.minecraft.tags.TagLoader<Holder<T>>(registry::getHolder,Registries.tagsDirPath(registry.key()));
        Map<net.minecraft.tags.TagKey<T>,List<Holder<T>>> tags=new HashMap<>();
        loader.loadAndBuild(resources).forEach((id,values)->tags.put(net.minecraft.tags.TagKey.create(registry.key(),id),List.copyOf(values)));
        registry.bindTags(tags);
    }
}
