package thaumcraft.port.alchemy.liquid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;

public final class SanitySoapItem extends Item {
    public SanitySoapItem(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 100; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand); return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (AlchemyRules.finishedWashing(remaining)) entity.releaseUsingItem();
        if (level.isClientSide) AlchemyLiquidVisuals.soap.accept(entity, false);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!AlchemyRules.finishedWashing(remaining) || !(entity instanceof Player) || stack.isEmpty()) return;
        // Original soap is consumed even in creative, after 96 uninterrupted ticks.
        stack.shrink(1);
        if (entity instanceof ServerPlayer player) {
            var storage = ResearchStorage.get(player.getServer());
            var state = storage.player(player.getUUID());
            int strength = AlchemyRules.soapStrength(player.hasEffect(AlchemyLiquids.WARP_WARD),
                    level.getBlockState(player.blockPosition()).is(AlchemyLiquids.PURIFYING_BATH_BLOCK.get()));
            state.warp(state.permanentWarp(), Math.max(0, state.normalWarp() - strength), 0);
            storage.setDirty(); ResearchProgress.sync(player);
        } else if (level.isClientSide) AlchemyLiquidVisuals.soap.accept(entity, true);
    }
}
