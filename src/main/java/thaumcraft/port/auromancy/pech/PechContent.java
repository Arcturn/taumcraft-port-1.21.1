package thaumcraft.port.auromancy.pech;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

public final class PechContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister.Items ITEMS=thaumcraft.port.content.PortContent.ITEMS;
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<EntityType<PechEntity>> PECH=ENTITIES.register("pech",()->EntityType.Builder.<PechEntity>of(PechEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).eyeHeight(1.188f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:pech"));
    public static final DeferredItem<PechWandItem> WAND=ITEMS.registerItem("pech_wand",PechWandItem::new,new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<SpawnEggItem> SPAWN_EGG=ITEMS.registerItem("pech_spawn_egg",p->new net.neoforged.neoforge.common.DeferredSpawnEggItem(PECH,-16129,-12582848,p));
    public static final Supplier<MenuType<PechMenu>> MENU=MENUS.register("pech",()->IMenuTypeExtension.create((id,inventory,buffer)->new PechMenu(id,inventory,buffer.readVarInt())));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(PechContent::attributes);bus.addListener(PechContent::placements);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(PECH.get(),PechEntity.attributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent event){event.register(PECH.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,PechEntity::canSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    private PechContent(){}
}
