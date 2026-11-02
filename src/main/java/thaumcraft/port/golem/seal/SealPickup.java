package thaumcraft.port.golem.seal;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

/** One grounded item reservation per five ticks, then physical collection by a golem. */
public class SealPickup extends SealFiltered implements ISealConfigArea {
    private int delay = new Random().nextInt(100);
    private final Map<Integer,Integer> itemEntities = new HashMap<>();
    protected ISealConfigToggles.SealToggle[] props = {
        new ISealConfigToggles.SealToggle(true,"pmeta","golem.prop.meta"),
        new ISealConfigToggles.SealToggle(true,"pnbt","golem.prop.nbt"),
        new ISealConfigToggles.SealToggle(false,"pore","golem.prop.ore"),
        new ISealConfigToggles.SealToggle(false,"pmod","golem.prop.mod")};
    @Override public String getKey() { return "thaumcraft:pickup"; }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide || delay++ % 5 != 0) return;
        for (var item : level.getEntitiesOfClass(ItemEntity.class, GolemHelper.getBoundsForArea(seal))) {
            if (!item.isAlive() || !item.onGround() || item.hasPickUpDelay() || itemEntities.containsValue(item.getId())
                    || !SealInventory.matchesFilters(getInv(), isBlacklist(), item.getItem(), SealInventory.filters(props))) continue;
            var task = new Task(seal.getSealPos(), item); task.setPriority(seal.getPriority());
            itemEntities.put(task.getId(), item.getId()); TaskHandler.addTask(level, task); break;
        }
        itemEntities.entrySet().removeIf(e -> {
            var entity = level.getEntity(e.getValue());
            return entity == null || !entity.isAlive() || TaskHandler.getTask(level, e.getKey()) == null;
        });
    }
    protected ItemEntity getItemEntity(Level level, Task task) {
        var id = itemEntities.get(task.getId());
        return id != null && level.getEntity(id) instanceof ItemEntity item && item.isAlive() ? item : null;
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        var item = getItemEntity(level, task);
        if (item != null && SealInventory.matchesFilters(getInv(), isBlacklist(), item.getItem(), SealInventory.filters(props))) {
            var remainder = golem.holdItem(item.getItem());
            if (remainder.isEmpty()) item.discard(); else item.setItem(remainder);
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, .125f, (level.random.nextFloat() - level.random.nextFloat()) * 1.4f + 2);
            golem.swingArm();
        }
        task.setSuspended(true); itemEntities.remove(task.getId());
        if (golem instanceof GolemEntity entity) for (var next : TaskHandler.getEntityTasksSorted(level, null, entity)) {
            if (!itemEntities.containsKey(next.getId()) || !next.canGolemPerformTask(golem) || !entity.isWithinHome(next.getPos())) continue;
            entity.setTask(next); next.setReserved(true); level.broadcastEntityEvent(entity, (byte)5); break;
        }
        return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        var item = getItemEntity(golem.getGolemWorld(), task);
        return item != null && golem.canCarry(item.getItem(), true);
    }
    @Override public void onTaskSuspension(Level level, Task task) { itemEntities.remove(task.getId()); }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_pickup"); }
    @Override public int[] getGuiCategories() { return new int[]{2,1,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return null; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY}; }
}
