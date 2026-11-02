package thaumcraft.port.golem.task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.seal.SealHandler;
import thaumcraft.port.golem.seal.SealPos;

/** One task pool per server level; never shares numeric dimension/entity IDs with a client. */
public final class TaskHandler {
    private static final int TASK_LIMIT=10000;
    private static final Map<Level,ConcurrentHashMap<Integer,Task>> TASKS=Collections.synchronizedMap(new WeakHashMap<>());
    public static ConcurrentHashMap<Integer,Task> getTasks(Level level){
        if(level.isClientSide)throw new IllegalArgumentException("Golem tasks belong to the logical server");
        synchronized(TASKS){return TASKS.computeIfAbsent(level,ignored->new ConcurrentHashMap<>());}
    }
    public static void addTask(Level level,Task task){
        var tasks=getTasks(level);
        if(tasks.size()>=TASK_LIMIT){var oldest=tasks.values().stream().filter(t->!t.isReserved()).min(Comparator.comparingInt(Task::getId)).orElse(null);if(oldest==null)return;suspendAndRemove(level,tasks,oldest);}
        tasks.put(task.getId(),task);
    }
    public static Task getTask(Level level,int id){return getTasks(level).get(id);}
    public static ArrayList<Task> getBlockTasksSorted(Level level,UUID uuid,Entity golem){return sorted(level,uuid,golem,(byte)0);}
    public static ArrayList<Task> getEntityTasksSorted(Level level,UUID uuid,Entity golem){return sorted(level,uuid,golem,(byte)1);}
    private static ArrayList<Task> sorted(Level level,UUID uuid,Entity golem,byte type){
        var result=new ArrayList<Task>();
        for(var task:getTasks(level).values()){
            if(task.isReserved()||task.isSuspended()||task.isCompleted()||task.getType()!=type||uuid!=null&&task.getGolemUUID()!=null&&!uuid.equals(task.getGolemUUID()))continue;
            if(type==1&&(task.getEntity()==null||task.getEntity().isRemoved())){task.setSuspended(true);continue;}
            result.add(task);
        }
        result.sort(Comparator.<Task>comparingDouble(t->t.getPos().distToCenterSqr(golem.position())-t.getPriority()*256.0).thenComparingInt(Task::getId));
        return result;
    }
    public static void completeTask(Task task,IGolemAPI golem){
        if(task.isCompleted()||task.isSuspended())return;
        var seal=task.getSealPos()==null?null:SealHandler.getSealEntity(golem.getGolemWorld(),task.getSealPos());
        task.setCompletion(seal==null||seal.getSeal().onTaskCompletion(golem.getGolemWorld(),golem,task));
    }
    public static void clearSuspendedOrExpiredTasks(Level level){
        var tasks=getTasks(level);
        for(var task:tasks.values()){
            if(!task.isSuspended()&&task.getLifespan()>0){task.setLifespan((short)(task.getLifespan()-1));continue;}
            suspendAndRemove(level,tasks,task);
        }
    }
    private static void suspendAndRemove(Level level,Map<Integer,Task> tasks,Task task){
        var seal=task.getSealPos()==null?null:SealHandler.getSealEntity(level,task.getSealPos());
        if(seal!=null)seal.getSeal().onTaskSuspension(level,task);
        task.setSuspended(true);tasks.remove(task.getId());
    }
    public static void suspendTasks(Level level,SealPos seal){for(var task:getTasks(level).values())if(Objects.equals(task.getSealPos(),seal))task.setSuspended(true);}
    public static void clear(Level level){TASKS.remove(level);}
    public static void clear(){TASKS.clear();}
    private TaskHandler(){}
}
