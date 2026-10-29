package thaumcraft.port.essentia;

import com.mojang.serialization.Codec;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.*;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.*;

public final class EssentiaContent {
    public static final BlockCapability<EssentiaTransport,net.minecraft.core.Direction> CAPABILITY=BlockCapability.createSided(ResourceLocation.parse("thaumcraft:essentia"),EssentiaTransport.class);
    public static final BlockCapability<EssentiaSource,Void> SOURCE=BlockCapability.createVoid(ResourceLocation.parse("thaumcraft:essentia_source"),EssentiaSource.class);
    public static final DeferredItem<JarBraceItem> BRACE=PortContent.ITEMS.registerItem("jar_brace",JarBraceItem::new);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<DataComponentType<String>> ASPECT=PortContent.COMPONENTS.register("essentia_aspect",()->DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredItem<EssentiaItem> PHIAL=PortContent.ITEMS.registerItem("phial",EssentiaItem::new);
    public static final DeferredItem<EssentiaItem> LABEL=PortContent.ITEMS.registerItem("label",EssentiaItem::new);
    public static final Supplier<net.minecraft.world.item.crafting.RecipeSerializer<thaumcraft.port.crafting.LabelRecipe>> LABEL_RECIPE=PortContent.SERIALIZERS.register("label_aspect",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(thaumcraft.port.crafting.LabelRecipe::new));
    public static final DeferredBlock<JarBlock> JAR=jar("jar_normal"),VOID_JAR=jar("jar_void"),ALEMBIC=jar("alembic");
    public static final Map<String,DeferredBlock<TubeBlock>> TUBES=new LinkedHashMap<>();
    public static final Map<String,DeferredBlock<SmelterBlock>> SMELTERS=new LinkedHashMap<>();
    public static final DeferredBlock<CentrifugeBlock> CENTRIFUGE=PortContent.BLOCKS.register("centrifuge",()->new CentrifugeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<BellowsBlock> BELLOWS=PortContent.BLOCKS.register("bellows",()->new BellowsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredBlock<SmelterAttachmentBlock> AUX=PortContent.BLOCKS.register("smelter_aux",()->new SmelterAttachmentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion())),VENT=PortContent.BLOCKS.register("smelter_vent",()->new SmelterAttachmentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final Supplier<BlockEntityType<BellowsEntity>> BELLOWS_ENTITY=ENTITIES.register("bellows",()->BlockEntityType.Builder.of(BellowsEntity::new,BELLOWS.get()).build(null));
    public static final DeferredBlock<Block> CONSTRUCT=PortContent.BLOCKS.registerSimpleBlock("metal_alchemical",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<ThaumatoriumBlock> THAUMATORIUM=PortContent.BLOCKS.register("thaumatorium",()->new ThaumatoriumBlock(false,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion())),THAUMATORIUM_TOP=PortContent.BLOCKS.register("thaumatorium_top",()->new ThaumatoriumBlock(true,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<BrainBoxBlock> BRAIN_BOX=PortContent.BLOCKS.register("brain_box",()->new BrainBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final Supplier<BlockEntityType<ThaumatoriumEntity>> THAUMATORIUM_ENTITY=ENTITIES.register("thaumatorium",()->BlockEntityType.Builder.of(ThaumatoriumEntity::new,THAUMATORIUM.get(),THAUMATORIUM_TOP.get()).build(null));
    public static final Supplier<MenuType<ThaumatoriumMenu>> THAUMATORIUM_MENU=MENUS.register("thaumatorium",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new ThaumatoriumMenu(id,inv)));
    static {
        PortContent.ITEMS.registerSimpleBlockItem("brain_box",BRAIN_BOX);PortContent.ITEMS.registerSimpleBlockItem("thaumatorium",THAUMATORIUM);PortContent.ITEMS.registerSimpleBlockItem("thaumatorium_top",THAUMATORIUM_TOP);PortContent.ITEMS.registerSimpleItem("mind_clockwork");

        PortContent.ITEMS.registerSimpleBlockItem("bellows",BELLOWS);PortContent.ITEMS.registerSimpleBlockItem("smelter_aux",AUX);PortContent.ITEMS.registerSimpleBlockItem("smelter_vent",VENT);
        for(TubeBlock.Kind kind:TubeBlock.Kind.values()){
            String name=kind==TubeBlock.Kind.NORMAL?"tube":"tube_"+kind.name().toLowerCase(Locale.ROOT);
            var block=PortContent.BLOCKS.register(name,()->new TubeBlock(kind,BlockBehaviour.Properties.of().strength(.5f).sound(SoundType.METAL).noOcclusion()));TUBES.put(name,block);PortContent.ITEMS.registerSimpleBlockItem(name,block);
        }
        for(int i=0;i<3;i++){final int tier=i;String name="smelter_"+List.of("basic","thaumium","void").get(i);var block=PortContent.BLOCKS.register(name,()->new SmelterBlock(tier,BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).lightLevel(s->s.getValue(SmelterBlock.LIT)?13:0)));SMELTERS.put(name,block);PortContent.ITEMS.registerSimpleBlockItem(name,block);}
        PortContent.ITEMS.registerSimpleBlockItem("centrifuge",CENTRIFUGE);
        for(String name:List.of("filter","plate_iron","plate_brass","plate_thaumium","plate_void","mechanism_simple","morphic_resonator"))PortContent.ITEMS.registerSimpleItem(name);
        PortContent.ITEMS.registerSimpleBlockItem("metal_alchemical",CONSTRUCT);
        var advanced=PortContent.BLOCKS.registerSimpleBlock("metal_alchemical_advanced",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));PortContent.ITEMS.registerSimpleBlockItem("metal_alchemical_advanced",advanced);
    }
    public static final Supplier<BlockEntityType<JarEntity>> JAR_ENTITY=ENTITIES.register("jar",()->BlockEntityType.Builder.of(JarEntity::new,JAR.get(),VOID_JAR.get(),ALEMBIC.get()).build(null));
    public static final Supplier<BlockEntityType<TubeEntity>> TUBE_ENTITY=ENTITIES.register("tube",()->BlockEntityType.Builder.of((pos,state)->((TubeBlock)state.getBlock()).kind==TubeBlock.Kind.BUFFER?new BufferEntity(pos,state):new TubeEntity(pos,state),TUBES.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    public static final Supplier<BlockEntityType<SmelterEntity>> SMELTER_ENTITY=ENTITIES.register("smelter",()->BlockEntityType.Builder.of(SmelterEntity::new,SMELTERS.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    public static final Supplier<BlockEntityType<CentrifugeEntity>> CENTRIFUGE_ENTITY=ENTITIES.register("centrifuge",()->BlockEntityType.Builder.of(CentrifugeEntity::new,CENTRIFUGE.get()).build(null));
    public static final Supplier<MenuType<SmelterMenu>> SMELTER_MENU=MENUS.register("smelter",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new SmelterMenu(id,inv)));
    private static DeferredBlock<JarBlock> jar(String name){var block=PortContent.BLOCKS.register(name,()->new JarBlock(BlockBehaviour.Properties.of().strength(.3f).sound(PortSounds.JAR).noOcclusion()));if(name.equals("alembic"))PortContent.ITEMS.registerSimpleBlockItem(name,block);else PortContent.ITEMS.registerItem(name,props->new JarItem(block.get(),props));return block;}
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(EssentiaContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent e){
        e.registerBlockEntity(CAPABILITY,JAR_ENTITY.get(),(be,d)->be);
        e.registerBlockEntity(SOURCE,JAR_ENTITY.get(),(be,ignored)->be.alembic?null:be);
        e.registerBlockEntity(CAPABILITY,THAUMATORIUM_ENTITY.get(),(be,d)->be);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,THAUMATORIUM_ENTITY.get(),(be,d)->be.main()==null?null:new InvWrapper(be.main()));
        e.registerBlockEntity(CAPABILITY,TUBE_ENTITY.get(),(be,d)->be);
        e.registerBlockEntity(CAPABILITY,CENTRIFUGE_ENTITY.get(),(be,d)->be);
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,SMELTER_ENTITY.get(),(be,d)->d==null?new InvWrapper(be):new SidedInvWrapper(be,d));
    }
    private EssentiaContent(){}
}
