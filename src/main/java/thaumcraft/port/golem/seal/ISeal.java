package thaumcraft.port.golem.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;

/** BETA26 seal behavior. Each placed seal owns its own behavior instance. */
public interface ISeal {
    String getKey();
    boolean canPlaceAt(Level level, BlockPos pos, Direction face);
    void tickSeal(Level level, ISealEntity seal);
    default void onTaskStarted(Level level, IGolemAPI golem, Task task) { }
    boolean onTaskCompletion(Level level, IGolemAPI golem, Task task);
    default void onTaskSuspension(Level level, Task task) { }
    boolean canGolemPerformTask(IGolemAPI golem, Task task);
    default void readCustomNBT(CompoundTag tag) { }
    default void writeCustomNBT(CompoundTag tag) { }
    default void readCustomNBT(CompoundTag tag, HolderLookup.Provider registries) { readCustomNBT(tag); }
    default void writeCustomNBT(CompoundTag tag, HolderLookup.Provider registries) { writeCustomNBT(tag); }
    ResourceLocation getSealIcon();
    default void onRemoval(Level level, BlockPos pos, Direction face) { }
    EnumGolemTrait[] getRequiredTags();
    EnumGolemTrait[] getForbiddenTags();
}
