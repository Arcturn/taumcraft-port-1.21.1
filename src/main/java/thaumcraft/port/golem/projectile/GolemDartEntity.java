package thaumcraft.port.golem.projectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
/** Original miniature arrow projectile; mob-fired darts cannot be recovered by players. */
public final class GolemDartEntity extends AbstractArrow {
    public GolemDartEntity(EntityType<? extends GolemDartEntity> type, Level level) { super(type, level); pickup = Pickup.DISALLOWED; }
    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }
}
