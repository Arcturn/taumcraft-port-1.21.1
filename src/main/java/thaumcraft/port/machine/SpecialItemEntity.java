package thaumcraft.port.machine;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.world.WorldEntities;

/** TC6's floating, fire-resistant crafted item, kept separate from crucible inputs. */
public final class SpecialItemEntity extends ItemEntity {
    public static final String CRUCIBLE_OUTPUT_TAG = "thaumcraft_crucible_output";

    public SpecialItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        // Keep the old port's marker so previously saved output entities remain protected too.
        addTag(CRUCIBLE_OUTPUT_TAG);
    }

    public SpecialItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(WorldEntities.SPECIAL_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setDeltaMovement((random.nextFloat() * .2f) - .1f, .2f, (random.nextFloat() * .2f) - .1f);
        lifespan = stack.getEntityLifespan(level);
    }

    @Override
    public void tick() {
        // The world increments tickCount before calling tick(), as it did in 1.12.2.
        if (tickCount <= 1) return;
        Vec3 motion = getDeltaMovement();
        // TC6 damps the initial rise and cancels ordinary item gravity before its item tick.
        setDeltaMovement(motion.x, (motion.y > 0 ? motion.y * .9f : motion.y) + .04, motion.z);
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_FIRE) && super.hurt(source, amount);
    }
}
