package thaumcraft.port.machine;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.ArcaneRecipe;

public final class MachineContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    private static final DeferredRegister<RecipeType<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_TYPE,"thaumcraft");
    public static final DeferredBlock<WorkbenchBlock> WORKBENCH=PortContent.BLOCKS.register("arcane_workbench",()->new WorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).noOcclusion()));
    public static final Supplier<BlockEntityType<WorkbenchEntity>> WORKBENCH_ENTITY=ENTITIES.register("arcane_workbench",()->BlockEntityType.Builder.of(WorkbenchEntity::new,WORKBENCH.get()).build(null));
    public static final Supplier<MenuType<WorkbenchMenu>> WORKBENCH_MENU=MENUS.register("arcane_workbench",()->IMenuTypeExtension.create((id,inventory,buffer)->new WorkbenchMenu(id,inventory,new net.minecraft.world.SimpleContainer(15),buffer.readBlockPos())));
    public static final Supplier<RecipeType<ArcaneRecipe>> ARCANE_RECIPE=RECIPES.register("arcane",()->new RecipeType<ArcaneRecipe>(){public String toString(){return "thaumcraft:arcane";}});
    public static final Supplier<RecipeSerializer<ArcaneRecipe>> ARCANE_SERIALIZER=PortContent.SERIALIZERS.register("arcane",ArcaneRecipe.Serializer::new);
    public static final DeferredBlock<CrucibleBlock> CRUCIBLE=PortContent.BLOCKS.register("crucible",()->new CrucibleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).noOcclusion()));
    public static final Supplier<BlockEntityType<CrucibleEntity>> CRUCIBLE_ENTITY=ENTITIES.register("crucible",()->BlockEntityType.Builder.of(CrucibleEntity::new,CRUCIBLE.get()).build(null));
    public static final Supplier<RecipeType<thaumcraft.port.crafting.CrucibleRecipe>> CRUCIBLE_RECIPE=RECIPES.register("crucible",()->new RecipeType<thaumcraft.port.crafting.CrucibleRecipe>(){public String toString(){return "thaumcraft:crucible";}});
    public static final Supplier<RecipeSerializer<thaumcraft.port.crafting.CrucibleRecipe>> CRUCIBLE_SERIALIZER=PortContent.SERIALIZERS.register("crucible",thaumcraft.port.crafting.CrucibleRecipe.Serializer::new);
    public static final java.util.Map<String,DeferredBlock<NitorBlock>> NITOR=new java.util.LinkedHashMap<>();
    static {
        PortContent.ITEMS.registerSimpleBlockItem("arcane_workbench",WORKBENCH);
        PortContent.ITEMS.registerSimpleBlockItem("crucible",CRUCIBLE);
        for(var color:net.minecraft.world.item.DyeColor.values()){
            String name="nitor_"+color.getName();var block=PortContent.BLOCKS.register(name,()->new NitorBlock(color,BlockBehaviour.Properties.of().strength(0).noCollission().noOcclusion().lightLevel(s->15)));
            NITOR.put(name,block);PortContent.ITEMS.registerSimpleBlockItem(name,block);
        }
        PortContent.ITEMS.registerSimpleItem("tallow");
        PortContent.ITEMS.registerItem("alumentum",thaumcraft.port.content.AlumentumItem::new);
        for(String name:java.util.List.of("arcane_stone","arcane_brick")){
            var block=PortContent.BLOCKS.registerSimpleBlock(name,BlockBehaviour.Properties.ofFullCopy(Blocks.STONE));
            PortContent.ITEMS.registerSimpleBlockItem(name,block);
        }
    }
    public static final Supplier<BlockEntityType<NitorEntity>> NITOR_ENTITY=ENTITIES.register("nitor",()->BlockEntityType.Builder.of(NitorEntity::new,NITOR.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);RECIPES.register(bus);}
    private MachineContent() {}
}
