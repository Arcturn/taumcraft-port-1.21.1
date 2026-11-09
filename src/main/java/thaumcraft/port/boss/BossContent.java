package thaumcraft.port.boss;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

/** TC6 boss encounters. Their eggs expose them without adding unimplemented Outer Lands generation. */
public final class BossContent {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "thaumcraft");
    public static final Supplier<EntityType<EldritchGolem>> ELDRITCH_GOLEM = TYPES.register("eldritch_golem", () -> EntityType.Builder.<EldritchGolem>of(EldritchGolem::new, MobCategory.MONSTER)
            .sized(1.75f, 3.5f).eyeHeight(3f).fireImmune().clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_golem"));
    public static final Supplier<EntityType<EldritchWarden>> ELDRITCH_WARDEN = TYPES.register("eldritch_warden", () -> EntityType.Builder.<EldritchWarden>of(EldritchWarden::new, MobCategory.MONSTER)
            .sized(1.5f, 3.5f).eyeHeight(3.1f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_warden"));
    public static final Supplier<EntityType<CultistLeader>> CULTIST_LEADER = TYPES.register("cultist_leader", () -> EntityType.Builder.<CultistLeader>of(CultistLeader::new, MobCategory.MONSTER)
            .sized(.75f, 2.25f).eyeHeight(2.05f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:cultist_leader"));
    public static final Supplier<EntityType<CultistPortalGreater>> CULTIST_PORTAL_GREATER = TYPES.register("cultist_portal_greater", () -> EntityType.Builder.<CultistPortalGreater>of(CultistPortalGreater::new, MobCategory.MONSTER)
            .sized(1.5f, 3f).eyeHeight(1.5f).fireImmune().clientTrackingRange(8).updateInterval(20).build("thaumcraft:cultist_portal_greater"));
    public static final Supplier<EntityType<GiantTaintacle>> GIANT_TAINTACLE = TYPES.register("giant_taintacle", () -> EntityType.Builder.<GiantTaintacle>of(GiantTaintacle::new, MobCategory.MONSTER)
            .sized(1.1f, 6f).eyeHeight(5.1f).clientTrackingRange(12).updateInterval(3).build("thaumcraft:giant_taintacle"));
    public static final Supplier<EntityType<GolemOrb>> GOLEM_ORB = TYPES.register("eldritch_golem_orb", () -> EntityType.Builder.<GolemOrb>of(GolemOrb::new, MobCategory.MISC)
            .sized(.25f, .25f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_golem_orb"));

    public static final DeferredItem<SpawnEggItem> ELDRITCH_GOLEM_EGG = egg("eldritch_golem", ELDRITCH_GOLEM, 6842578, 0x888888);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_WARDEN_EGG = egg("eldritch_warden", ELDRITCH_WARDEN, 6842578, 0x808080);
    public static final DeferredItem<SpawnEggItem> CULTIST_LEADER_EGG = egg("cultist_leader", CULTIST_LEADER, 6842578, 9438728);
    public static final DeferredItem<SpawnEggItem> CULTIST_PORTAL_GREATER_EGG = egg("cultist_portal_greater", CULTIST_PORTAL_GREATER, 6842578, 32896);
    public static final DeferredItem<SpawnEggItem> GIANT_TAINTACLE_EGG = egg("giant_taintacle", GIANT_TAINTACLE, 6842578, 10618530);

    private static DeferredItem<SpawnEggItem> egg(String id, Supplier<? extends EntityType<? extends Mob>> type, int primary, int secondary) {
        return PortContent.ITEMS.registerItem(id + "_spawn_egg", properties -> new DeferredSpawnEggItem(type, primary, secondary, properties));
    }

    public static void register(IEventBus bus) {
        BossGear.register();
        BossLoot.register();
        TYPES.register(bus);
        bus.addListener(BossContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_GOLEM.get(), EldritchGolem.attributes().build());
        event.put(ELDRITCH_WARDEN.get(), EldritchWarden.attributes().build());
        event.put(CULTIST_LEADER.get(), CultistLeader.attributes().build());
        event.put(CULTIST_PORTAL_GREATER.get(), CultistPortalGreater.attributes().build());
        event.put(GIANT_TAINTACLE.get(), GiantTaintacle.attributes().build());
    }

    private BossContent() {}
}
