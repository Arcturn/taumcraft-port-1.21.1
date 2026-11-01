package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Extended infusion stabilization values used by the altar's symmetry scan. */
public interface InfusionStabilizer {
    default boolean canStabilise(Level level, BlockPos pos) { return true; }
    float stabilization(Level level, BlockPos pos);
    default boolean symmetryPenalty(Level level, BlockPos first, BlockPos opposite) { return false; }
    default float penalty(Level level, BlockPos pos) { return 0; }
}
