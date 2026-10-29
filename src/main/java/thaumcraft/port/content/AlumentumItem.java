package thaumcraft.port.content;

import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.world.WorldEntities;

public final class AlumentumItem extends Item {
    public AlumentumItem(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide){
            var projectile=WorldEntities.ALUMENTUM.get().create(level);if(projectile==null)return InteractionResultHolder.fail(stack);
            projectile.setOwner(player);projectile.setPos(player.getX(),player.getEyeY()-.1,player.getZ());projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),-5,.75f,2);
            if(!level.addFreshEntity(projectile))return InteractionResultHolder.fail(stack);
            if(!player.isCreative())stack.shrink(1);level.playSound(null,player.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.3f,.4f/(level.random.nextFloat()*.4f+.8f));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
