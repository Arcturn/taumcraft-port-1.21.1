package thaumcraft.port.artifice.construct;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Player placed BETA26 constructs. Items retain the existing crafting registry IDs. */
public final class ConstructContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<EntityType<TurretEntity>> BASIC=ENTITIES.register("turret_basic",()->EntityType.Builder.<TurretEntity>of((t,l)->new TurretEntity(t,l,false),MobCategory.MISC).sized(.95f,1.25f).eyeHeight(.825f).clientTrackingRange(10).updateInterval(2).build("thaumcraft:turret_basic"));
    public static final Supplier<EntityType<TurretEntity>> ADVANCED=ENTITIES.register("turret_advanced",()->EntityType.Builder.<TurretEntity>of((t,l)->new TurretEntity(t,l,true),MobCategory.MISC).sized(.95f,1.5f).eyeHeight(1).clientTrackingRange(10).updateInterval(2).build("thaumcraft:turret_advanced"));
    public static final Supplier<EntityType<ArcaneBoreEntity>> BORE=ENTITIES.register("arcane_bore",()->EntityType.Builder.<ArcaneBoreEntity>of(ArcaneBoreEntity::new,MobCategory.MISC).sized(.9f,.9f).eyeHeight(.8125f).clientTrackingRange(10).updateInterval(2).build("thaumcraft:arcane_bore"));
    public static final Supplier<MenuType<ConstructMenu>> MENU=MENUS.register("construct",()->IMenuTypeExtension.create((id,inventory,buffer)->new ConstructMenu(id,inventory,buffer.readVarInt())));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(ConstructContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(BASIC.get(),TurretEntity.attributes(false).build());event.put(ADVANCED.get(),TurretEntity.attributes(true).build());event.put(BORE.get(),ArcaneBoreEntity.attributes().build());}
    private ConstructContent(){}
}
