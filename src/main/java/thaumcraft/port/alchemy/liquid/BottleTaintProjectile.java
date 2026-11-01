package thaumcraft.port.alchemy.liquid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.world.TaintSeed;
import thaumcraft.port.world.ThaumicSlime;
import thaumcraft.port.world.WorldContent;
import thaumcraft.port.world.WorldEntities;

public final class BottleTaintProjectile extends ThrowableItemProjectile {
    public BottleTaintProjectile(EntityType<? extends BottleTaintProjectile> type, Level level) { super(type, level); }
    @Override protected Item getDefaultItem() { return AlchemyLiquids.BOTTLE_TAINT.get(); }
    @Override protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) splash();
    }
    /** The original splash is a ten-block cube, not vanilla potion distance falloff. */
    public void splash() {
        if (level().isClientSide || isRemoved()) return;
        var box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(5);
        for (var living : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (living instanceof TaintSeed || living instanceof ThaumicSlime || living.getType().is(EntityTypeTags.UNDEAD)) continue;
            living.addEffect(new MobEffectInstance(WorldEntities.TAINT, 100, 0, false, true));
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = (int)((random.nextFloat() - random.nextFloat()) * 4);
            int dz = (int)((random.nextFloat() - random.nextFloat()) * 4);
            BlockPos pos = blockPosition().offset(dx, 0, dz);
            if (level().random.nextBoolean() && !placeGoo(pos)) placeGoo(pos.below());
        }
        level().broadcastEntityEvent(this, (byte)3); discard();
    }
    private boolean placeGoo(BlockPos pos) {
        if (!level().getBlockState(pos.below()).isFaceSturdy(level(), pos.below(), Direction.UP)
                || !level().getBlockState(pos).canBeReplaced()) return false;
        return level().setBlockAndUpdate(pos, WorldContent.block("flux_goo").defaultBlockState());
    }
    @Override public void handleEntityEvent(byte id) {
        if (id == 3) AlchemyLiquidVisuals.bottle.accept(this); else super.handleEntityEvent(id);
    }
}
