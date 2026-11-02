package thaumcraft.port.golem.seal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

public class SealButcher implements ISeal, ISealGui, ISealConfigArea {
    private int delay = new Random().nextInt(200);
    private boolean waiting;
    @Override public String getKey() { return "thaumcraft:butcher"; }
    public static boolean validAnimal(LivingEntity entity) {
        return entity.isAlive() && (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature)
                && !(entity instanceof Enemy) && !(entity instanceof AbstractGolem)
                && !(entity instanceof TamableAnimal tame && tame.isTame())
                && !(entity instanceof Animal animal && animal.isBaby());
    }
    @Override public void tickSeal(Level level, ISealEntity seal) {
        if (level.isClientSide || delay++ % 200 != 0 || waiting) return;
        var bounds = GolemHelper.getBoundsForArea(seal);
        var animals = level.getEntitiesOfClass(LivingEntity.class,bounds,SealButcher::validAnimal);
        for (var animal : animals) {
            // Class.isInstance mirrors getEntitiesWithinAABB(entity.getClass()) in BETA26.
            if (animals.stream().filter(animal.getClass()::isInstance).limit(3).count() <= 2) continue;
            var task = new Task(seal.getSealPos(),animal);
            task.setPriority(seal.getPriority()); task.setLifespan((short)10); TaskHandler.addTask(level,task);
            waiting = true; break;
        }
    }
    @Override public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        if (task.getEntity() instanceof LivingEntity target && validAnimal(target) && golem.getGolemEntity() instanceof Mob mob) {
            mob.setTarget(target); golem.addRankXp(1);
        }
        task.setSuspended(true); waiting=false;
    }
    @Override public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) { task.setSuspended(true); waiting=false; return true; }
    @Override public boolean canGolemPerformTask(IGolemAPI golem, Task task) { return task.getEntity() instanceof LivingEntity target && validAnimal(target); }
    @Override public void onTaskSuspension(Level level, Task task) { waiting=false; }
    @Override public void onRemoval(Level level, BlockPos pos, Direction face) { waiting=false; }
    @Override public boolean canPlaceAt(Level level, BlockPos pos, Direction face) { return !level.isEmptyBlock(pos); }
    @Override public ResourceLocation getSealIcon() { return ResourceLocation.parse("thaumcraft:items/seals/seal_butcher"); }
    @Override public int[] getGuiCategories() { return new int[]{2,0,4}; }
    @Override public EnumGolemTrait[] getRequiredTags() { return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER,EnumGolemTrait.SMART}; }
    @Override public EnumGolemTrait[] getForbiddenTags() { return null; }
}
