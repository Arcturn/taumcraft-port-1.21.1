package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Client callbacks keep particle and Minecraft classes out of dedicated-server class loading. */
public final class StructureEffects {
    @FunctionalInterface public interface EventEffect { void accept(BlockEntity entity, int event, int parameter); }
    @FunctionalInterface public interface AmbientEffect { void accept(Level level, BlockPos pos, int charge, boolean pedestal); }
    public static EventEffect events = (entity, event, parameter) -> {};
    public static AmbientEffect ambient = (level, pos, charge, pedestal) -> {};
    public static void blockEvent(BlockEntity entity, int event, int parameter) { events.accept(entity, event, parameter); }
    private StructureEffects() {}
}
