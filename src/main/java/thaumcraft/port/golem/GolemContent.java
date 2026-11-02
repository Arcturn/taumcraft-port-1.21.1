package thaumcraft.port.golem;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.golem.builder.*;
import thaumcraft.port.golem.item.*;
import thaumcraft.port.golem.projectile.*;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.TaskHandler;

/** BETA26 golem and press registrations; existing seal item IDs remain unchanged. */
public final class GolemContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<EntityType<GolemEntity>> GOLEM=ENTITIES.register("golem",()->EntityType.Builder.<GolemEntity>of(GolemEntity::new,MobCategory.MISC).sized(.4f,.9f).eyeHeight(.7f).clientTrackingRange(10).updateInterval(2).build("thaumcraft:golem"));
    public static final Supplier<EntityType<GolemDartEntity>> DART=ENTITIES.register("golem_dart",()->EntityType.Builder.<GolemDartEntity>of(GolemDartEntity::new,MobCategory.MISC).sized(.2f,.2f).clientTrackingRange(10).updateInterval(1).build("thaumcraft:golem_dart"));
    public static final Supplier<EntityType<GolemOrbEntity>> ORB=ENTITIES.register("golem_orb",()->EntityType.Builder.<GolemOrbEntity>of(GolemOrbEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(10).updateInterval(1).build("thaumcraft:golem_orb"));
    public static final DeferredItem<GolemPlacerItem> GOLEM_ITEM=PortContent.ITEMS.registerItem("golem",GolemPlacerItem::new);
    public static final DeferredItem<GolemBellItem> BELL=PortContent.ITEMS.registerItem("golem_bell",GolemBellItem::new);
    public static final DeferredBlock<GolemBuilderBlock> BUILDER=PortContent.BLOCKS.register("golem_builder",()->new GolemBuilderBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.5f).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<GolemBuilderShellBlock> BUILDER_BARS=shell("placeholder_bars",()->Blocks.IRON_BARS),
            BUILDER_ANVIL=shell("placeholder_anvil",()->Blocks.ANVIL),BUILDER_CAULDRON=shell("placeholder_cauldron",()->Blocks.CAULDRON),
            BUILDER_TABLE=shell("placeholder_table",()->CraftingContent.TABLE_STONE.get());
    public static final Supplier<BlockEntityType<GolemBuilderEntity>> BUILDER_ENTITY=BLOCK_ENTITIES.register("golem_builder",()->BlockEntityType.Builder.of(GolemBuilderEntity::new,BUILDER.get()).build(null));
    public static final Supplier<MenuType<GolemBuilderMenu>> BUILDER_MENU=MENUS.register("golem_builder",()->IMenuTypeExtension.create((id,inventory,buffer)->new GolemBuilderMenu(id,inventory,buffer.readBlockPos())));
    public static final Supplier<MenuType<SealMenu>> SEAL_MENU=MENUS.register("golem_seal",()->IMenuTypeExtension.create(SealMenu::new));
    public static final Supplier<MenuType<LogisticsMenu>> LOGISTICS_MENU=MENUS.register("golem_logistics",()->IMenuTypeExtension.create(LogisticsMenu::new));
    static {PortContent.ITEMS.registerSimpleBlockItem("golem_builder",BUILDER);}
    private static DeferredBlock<GolemBuilderShellBlock> shell(String id,Supplier<Block> original){return PortContent.BLOCKS.register(id,()->new GolemBuilderShellBlock(original.get(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3).noOcclusion().pushReaction(PushReaction.BLOCK)));}
    public static void register(IEventBus bus){
        ENTITIES.register(bus);BLOCK_ENTITIES.register(bus);MENUS.register(bus);bus.addListener(GolemContent::attributes);bus.addListener(GolemContent::capabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(GolemContent::registerSeals));
    }
    private static void registerSeals(){
        for(var seal:java.util.List.of(new SealPickup(),new SealPickupAdvanced(),new SealFill(),new SealFillAdvanced(),
                new SealEmpty(),new SealEmptyAdvanced(),new SealHarvest(),new SealButcher(),new SealGuard(),new SealGuardAdvanced(),
                new SealLumber(),new SealBreaker(),new SealUse(),new SealProvide(),new SealStock(),new SealBreakerAdvanced()))SealHandler.registerSeal(seal);
    }
    private static void attributes(EntityAttributeCreationEvent event){event.put(GOLEM.get(),GolemEntity.attributes().build());}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(EssentiaContent.CAPABILITY,BUILDER_ENTITY.get(),(be,side)->be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,BUILDER_ENTITY.get(),(be,side)->new InvWrapper(be));
    }
    @EventBusSubscriber(modid="thaumcraft")
    public static final class Runtime {
        @SubscribeEvent(priority=EventPriority.HIGH) public static void tick(LevelTickEvent.Post event){
            // Original ServerEvents decrements task lifespan once per second, before ticking seals.
            if(event.getLevel() instanceof ServerLevel level&&level.getGameTime()%20==0)TaskHandler.clearSuspendedOrExpiredTasks(level);
        }
        @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)GolemHelper.clear(level);}
        @SubscribeEvent public static void stopped(ServerStoppedEvent event){GolemHelper.clear();}
    }
    private GolemContent(){}
}
