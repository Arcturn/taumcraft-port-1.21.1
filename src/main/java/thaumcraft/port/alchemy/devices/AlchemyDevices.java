package thaumcraft.port.alchemy.devices;

import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.essentia.EssentiaContent;

/** TC6's water-producing urn and reusable potion-template sprayer. */
public final class AlchemyDevices {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final DeferredBlock<EverfullUrnBlock> URN=PortContent.BLOCKS.register("everfull_urn",()->new EverfullUrnBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.5f).noOcclusion()));
    public static final DeferredBlock<PotionSprayerBlock> SPRAYER=PortContent.BLOCKS.register("potion_sprayer",()->new PotionSprayerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5f)));
    public static final Supplier<BlockEntityType<EverfullUrnEntity>> URN_ENTITY=ENTITIES.register("everfull_urn",()->BlockEntityType.Builder.of(EverfullUrnEntity::new,URN.get()).build(null));
    public static final Supplier<BlockEntityType<PotionSprayerEntity>> SPRAYER_ENTITY=ENTITIES.register("potion_sprayer",()->BlockEntityType.Builder.of(PotionSprayerEntity::new,SPRAYER.get()).build(null));
    public static final Supplier<MenuType<PotionSprayerMenu>> SPRAYER_MENU=MENUS.register("potion_sprayer",()->IMenuTypeExtension.create((id,inventory,buffer)->new PotionSprayerMenu(id,inventory)));
    static {PortContent.ITEMS.registerSimpleBlockItem("everfull_urn",URN);PortContent.ITEMS.registerSimpleBlockItem("potion_sprayer",SPRAYER);}
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(AlchemyDevices::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,URN_ENTITY.get(),(entity,side)->side==Direction.UP?entity.fluidHandler:null);
        event.registerBlockEntity(EssentiaContent.CAPABILITY,SPRAYER_ENTITY.get(),(entity,side)->entity.connects(side)?entity:null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,SPRAYER_ENTITY.get(),(entity,side)->new InvWrapper(entity));
    }
    private AlchemyDevices(){}
}
