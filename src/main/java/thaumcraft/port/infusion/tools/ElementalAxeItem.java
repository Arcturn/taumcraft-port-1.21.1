package thaumcraft.port.infusion.tools;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ElementalAxeItem extends AxeItem {
    public ElementalAxeItem(Properties properties) { super(InfusionTools.ELEMENTAL,properties); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level, LivingEntity player, ItemStack stack, int remaining) {
        for (var item : level.getEntitiesOfClass(ItemEntity.class,
                new AABB(player.getX(),player.getY(),player.getZ(),player.getX(),player.getY(),player.getZ()).inflate(10),
                ItemEntity::isAlive)) {
            Vec3 delta = new Vec3(item.getX()-player.getX(),item.getY()-player.getY()+player.getBbHeight()/2,item.getZ()-player.getZ());
            double length=delta.length();
            if (length>1.0e-9) {
                Vec3 velocity=item.getDeltaMovement().subtract(delta.scale(.3/length)).add(0,.1,0);
                item.setDeltaMovement(Mth.clamp(velocity.x,-.25,.25),Mth.clamp(velocity.y,-.25,.25),Mth.clamp(velocity.z,-.25,.25));
                item.hasImpulse=true;
            }
            if (level.isClientSide) ElementalToolVisuals.attraction.accept(item);
        }
    }
}
