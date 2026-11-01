package thaumcraft.port.alchemy.liquid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BottleTaintItem extends Item {
    public BottleTaintItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var projectile = AlchemyLiquids.BOTTLE_PROJECTILE.get().create(level);
            if (projectile == null) return InteractionResultHolder.fail(stack);
            projectile.setOwner(player); projectile.setItem(stack.copyWithCount(1));
            projectile.setPos(player.getX(), player.getEyeY() - .1, player.getZ());
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -5, .66f, 1);
            if (!level.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);
            if (!player.isCreative()) stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.EGG_THROW, SoundSource.PLAYERS,
                    .5f, .4f / (level.random.nextFloat() * .4f + .8f));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
