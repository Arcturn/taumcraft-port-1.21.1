package thaumcraft.port.golem.seal;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
    private int delay = new Random().nextInt(42);
    private final Map<Integer,Long> cache = new HashMap<>();
    private final Map<Integer,Integer> breakers = new HashMap<>();
    protected SealToggle[] props = {new SealToggle(true,"pmeta","golem.prop.meta")};
    @Override public String getKey() { return "thaumcraft:breaker"; }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide) return;
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level,id)==null);
        var pos = GolemHelper.getPosInArea(seal,++delay);
        if (cache.containsValue(pos.asLong()) || !isValidBlock(level,pos)) return;
        var task = new Task(seal.getSealPos(),pos); task.setPriority(seal.getPriority());
        task.setData((int)(level.getBlockState(pos).getDestroySpeed(level,pos)*10));
        TaskHandler.addTask(level,task); cache.put(task.getId(),pos.asLong());
    }
    public boolean isValidBlock(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level,pos)<0) return false;
        var blockItem = new ItemStack(state.getBlock());
        var matching = new SealInventory.Filter(!props[0].value,true,false,false);
        // Preserve the original AND semantics of the advanced whitelist.
        for (var template : getInv()) if (!template.isEmpty()) {
            boolean match = SealInventory.matches(template,blockItem,matching);
            if (isBlacklist() ? match : !match) return false;
        }
        return true;
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && isValidBlock(level,task.getPos())) {
            var pos = task.getPos(); var state = level.getBlockState(pos); golem.swingArm();
            boolean silk = props.length>1 && props[1].value; int speed = silk ? 7 : 21;
            if (task.getData()>speed) {
                task.setLifespan((short)Math.max(task.getLifespan(),10)); task.setData(task.getData()-speed);
                var sound = state.getSoundType(level,pos,golem.getGolemEntity());
                level.playSound(null,pos,sound.getHitSound(),SoundSource.BLOCKS,(sound.getVolume()+.7f)/8,sound.getPitch()*.5f);
                int id = golem.getGolemEntity().getId(); breakers.put(task.getId(),id);
                level.destroyBlockProgress(id,pos,(int)(9*(1-task.getData()/(state.getDestroySpeed(level,pos)*10))));
                return false;
            }
            level.destroyBlockProgress(golem.getGolemEntity().getId(),pos,-1);
            if (GolemInteractionHelper.harvestBlock(level,golem,pos,true,silk)) golem.addRankXp(1);
        }
        onTaskSuspension(level,task); task.setSuspended(true); return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && isValidBlock(golem.getGolemWorld(),task.getPos())) return true;
        task.setSuspended(true); return false;
    }
    @Override public void onTaskSuspension(Level level, Task task) {
        cache.remove(task.getId()); var id=breakers.remove(task.getId());
        if (id!=null) level.destroyBlockProgress(id,task.getPos(),-1);
    }
    @Override public void onRemoval(Level level, BlockPos pos, Direction face) {
        for (var entry : breakers.entrySet()) {
            var location=cache.get(entry.getKey()); if (location!=null) level.destroyBlockProgress(entry.getValue(),BlockPos.of(location),-1);
        }
        breakers.clear(); cache.clear();
    }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_breaker"); }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{2,1,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.BREAKER}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
}
