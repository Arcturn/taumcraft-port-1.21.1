package thaumcraft.port.golem.seal;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealLumber implements ISeal, ISealGui, ISealConfigArea {
    private int delay = new Random().nextInt(33);
    private final Map<Integer,Long> cache = new HashMap<>();
    @Override public String getKey() { return "thaumcraft:lumber"; }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide) return;
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level,id)==null);
        var pos = GolemHelper.getPosInArea(seal,++delay);
        if (cache.containsValue(pos.asLong()) || !level.hasChunkAt(pos) || !level.getBlockState(pos).is(BlockTags.LOGS)) return;
        var task = new Task(seal.getSealPos(),pos); task.setPriority(seal.getPriority());
        TaskHandler.addTask(level,task); cache.put(task.getId(),pos.asLong());
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && level.hasChunkAt(task.getPos()) && level.getBlockState(task.getPos()).is(BlockTags.LOGS)) {
            golem.swingArm();
            if (GolemInteractionHelper.breakFurthestBlock(level,golem,task.getPos())) {
                task.setLifespan((short)Math.max(task.getLifespan(),10)); golem.addRankXp(1); return false;
            }
        }
        cache.remove(task.getId()); task.setSuspended(true); return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && golem.getGolemWorld().hasChunkAt(task.getPos()) && golem.getGolemWorld().getBlockState(task.getPos()).is(BlockTags.LOGS)) return true;
        task.setSuspended(true); return false;
    }
    @Override public void onTaskSuspension(Level level, Task task) { cache.remove(task.getId()); }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_lumber"); }
    @Override public int[] getGuiCategories() { return new int[]{2,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.BREAKER,EnumGolemTrait.SMART}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
}
