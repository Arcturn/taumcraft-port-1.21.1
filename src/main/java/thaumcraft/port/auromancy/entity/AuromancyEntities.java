package thaumcraft.port.auromancy.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AuromancyEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    public static final DeferredHolder<EntityType<?>,EntityType<EntityFocusProjectile>> PROJECTILE = TYPES.register("focus_projectile",() -> EntityType.Builder.<EntityFocusProjectile>of(EntityFocusProjectile::new,MobCategory.MISC).sized(.15f,.15f).clientTrackingRange(8).updateInterval(1).build("thaumcraft:focus_projectile"));
    public static final DeferredHolder<EntityType<?>,EntityType<EntityFocusMine>> MINE = TYPES.register("focus_mine",() -> EntityType.Builder.<EntityFocusMine>of(EntityFocusMine::new,MobCategory.MISC).sized(.15f,.15f).clientTrackingRange(8).updateInterval(1).build("thaumcraft:focus_mine"));
    public static final DeferredHolder<EntityType<?>,EntityType<EntityFocusCloud>> CLOUD = TYPES.register("focus_cloud",() -> EntityType.Builder.<EntityFocusCloud>of(EntityFocusCloud::new,MobCategory.MISC).sized(1,.5f).clientTrackingRange(8).updateInterval(5).build("thaumcraft:focus_cloud"));
    public static final DeferredHolder<EntityType<?>,EntityType<EntitySpellBat>> BAT = TYPES.register("spellbat",() -> EntityType.Builder.<EntitySpellBat>of(EntitySpellBat::new,MobCategory.MISC).sized(.5f,.9f).clientTrackingRange(8).updateInterval(1).build("thaumcraft:spellbat"));
    public static void register(IEventBus bus) {
        TYPES.register(bus); bus.addListener(AuromancyEntities::attributes);
        NeoForge.EVENT_BUS.addListener(SpellExecution::tick);
    }
    private static void attributes(EntityAttributeCreationEvent event) { event.put(BAT.get(),EntitySpellBat.attributes().build()); }
    private AuromancyEntities() { }
}
