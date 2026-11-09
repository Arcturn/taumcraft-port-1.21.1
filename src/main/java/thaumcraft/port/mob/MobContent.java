package thaumcraft.port.mob;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.world.WorldConfig;

/** Ordinary and tainted TC6 mobs. Structure encounters register their own spawn sources. */
public final class MobContent {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "thaumcraft");

    public static final Supplier<EntityType<BrainyZombie>> BRAINY_ZOMBIE = type("brainy_zombie", BrainyZombie::new, .6f, 1.95f, 1.74f);
    public static final Supplier<EntityType<GiantBrainyZombie>> GIANT_BRAINY_ZOMBIE = type("giant_brainy_zombie", GiantBrainyZombie::new, .6f, 1.95f, 1.74f);
    public static final Supplier<EntityType<EldritchCrab>> ELDRITCH_CRAB = type("eldritch_crab", EldritchCrab::new, .8f, .6f, .3f);
    public static final Supplier<EntityType<InhabitedZombie>> INHABITED_ZOMBIE = type("inhabited_zombie", InhabitedZombie::new, .6f, 1.95f, 1.74f);
    public static final Supplier<EntityType<TaintCrawler>> CRAWLER = type("taint_crawler", TaintCrawler::new, .5f, .4f, .1f);
    public static final Supplier<EntityType<TaintSwarm>> SWARM = type("taint_swarm", TaintSwarm::new, 2f, 2f, 1.7f);
    public static final Supplier<EntityType<Taintacle>> TAINTACLE = type("taintacle", Taintacle::new, .8f, 3f, 2.55f);
    public static final Supplier<EntityType<TaintacleSmall>> TAINTACLE_SMALL = type("taintacle_small", TaintacleSmall::new, .22f, 1f, .85f);

    public static final DeferredItem<SpawnEggItem> BRAINY_ZOMBIE_EGG = egg("brainy_zombie", BRAINY_ZOMBIE, -16129, -16744448);
    public static final DeferredItem<SpawnEggItem> GIANT_BRAINY_ZOMBIE_EGG = egg("giant_brainy_zombie", GIANT_BRAINY_ZOMBIE, -16129, -16760832);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_CRAB_EGG = egg("eldritch_crab", ELDRITCH_CRAB, 0x808080, 0x550000);
    public static final DeferredItem<SpawnEggItem> INHABITED_ZOMBIE_EGG = egg("inhabited_zombie", INHABITED_ZOMBIE, 0x808080, 0x550000);
    public static final DeferredItem<SpawnEggItem> CRAWLER_EGG = egg("taint_crawler", CRAWLER, 10618530, 0x303030);
    public static final DeferredItem<SpawnEggItem> SWARM_EGG = egg("taint_swarm", SWARM, 10618530, 0xFF8080);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_EGG = egg("taintacle", TAINTACLE, 10618530, 0x443344);

    private static <T extends Monster> Supplier<EntityType<T>> type(String id, EntityType.EntityFactory<T> factory, float width, float height, float eyeHeight) {
        return TYPES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).eyeHeight(eyeHeight).clientTrackingRange(8).updateInterval(3).build("thaumcraft:" + id));
    }

    private static DeferredItem<SpawnEggItem> egg(String id, Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int primary, int secondary) {
        return PortContent.ITEMS.registerItem(id + "_spawn_egg", p -> new DeferredSpawnEggItem(type, primary, secondary, p));
    }

    public static void register(IEventBus bus) {
        MobArmor.register();
        TYPES.register(bus);
        bus.addListener(MobContent::attributes);
        bus.addListener(MobContent::placements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(BRAINY_ZOMBIE.get(), BrainyZombie.attributes().build());
        event.put(GIANT_BRAINY_ZOMBIE.get(), GiantBrainyZombie.attributes().build());
        event.put(ELDRITCH_CRAB.get(), EldritchCrab.attributes().build());
        event.put(INHABITED_ZOMBIE.get(), InhabitedZombie.attributes().build());
        event.put(CRAWLER.get(), TaintCrawler.attributes().build());
        event.put(SWARM.get(), TaintSwarm.attributes().build());
        event.put(TAINTACLE.get(), Taintacle.attributes().build());
        event.put(TAINTACLE_SMALL.get(), TaintacleSmall.attributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION || WorldConfig.BRAINY_ZOMBIES.get())
                        && Monster.checkMonsterSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private MobContent() {}
}
