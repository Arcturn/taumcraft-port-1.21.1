package thaumcraft.port.world;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortContent;

public final class WorldContent {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"thaumcraft");
    public static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"thaumcraft");
    public static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"thaumcraft");
    public static final Supplier<Feature<?>> DEPOSITS=FEATURES.register("deposits",DepositsFeature::new);
    public static final Supplier<Feature<?>> VEGETATION=FEATURES.register("vegetation",VegetationFeature::new);
    public static final Supplier<Feature<?>> PORTALS=FEATURES.register("lesser_portals",PortalFeature::new);
    public static final Supplier<Feature<?>> GREATWOOD=FEATURES.register("greatwood",()->new MagicTreeFeature(false));
    public static final Supplier<Feature<?>> SILVERWOOD=FEATURES.register("silverwood",()->new MagicTreeFeature(true));
    public static final Supplier<StructureType<MoundStructure>> MOUND=STRUCTURES.register("mound",()->()->MoundStructure.CODEC);
    public static final Supplier<StructurePieceType> MOUND_PIECE=PIECES.register("mound",()->MoundStructure.Piece::new);
    public static final Map<String,DeferredBlock<? extends Block>> BLOCKS=new LinkedHashMap<>();
    public static final Map<Aspect,DeferredBlock<CrystalClusterBlock>> CRYSTALS=new EnumMap<>(Aspect.class);
    static {
        for(String wood:List.of("greatwood","silverwood")) {
            add("log_"+wood,()->new MagicLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
            add("leaves_"+wood,()->new MagicLeavesBlock(wood.equals("silverwood"),BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
            add("plank_"+wood,()->new MagicLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS),false));
            var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,id(wood+"_sapling"));
            var grower=new TreeGrower("thaumcraft:"+wood,wood.equals("greatwood")?Optional.of(key):Optional.empty(),wood.equals("silverwood")?Optional.of(key):Optional.empty(),Optional.empty());
            add("sapling_"+wood,()->new MagicSaplingBlock(grower,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
        }
        add("grass_ambient",()->new AmbientGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));
        for(String plant:List.of("shimmerleaf","cinderpearl","vishroom"))
            add(plant,()->new MagicPlantBlock(plant,BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).lightLevel(s->plant.equals("cinderpearl")?7:6)));
        for(Aspect a:List.of(Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY,Aspect.FLUX)) {
            String id="crystal_"+a.id();var b=PortContent.BLOCKS.register(id,()->new CrystalClusterBlock(a,BlockBehaviour.Properties.of().strength(.25f).sound(thaumcraft.port.content.PortSounds.CRYSTAL).noOcclusion().noCollission().randomTicks().lightLevel(s->1)));
            BLOCKS.put(id,b);CRYSTALS.put(a,b);PortContent.ITEMS.registerSimpleBlockItem(id,b);
        }
        for(String container:List.of("urn","crate")) for(String rarity:List.of("common","uncommon","rare")) {
            String id="loot_"+container+"_"+rarity;
            add(id,()->new LootContainerBlock(container.equals("urn"),BlockBehaviour.Properties.of().strength(.15f).sound(container.equals("urn")?thaumcraft.port.content.PortSounds.URN:SoundType.WOOD).noOcclusion()));
        }
        for(String kind:List.of("taint_fibre","taint_soil","taint_rock","taint_crust","taint_log"))
            add(kind,()->new TaintBlock(kind,BlockBehaviour.Properties.of().strength(kind.equals("taint_rock")?1.5f:.5f).sound(thaumcraft.port.content.PortSounds.GORE).noOcclusion()));
        add("flux_goo",()->new FluxGooBlock(BlockBehaviour.Properties.of().strength(100).sound(thaumcraft.port.content.PortSounds.GORE)));
        PortContent.ITEMS.registerSimpleItem("void_seed");
        for(String material:List.of("iron","gold","copper","cinnabar"))PortContent.ITEMS.registerSimpleItem("cluster_"+material);
        PortContent.ITEMS.registerItem("primordial_pearl",thaumcraft.port.content.PrimordialPearlItem::new,new Item.Properties().durability(8));
        add("stone_porous",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    }
    private static void add(String id,Supplier<? extends Block> factory) {
        var block=PortContent.BLOCKS.register(id,factory);BLOCKS.put(id,block);PortContent.ITEMS.registerSimpleBlockItem(id,block);
    }
    public static Block block(String id){return BLOCKS.get(id).get();}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public static void register(IEventBus bus){FEATURES.register(bus);STRUCTURES.register(bus);PIECES.register(bus);}
    private WorldContent(){}
}
