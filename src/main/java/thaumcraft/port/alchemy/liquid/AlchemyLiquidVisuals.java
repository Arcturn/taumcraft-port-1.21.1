package thaumcraft.port.alchemy.liquid;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Client-installed callbacks keep rendering classes absent from a dedicated server's class path. */
public final class AlchemyLiquidVisuals {
    @FunctionalInterface public interface FluidTick { void tick(Level level, BlockPos pos, int amount, boolean death); }
    public static FluidTick fluid = (level, pos, amount, death) -> {};
    public static BiConsumer<LivingEntity, Boolean> soap = (entity, finished) -> {};
    public static Consumer<BottleTaintProjectile> bottle = entity -> {};
    private AlchemyLiquidVisuals() {}
}
