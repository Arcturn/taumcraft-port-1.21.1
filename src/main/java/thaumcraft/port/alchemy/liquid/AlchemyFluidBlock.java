package thaumcraft.port.alchemy.liquid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import thaumcraft.port.research.ResearchStorage;

public final class AlchemyFluidBlock extends LiquidBlock {
    private final boolean death;
    public AlchemyFluidBlock(FlowingFluid fluid, boolean death, Properties properties) {
        super(fluid, properties); this.death = death;
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int amount = state.getFluidState().getAmount();
        double slowdown = 1 - amount / 16.0;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(slowdown, 1, slowdown));
        if (level.isClientSide) return;
        if (death && entity instanceof LivingEntity living) {
            living.hurt(new DamageSource(level.registryAccess().holderOrThrow(AlchemyLiquids.DISSOLVE)), 1 + amount / 2);
        } else if (!death && state.getFluidState().isSource() && entity instanceof ServerPlayer player
                && !player.hasEffect(AlchemyLiquids.WARP_WARD)) {
            int warp = ResearchStorage.get(player.getServer()).player(player.getUUID()).permanentWarp();
            player.addEffect(new MobEffectInstance(AlchemyLiquids.WARP_WARD, AlchemyRules.wardDuration(warp), 0, true, true));
            // removeBlock retains the contained fluid in modern Minecraft.
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        AlchemyLiquidVisuals.fluid.tick(level, pos, state.getFluidState().getAmount(), death);
    }
}
