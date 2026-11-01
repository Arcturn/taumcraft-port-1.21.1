package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import thaumcraft.port.alchemy.liquid.AlchemyLiquidVisuals;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;
import thaumcraft.port.alchemy.liquid.BottleTaintProjectile;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.content.PortSounds;

@EventBusSubscriber(modid = "thaumcraft", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AlchemyLiquidClient {
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(fluid("thaumcraft:blocks/animatedglow", "thaumcraft:blocks/animatedglow", AlchemyLiquids.DEATH_COLOUR), AlchemyLiquids.DEATH_TYPE);
        event.registerFluidType(fluid("minecraft:block/water_still", "minecraft:block/water_flow", AlchemyLiquids.BATH_COLOUR), AlchemyLiquids.BATH_TYPE);
    }
    private static IClientFluidTypeExtensions fluid(String still, String flowing, int colour) {
        return new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.parse(still); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.parse(flowing); }
            @Override public int getTintColor() { return colour; }
        };
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AlchemyLiquids.BOTTLE_PROJECTILE.get(), ThrownItemRenderer::new);
    }
    @SubscribeEvent public static void colours(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), AlchemyLiquids.DEATH_BUCKET.get(), AlchemyLiquids.PURIFYING_BUCKET.get());
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var fluid : java.util.List.of(AlchemyLiquids.DEATH, AlchemyLiquids.FLOWING_DEATH,
                    AlchemyLiquids.PURIFYING_BATH, AlchemyLiquids.FLOWING_PURIFYING_BATH))
                ItemBlockRenderTypes.setRenderLayer(fluid.get(), RenderType.translucent());
            AlchemyLiquidVisuals.fluid = AlchemyLiquidClient::fluidTick;
            AlchemyLiquidVisuals.soap = AlchemyLiquidClient::soap;
            AlchemyLiquidVisuals.bottle = AlchemyLiquidClient::bottle;
        });
    }
    private static void fluidTick(Level world, BlockPos pos, int amount, boolean death) {
        if (!(world instanceof ClientLevel level)) return;
        var r = level.random;
        if (r.nextInt(death ? 20 : 10) == 0) {
            double x = pos.getX() + r.nextFloat(), z = pos.getZ() + r.nextFloat();
            if (death) Minecraft.getInstance().particleEngine.add(AlchemyLiquidParticle.bubble(level, x,
                    pos.getY() + .1 + .1125 * amount, z));
            else {
                var preset = new CrucibleVisualRules.Bubble(10 + r.nextInt(10), (.3f + r.nextFloat() * .3f) * .1f,
                        -.01f, .001f, 64, new int[]{65,66}, true, .25f, true);
                bubble(level, x, pos.getY() + .125 * amount, z, preset, 1, 1, 1);
            }
        }
        if (r.nextInt(50) == 0) level.playLocalSound(pos.getX() + r.nextFloat(), pos.getY() + .75,
                pos.getZ() + r.nextFloat(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                .1f + r.nextFloat() * .1f, .9f + r.nextFloat() * .15f, false);
    }
    private static void soap(LivingEntity entity, boolean finished) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        var r = level.random;
        if (finished) level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), PortSounds.get("craftstart"), SoundSource.PLAYERS, .25f, 1, false);
        else if (r.nextFloat() < .2f) level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_SWIM, SoundSource.PLAYERS, .1f, 1.5f + r.nextFloat() * .2f, false);
        for (int i = 0; i < (finished ? 40 : 10); i++) {
            float spread = finished ? 1.5f : 1;
            bubble(level, entity.getX() - .5 + r.nextFloat() * spread,
                    entity.getBoundingBox().minY + r.nextFloat() * entity.getBbHeight(),
                    entity.getZ() - .5 + r.nextFloat() * spread,
                    CrucibleVisualRules.bubble(r, CrucibleVisualRules.Kind.BUBBLE, 1), 1, finished ? .7f : .8f, .9f);
        }
    }
    private static void bubble(ClientLevel level, double x, double y, double z, CrucibleVisualRules.Bubble preset,
                               float red, float green, float blue) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("crucible_bubble"), x, y, z, 0, 0, 0);
        if (particle instanceof SalisMundusParticle bubble) bubble.crucible(preset, red, green, blue);
    }
    private static void bottle(BottleTaintProjectile entity) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        for (int i = 0; i < 100; i++) Minecraft.getInstance().particleEngine.add(AlchemyLiquidParticle.taint(level,
                entity.getX(), entity.getY() + level.random.nextFloat() * entity.getBbHeight(), entity.getZ()));
        var shard = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(AlchemyLiquids.BOTTLE_TAINT.get()));
        for (int i = 0; i < 8; i++) level.addParticle(shard, entity.getX(), entity.getY(), entity.getZ(),
                level.random.nextGaussian() * .15, level.random.nextDouble() * .2, level.random.nextGaussian() * .15);
        level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SPLASH_POTION_BREAK,
                SoundSource.NEUTRAL, 1, level.random.nextFloat() * .1f + .9f, false);
    }
    private AlchemyLiquidClient() {}
}
