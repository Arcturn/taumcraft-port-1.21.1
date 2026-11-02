package thaumcraft.port.auromancy.firebat;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FireBatContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> MODIFIERS=DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,"thaumcraft");
    public static final DeferredHolder<EntityType<?>,EntityType<FireBat>> FIREBAT=ENTITIES.register("firebat",()->EntityType.Builder.<FireBat>of(FireBat::new,MobCategory.MONSTER).sized(.5f,.9f).fireImmune().clientTrackingRange(8).updateInterval(3).build("thaumcraft:firebat"));
    public static final Supplier<MapCodec<FireBatSpawns>> SPAWNS=MODIFIERS.register("firebat_spawns",()->MapCodec.unit(new FireBatSpawns()));
    public static void register(IEventBus bus){ENTITIES.register(bus);MODIFIERS.register(bus);bus.addListener(FireBatContent::attributes);bus.addListener(FireBatContent::placements);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(FIREBAT.get(),FireBat.attributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent event){event.register(FIREBAT.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,FireBat::canSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    private FireBatContent(){}
}
