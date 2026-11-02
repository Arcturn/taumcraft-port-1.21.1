package thaumcraft.port.golem.seal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealGuard implements ISeal, ISealGui, ISealConfigArea {
    private int delay = new Random().nextInt(22);
    protected ISealConfigToggles.SealToggle[] props = {
        new ISealConfigToggles.SealToggle(true,"pmob","golem.prop.mob"),
        new ISealConfigToggles.SealToggle(false,"panimal","golem.prop.animal"),
        new ISealConfigToggles.SealToggle(false,"pplayer","golem.prop.player")};
    @Override public String getKey() { return "thaumcraft:guard"; }
    protected boolean validTarget(Level level, LivingEntity target) {
        return target.isAlive() && (props[0].value && target instanceof Enemy
                || props[1].value && (target instanceof Animal || target instanceof WaterAnimal || target instanceof AbstractGolem || target instanceof AmbientCreature || target instanceof Enemy)
                || props[2].value && target instanceof Player && level.getServer() != null && level.getServer().isPvpAllowed());
    }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide || delay++ % 20 != 0) return;
        for (var target : level.getEntitiesOfClass(LivingEntity.class, GolemHelper.getBoundsForArea(seal))) {
            if (!validTarget(level,target)) continue;
            var task = new Task(seal.getSealPos(),target);
            task.setPriority(seal.getPriority()); task.setLifespan((short)10); TaskHandler.addTask(level,task);
        }
    }
    @Override public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        if (task.getEntity() instanceof LivingEntity target && validTarget(level,target)
                && golem.getGolemEntity() instanceof Mob mob && canGolemPerformTask(golem,task)) {
            mob.setTarget(target); golem.addRankXp(1);
        }
        task.setSuspended(true);
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) { task.setSuspended(true); return true; }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        return task.getEntity() instanceof LivingEntity target && target.isAlive()
                && target != golem.getGolemEntity() && !golem.getGolemEntity().isAlliedTo(target);
    }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_guard"); }
    @Override public int[] getGuiCategories() { return new int[]{2,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
}
