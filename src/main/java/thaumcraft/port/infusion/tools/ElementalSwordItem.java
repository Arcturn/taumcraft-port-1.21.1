package thaumcraft.port.infusion.tools;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortSounds;

public final class ElementalSwordItem extends SwordItem {
    public ElementalSwordItem(Properties properties) { super(InfusionTools.ELEMENTAL,properties); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity) { return 72000; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level,LivingEntity player,ItemStack stack,int remaining) {
        int ticks=72000-remaining;
        Vec3 velocity=player.getDeltaMovement();
        double lift=velocity.y;
        if (lift<0) { lift/=1.2f;player.fallDistance/=1.2f; }
        lift+=.08f;
        if (lift>.5) lift=.2f;
        player.setDeltaMovement(velocity.x,lift,velocity.z);
        if (player instanceof ServerPlayer server && server.connection!=null) server.connection.aboveGroundTickCount=0;
        for (var target:level.getEntities(player,player.getBoundingBox().inflate(2.5))) {
            if (!(target instanceof LivingEntity) || target instanceof Player || !target.isAlive() || target==player.getVehicle()) continue;
            Vec3 delta=target.position().subtract(player.position());
            target.setDeltaMovement(target.getDeltaMovement().add(delta.scale(1/(2.5*(delta.length()+.1)))));
            target.hasImpulse=true;
        }
        if (level.isClientSide) ElementalToolVisuals.vortex.accept(player);
        else if (ticks%20==0) {
            player.playSound(PortSounds.get("wind"),.5f,.9f+level.random.nextFloat()*.2f);
            stack.hurtAndBreak(1,player,LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
    }
}
