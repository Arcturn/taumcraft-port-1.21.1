package thaumcraft.port.artifice.tool;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArtificeTools {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    public static final DeferredHolder<EntityType<?>,EntityType<GrappleEntity>> GRAPPLE=ENTITIES.register("grapple",()->EntityType.Builder.<GrappleEntity>of(GrappleEntity::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build("thaumcraft:grapple"));
    public static void register(IEventBus bus){ENTITIES.register(bus);}
    private ArtificeTools(){}
}
