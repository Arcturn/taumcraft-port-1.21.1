package thaumcraft.port.artifice.furnace;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;

/** The formed furnace and its invisible, solid shell are separate original blocks. */
public final class InfernalFurnaceContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<InfernalFurnaceBlock> FURNACE=PortContent.BLOCKS.register("infernal_furnace",()->new InfernalFurnaceBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2,12).lightLevel(s->13).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<FurnaceShellBlock> BRICK=shell("placeholder_brick",Blocks.NETHER_BRICKS),OBSIDIAN=shell("placeholder_obsidian",Blocks.OBSIDIAN);
    public static final Supplier<BlockEntityType<InfernalFurnaceEntity>> ENTITY=ENTITIES.register("infernal_furnace",()->BlockEntityType.Builder.of(InfernalFurnaceEntity::new,FURNACE.get()).build(null));
    public static final Map<String,DeferredItem<Item>> NUGGETS=new LinkedHashMap<>();
    public static final Supplier<RecipeSerializer<NuggetCompressionRecipe>> COMPRESSION=PortContent.SERIALIZERS.register("furnace_nugget_compression",()->new SimpleCraftingRecipeSerializer<>(NuggetCompressionRecipe::new));
    static {
        PortContent.ITEMS.registerSimpleBlockItem("infernal_furnace",FURNACE);
        for(String metal:List.of("copper","tin","silver","lead"))NUGGETS.put(metal,PortContent.ITEMS.registerItem("nugget_"+metal,p->new Item(p){
            @Override public String getDescriptionId(){return "item.nugget."+metal+".name";}
        }));
    }
    private static DeferredBlock<FurnaceShellBlock> shell(String name,Block original){return PortContent.BLOCKS.register(name,()->new FurnaceShellBlock(original,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2.5f).noOcclusion().pushReaction(PushReaction.BLOCK)));}
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(InfernalFurnaceContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->new SidedInvWrapper(be,side==null?Direction.UP:side));}
    private InfernalFurnaceContent(){}
}
