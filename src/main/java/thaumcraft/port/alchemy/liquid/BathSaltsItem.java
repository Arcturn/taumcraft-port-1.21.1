package thaumcraft.port.alchemy.liquid;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

public final class BathSaltsItem extends Item {
    public BathSaltsItem(Properties properties) { super(properties); }
    @Override public int getEntityLifespan(ItemStack stack, Level level) { return 200; }
    @Override public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // TC6's 1.12 items sank; modern buoyancy otherwise lifts salts out of the source
        // before their expiry event can dissolve them. Keep the original sinking behavior.
        if (entity.isInWater()) {
            var motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(motion.y, -.04), motion.z);
        }
        return false;
    }
}
