package thaumcraft.port.golem.seal;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealEmpty extends SealFiltered {
    private int delay = new Random().nextInt(30);
    private int filterInc;
    private final Map<Integer,ItemStack> cache = new HashMap<>();
    protected ISealConfigToggles.SealToggle[] props = {
        new ISealConfigToggles.SealToggle(true,"pmeta","golem.prop.meta"),
        new ISealConfigToggles.SealToggle(true,"pnbt","golem.prop.nbt"),
        new ISealConfigToggles.SealToggle(false,"pore","golem.prop.ore"),
        new ISealConfigToggles.SealToggle(false,"pmod","golem.prop.mod"),
        new ISealConfigToggles.SealToggle(false,"pcycle","golem.prop.cycle"),
        new ISealConfigToggles.SealToggle(false,"pleave","golem.prop.leave")};
    @Override public String getKey() { return "thaumcraft:empty"; }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide) return;
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level,id) == null);
        if (delay++ % 20 != 0) return;
        var stack = SealInventory.firstExtractable(getInv(filterInc), isBlacklist(),
                SealInventory.handler(level,seal.getSealPos().pos,seal.getSealPos().face), SealInventory.filters(props), props[5].value);
        if (stack.isEmpty()) return;
        var task = new Task(seal.getSealPos(),seal.getSealPos().pos); task.setPriority(seal.getPriority());task.setLifespan((short)5);
        cache.put(task.getId(),stack.copy());TaskHandler.addTask(level,task);
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        var stack = cache.getOrDefault(task.getId(),ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            var pos = task.getSealPos(); var inventory = SealInventory.handler(level,pos.pos,pos.face); var filter = SealInventory.filters(props);
            int amount = Math.min(stack.getCount(),golem.canCarryAmount(stack));
            if (props[5].value) amount = Math.min(amount,Math.max(0,SealInventory.count(inventory,stack,filter)-1));
            if (amount > 0) {
                var removed = SealInventory.extract(inventory,stack.copyWithCount(amount),filter,false);
                if (!removed.isEmpty()) {
                    var remainder = golem.holdItem(removed);
                    if (!remainder.isEmpty()) {
                        remainder = SealInventory.insert(inventory,remainder,false);
                        if (!remainder.isEmpty()) GolemInteractionHelper.returnStack(golem,remainder);
                    }
                    golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP,.125f,(level.random.nextFloat()-level.random.nextFloat())*1.4f+2);
                    golem.swingArm();
                }
            }
        }
        cache.remove(task.getId());filterInc++;task.setSuspended(true);return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        var stack = cache.getOrDefault(task.getId(),ItemStack.EMPTY);
        return !stack.isEmpty() && golem.canCarry(stack,true);
    }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return SealInventory.handler(level,pos,face) != null; }
    public NonNullList<ItemStack> getInv(int cycle) { return super.getInv(); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_empty"); }
    @Override public int[] getGuiCategories() { return new int[]{1,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return null; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY}; }
    @Override public void onTaskSuspension(Level level, Task task) { cache.remove(task.getId()); }
}
