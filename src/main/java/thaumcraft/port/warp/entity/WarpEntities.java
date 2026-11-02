package thaumcraft.port.warp.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real entities created by warp events; they are not added to natural spawn lists. */
public final class WarpEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    public static final DeferredHolder<EntityType<?>,EntityType<MindSpider>> MIND_SPIDER=TYPES.register("mind_spider",()->EntityType.Builder.<MindSpider>of(MindSpider::new,MobCategory.MONSTER)
            .sized(.7f,.5f).eyeHeight(.45f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:mind_spider"));
    public static final DeferredHolder<EntityType<?>,EntityType<EldritchGuardian>> ELDRITCH_GUARDIAN=TYPES.register("eldritch_guardian",()->EntityType.Builder.<EldritchGuardian>of(EldritchGuardian::new,MobCategory.MONSTER)
            .sized(.8f,2.25f).eyeHeight(2.1f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_guardian"));
    public static final DeferredHolder<EntityType<?>,EntityType<EldritchOrb>> ELDRITCH_ORB=TYPES.register("eldritch_orb",()->EntityType.Builder.<EldritchOrb>of(EldritchOrb::new,MobCategory.MISC)
            .sized(.25f,.25f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_orb"));
    public static void register(IEventBus bus){TYPES.register(bus);bus.addListener(WarpEntities::attributes);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(MIND_SPIDER.get(),MindSpider.attributes().build());event.put(ELDRITCH_GUARDIAN.get(),EldritchGuardian.attributes().build());}
    private WarpEntities(){}
}
