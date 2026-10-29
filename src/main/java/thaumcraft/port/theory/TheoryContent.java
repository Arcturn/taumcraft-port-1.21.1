package thaumcraft.port.theory;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;

public final class TheoryContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final DeferredBlock<ResearchTableBlock> TABLE=PortContent.BLOCKS.register("research_table",()->new ResearchTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2).noOcclusion()));
    public static final DeferredBlock<WoodTableBlock> WOOD_TABLE=PortContent.BLOCKS.register("table_wood",()->new WoodTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2).noOcclusion()));
    public static final Supplier<BlockEntityType<ResearchTableEntity>> ENTITY=ENTITIES.register("research_table",()->BlockEntityType.Builder.of(ResearchTableEntity::new,TABLE.get()).build(null));
    public static final Supplier<MenuType<TheoryMenu>> MENU=MENUS.register("research_table",()->IMenuTypeExtension.create((id,inventory,buffer)->new TheoryMenu(id,inventory,new net.minecraft.world.SimpleContainer(2),buffer.readBlockPos())));
    static {
        PortContent.ITEMS.registerSimpleBlockItem("research_table",TABLE);PortContent.ITEMS.registerSimpleBlockItem("table_wood",WOOD_TABLE);
        for(String id:java.util.List.of("vis_resonator","brain","fabric"))PortContent.ITEMS.registerSimpleItem(id);
        for(String type:TheoryCards.CURIOS)PortContent.ITEMS.registerItem("curio_"+type,p->new ResearchCurioItem(p,type),new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);}
    private TheoryContent(){}
}
