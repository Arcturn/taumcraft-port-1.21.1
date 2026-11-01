package thaumcraft.port.alchemy.liquid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.port.content.PortContent;

/** BETA26 alchemical liquids, their containers and cleansing consumables. */
public final class AlchemyLiquids {
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "thaumcraft");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "thaumcraft");
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "thaumcraft");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "thaumcraft");

    public static final ResourceKey<DamageType> DISSOLVE = ResourceKey.create(Registries.DAMAGE_TYPE, id("dissolve"));
    public static final int DEATH_COLOUR = -263978855, BATH_COLOUR = 2013252778;
    public static final DeferredHolder<FluidType, FluidType> DEATH_TYPE = FLUID_TYPES.register("liquid_death",
            () -> new FluidType(typeProperties("liquid_death").viscosity(1500)) {});
    public static final DeferredHolder<FluidType, FluidType> BATH_TYPE = FLUID_TYPES.register("purifying_fluid",
            () -> new FluidType(typeProperties("purifying_fluid").lightLevel(5)) {});
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> DEATH = FLUIDS.register("liquid_death",
            () -> new BaseFlowingFluid.Source(deathProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_DEATH = FLUIDS.register("flowing_liquid_death",
            () -> new BaseFlowingFluid.Flowing(deathProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PURIFYING_BATH = FLUIDS.register("purifying_fluid",
            () -> new BaseFlowingFluid.Source(bathProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_PURIFYING_BATH = FLUIDS.register("flowing_purifying_fluid",
            () -> new BaseFlowingFluid.Flowing(bathProperties()));

    public static final DeferredBlock<AlchemyFluidBlock> DEATH_BLOCK = PortContent.BLOCKS.register("liquid_death",
            () -> new AlchemyFluidBlock(DEATH.get(), true, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
    public static final DeferredBlock<AlchemyFluidBlock> PURIFYING_BATH_BLOCK = PortContent.BLOCKS.register("purifying_fluid",
            () -> new AlchemyFluidBlock(PURIFYING_BATH.get(), false,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(state -> 5)));
    public static final DeferredItem<BucketItem> DEATH_BUCKET = PortContent.ITEMS.register("bucket_death",
            () -> new BucketItem(DEATH.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.RARE)));
    // TC6 used Forge's universal bucket for the bath fluid. A dedicated bucket is its modern equivalent.
    public static final DeferredItem<BucketItem> PURIFYING_BUCKET = PortContent.ITEMS.register("bucket_purifying",
            () -> new BucketItem(PURIFYING_BATH.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.RARE)));
    public static final DeferredItem<BathSaltsItem> BATH_SALTS = PortContent.ITEMS.register("bath_salts", () -> new BathSaltsItem(new Item.Properties()));
    public static final DeferredItem<SanitySoapItem> SANITY_SOAP = PortContent.ITEMS.register("sanity_soap", () -> new SanitySoapItem(new Item.Properties()));
    public static final DeferredItem<BottleTaintItem> BOTTLE_TAINT = PortContent.ITEMS.register("bottle_taint", () -> new BottleTaintItem(new Item.Properties().stacksTo(8)));
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = EFFECTS.register("warp_ward",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 14742263) {});
    public static final DeferredHolder<EntityType<?>, EntityType<BottleTaintProjectile>> BOTTLE_PROJECTILE = ENTITIES.register("bottle_taint",
            () -> EntityType.Builder.<BottleTaintProjectile>of(BottleTaintProjectile::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(10).build("thaumcraft:bottle_taint"));

    private static FluidType.Properties typeProperties(String name) {
        return FluidType.Properties.create().descriptionId("block.thaumcraft." + name).rarity(Rarity.RARE)
                .canDrown(false)
                .canConvertToSource(false).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }
    private static BaseFlowingFluid.Properties deathProperties() {
        // Modern fluids have eight levels: dropping two per block preserves TC6's four quanta.
        return new BaseFlowingFluid.Properties(DEATH_TYPE, DEATH, FLOWING_DEATH).block(DEATH_BLOCK)
                .bucket(DEATH_BUCKET).levelDecreasePerBlock(2).slopeFindDistance(4).tickRate(7);
    }
    private static BaseFlowingFluid.Properties bathProperties() {
        return new BaseFlowingFluid.Properties(BATH_TYPE, PURIFYING_BATH, FLOWING_PURIFYING_BATH)
                .block(PURIFYING_BATH_BLOCK).bucket(PURIFYING_BUCKET).tickRate(5);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus); FLUIDS.register(bus); EFFECTS.register(bus); ENTITIES.register(bus);
    }
    private AlchemyLiquids() {}
}
