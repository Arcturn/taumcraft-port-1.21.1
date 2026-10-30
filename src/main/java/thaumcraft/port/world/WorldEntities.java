package thaumcraft.port.world;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
public final class WorldEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,"thaumcraft");
    public static final DeferredHolder<EntityType<?>,EntityType<thaumcraft.port.machine.SpecialItemEntity>> SPECIAL_ITEM=TYPES.register("special_item",()->EntityType.Builder.<thaumcraft.port.machine.SpecialItemEntity>of(thaumcraft.port.machine.SpecialItemEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(20).build("thaumcraft:special_item"));
    public static final DeferredHolder<EntityType<?>,EntityType<AlumentumProjectile>> ALUMENTUM=TYPES.register("alumentum",()->EntityType.Builder.<AlumentumProjectile>of(AlumentumProjectile::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build("thaumcraft:alumentum"));
    public static final DeferredHolder<EntityType<?>,EntityType<CultistPortal>> PORTAL=TYPES.register("cultist_portal_lesser",()->EntityType.Builder.<CultistPortal>of(CultistPortal::new,MobCategory.MONSTER).sized(1.5f,3).fireImmune().clientTrackingRange(8).build("thaumcraft:cultist_portal_lesser"));
    public static final DeferredHolder<EntityType<?>,EntityType<Cultist>> KNIGHT=TYPES.register("cultist_knight",()->EntityType.Builder.<Cultist>of(Cultist::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(8).build("thaumcraft:cultist_knight"));
    public static final DeferredHolder<EntityType<?>,EntityType<Cultist>> CLERIC=TYPES.register("cultist_cleric",()->EntityType.Builder.<Cultist>of(Cultist::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(8).build("thaumcraft:cultist_cleric"));
    public static final DeferredHolder<EntityType<?>,EntityType<CultistOrb>> ORB=TYPES.register("cultist_orb",()->EntityType.Builder.<CultistOrb>of(CultistOrb::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(8).build("thaumcraft:cultist_orb"));
    public static final DeferredHolder<EntityType<?>,EntityType<FluxRift>> RIFT=TYPES.register("flux_rift",()->EntityType.Builder.<FluxRift>of(FluxRift::new,MobCategory.MISC).sized(1,1).fireImmune().clientTrackingRange(8).updateInterval(20).build("thaumcraft:flux_rift"));
    public static final DeferredHolder<EntityType<?>,EntityType<TaintSeed>> SEED=TYPES.register("taint_seed",()->EntityType.Builder.<TaintSeed>of(TaintSeed::new,MobCategory.MONSTER).sized(1.5f,1.25f).clientTrackingRange(8).build("thaumcraft:taint_seed"));
    public static final DeferredHolder<EntityType<?>,EntityType<TaintSeed>> PRIME=TYPES.register("taint_seed_prime",()->EntityType.Builder.<TaintSeed>of(TaintSeed::new,MobCategory.MONSTER).sized(2,2).clientTrackingRange(8).build("thaumcraft:taint_seed_prime"));
    public static final DeferredHolder<EntityType<?>,EntityType<Wisp>> WISP=TYPES.register("wisp",()->EntityType.Builder.<Wisp>of(Wisp::new,MobCategory.MONSTER).sized(.9f,.9f).fireImmune().clientTrackingRange(8).build("thaumcraft:wisp"));
    public static final DeferredHolder<EntityType<?>,EntityType<ThaumicSlime>> SLIME=TYPES.register("thaumic_slime",()->EntityType.Builder.<ThaumicSlime>of(ThaumicSlime::new,MobCategory.MONSTER).sized(.52f,.52f).clientTrackingRange(8).build("thaumcraft:thaumic_slime"));
    public static final DeferredHolder<MobEffect,MobEffect> TAINT=EFFECTS.register("flux_taint",()->new FluxEffect(true));
    public static final DeferredHolder<MobEffect,MobEffect> INFECTIOUS=EFFECTS.register("infectious_vis_exhaust",()->new FluxEffect(false));
    public static final DeferredHolder<MobEffect,MobEffect> VIS_EXHAUST=EFFECTS.register("vis_exhaust",()->new MobEffect(MobEffectCategory.HARMFUL,0x604482){});
    public static void register(IEventBus bus){TYPES.register(bus);EFFECTS.register(bus);bus.addListener(WorldEntities::attributes);bus.addListener(WorldEntities::placements);}
    private static void placements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e){e.register(WISP.get(),SpawnPlacementTypes.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Wisp::canSpawn,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    private static void attributes(EntityAttributeCreationEvent e){e.put(PORTAL.get(),CultistPortal.attributes().build());e.put(KNIGHT.get(),Cultist.attributes(false).build());e.put(CLERIC.get(),Cultist.attributes(true).build());e.put(SLIME.get(),net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityType.SLIME));e.put(SEED.get(),TaintSeed.attributes(false).build());e.put(PRIME.get(),TaintSeed.attributes(true).build());e.put(WISP.get(),Wisp.attributes().build());}
}
