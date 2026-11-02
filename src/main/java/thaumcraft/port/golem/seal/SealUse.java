package thaumcraft.port.golem.seal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealUse extends SealFiltered implements ISealConfigToggles {
    private int delay=new Random().nextInt(49), watchedTask=Integer.MIN_VALUE;
    protected SealToggle[] props={
        new SealToggle(true,"pmeta","golem.prop.meta"),new SealToggle(true,"pnbt","golem.prop.nbt"),
        new SealToggle(false,"pore","golem.prop.ore"),new SealToggle(false,"pmod","golem.prop.mod"),
        new SealToggle(false,"pleft","golem.prop.left"),new SealToggle(false,"pempty","golem.prop.empty"),
        new SealToggle(false,"pemptyhand","golem.prop.emptyhand"),new SealToggle(false,"psneak","golem.prop.sneak"),
        new SealToggle(false,"ppro","golem.prop.provision.wl")};
    @Override public String getKey() { return "thaumcraft:use"; }
    @Override public void tickSeal(Level level,ISealEntity seal) {
        if (level.isClientSide || delay++%5!=0) return;
        var old=TaskHandler.getTask(level,watchedTask);
        if ((old==null || old.isSuspended() || old.isCompleted()) && props[5].value==level.isEmptyBlock(seal.getSealPos().pos)) {
            var task=new Task(seal.getSealPos(),seal.getSealPos().pos); task.setPriority(seal.getPriority());
            TaskHandler.addTask(level,task); watchedTask=task.getId();
        }
    }
    private ItemStack selected(IGolemAPI golem) {
        return SealInventory.firstMatch(getInv(),getSizes(),isBlacklist(),golem.getCarrying(),SealInventory.filters(props)).stack();
    }
    @Override public boolean onTaskCompletion(Level level,IGolemAPI golem,Task task) {
        if (level.hasChunkAt(task.getPos()) && props[5].value==level.isEmptyBlock(task.getPos())) {
            // Empty-hand use must not remove the first carried stack (the BETA26 code lost it).
            var stack=props[6].value ? ItemStack.EMPTY : selected(golem);
            if (props[6].value || !stack.isEmpty()) {
                var owned=stack.isEmpty() ? ItemStack.EMPTY : golem.dropItem(stack.copy());
                GolemInteractionHelper.golemClick(level,golem,task.getPos(),task.getSealPos().face,owned,props[7].value,!props[4].value);
            }
        }
        task.setSuspended(true); return true;
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem,Task task) {
        if (props[6].value) return true;
        boolean found=!selected(golem).isEmpty();
        if (!found && props[8].value && !isBlacklist() && !getFilterSlot(0).isEmpty()) {
            var seal=SealHandler.getSealEntity(golem.getGolemWorld(),task.getSealPos());
            if (seal!=null) GolemHelper.requestProvisioning(golem.getGolemWorld(),seal,getFilterSlot(0).copy());
        }
        return found;
    }
    @Override public boolean canPlaceAt(Level level,BlockPos pos,Direction face) { return true; }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_use"); }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{1,3,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.DEFT,EnumGolemTrait.SMART}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
}
