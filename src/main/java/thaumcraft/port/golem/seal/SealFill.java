package thaumcraft.port.golem.seal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

/** Delivers carried stacks to a sided inventory, or onto the selected block face. */
public class SealFill extends SealFiltered {
    private int delay = new Random().nextInt(50);
    private int watchedTask = Integer.MIN_VALUE;
    protected ISealConfigToggles.SealToggle[] props = {
        new ISealConfigToggles.SealToggle(true,"pmeta","golem.prop.meta"),
        new ISealConfigToggles.SealToggle(true,"pnbt","golem.prop.nbt"),
        new ISealConfigToggles.SealToggle(false,"pore","golem.prop.ore"),
        new ISealConfigToggles.SealToggle(false,"pmod","golem.prop.mod"),
        new ISealConfigToggles.SealToggle(false,"pexist","golem.prop.exist")};
    @Override public String getKey() { return "thaumcraft:fill"; }
    private void createTask(Level level, ISealEntity seal) {
        var task = new Task(seal.getSealPos(), seal.getSealPos().pos); task.setPriority(seal.getPriority());
        TaskHandler.addTask(level, task); watchedTask = task.getId();
    }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide || delay++ % 20 != 0) return;
        var old = TaskHandler.getTask(level, watchedTask);
        if (old == null || old.isReserved() || old.isSuspended() || old.isCompleted()) createTask(level, seal);
    }
    @Override public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        var seal = SealHandler.getSealEntity(level, task.getSealPos());
        if (seal != null && !seal.isStoppedByRedstone(level)) createTask(level, seal);
    }
    private int deliveryAmount(IGolemAPI golem, Task task, SealInventory.Match match) {
        var stack = match.stack(); if (stack.isEmpty()) return 0;
        var level = golem.getGolemWorld(); var pos = task.getSealPos();
        if (pos == null || !level.hasChunkAt(pos.pos)) return 0;
        var inventory = SealInventory.handler(level, pos.pos, pos.face); var filter = SealInventory.filters(props);
        int existing = inventory == null ? SealInventory.countInWorld(level, pos.pos, stack, 1.5, filter) : SealInventory.count(inventory, stack, filter);
        if (inventory != null && props[4].value && existing <= 0) return 0;
        int amount = stack.getCount();
        if (hasStacksizeLimiters() && match.limit() > 0) amount = Math.min(amount, Math.max(0, match.limit() - existing));
        if (inventory != null) amount = Math.min(amount, SealInventory.availableRoom(inventory, stack.copyWithCount(amount)));
        return amount;
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        var match = SealInventory.firstMatch(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), SealInventory.filters(props));
        int amount = deliveryAmount(golem, task, match);
        if (amount > 0) {
            var pos = task.getSealPos(); var inventory = SealInventory.handler(level, pos.pos, pos.face);
            var removed = golem.dropItem(match.stack().copyWithCount(amount));
            ItemStack remainder = inventory == null
                    ? (SealInventory.eject(level, SealInventory.output(pos), removed) ? ItemStack.EMPTY : removed)
                    : SealInventory.insert(inventory, removed, false);
            if (!remainder.isEmpty()) golem.holdItem(remainder);
            if (remainder.getCount() < removed.getCount()) {
                golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, .125f, (level.random.nextFloat() - level.random.nextFloat()) * .7f + 1);
                golem.addRankXp(1); golem.swingArm();
            }
        }
        task.setSuspended(true); return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        var match = SealInventory.firstMatch(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), SealInventory.filters(props));
        return deliveryAmount(golem, task, match) > 0;
    }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_fill"); }
    @Override public int[] getGuiCategories() { return new int[]{1,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return null; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY}; }
    @Override public boolean hasStacksizeLimiters() { return !isBlacklist(); }
}
