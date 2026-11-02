package thaumcraft.port.golem.seal;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.ProvisionRequest;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.golem.task.TaskHandler;

/** Provider pickup and delivery are separate tickets, traversed by a real carrying golem. */
public final class SealProvide extends SealFiltered implements ISealConfigToggles {
    private int delay=new Random().nextInt(88);
    protected SealToggle[] props={new SealToggle(true,"pmeta","golem.prop.meta"),new SealToggle(true,"pnbt","golem.prop.nbt"),
        new SealToggle(false,"pore","golem.prop.ore"),new SealToggle(false,"pmod","golem.prop.mod"),
        new SealToggle(false,"psing","golem.prop.single"),new SealToggle(false,"pleave","golem.prop.leave")};
    @Override public String getKey(){return "thaumcraft:provider";}
    @Override public int getFilterSize(){return 9;}
    @Override public void tickSeal(Level level,ISealEntity seal){
        if(level.isClientSide)return;
        var requests=GolemHelper.getProvisionRequests(level);
        if(delay%100==0)requests.removeIf(pr->pr.isInvalid()||pr.getLinkedTask()==null||pr.getLinkedTask().isSuspended()
                ||pr.getLinkedTask().isCompleted()||pr.getTimeout()<System.currentTimeMillis());
        if(delay++%20!=0)return;
        var pos=seal.getSealPos();var inventory=SealInventory.handler(level,pos.pos,pos.face);if(inventory==null)return;
        for(var iterator=requests.iterator();iterator.hasNext();){
            var request=iterator.next();if(request.isInvalid()){iterator.remove();continue;}
            if(request.getLinkedTask()!=null||!inRange(pos.pos,request)||!matchesFilters(request.getStack())
                    ||SealInventory.count(inventory,request.getStack(),SealInventory.STRICT)<=(props[5].value?1:0))continue;
            var task=new Task(pos,pos.pos);task.setPriority(request.getSeal()!=null?request.getSeal().getPriority():(byte)5);
            task.setLifespan(request.getSeal()!=null?(short)10:(short)31000);
            TaskHandler.addTask(level,task);request.setLinkedTask(task);task.setLinkedProvision(request);break;
        }
    }
    private static boolean inRange(BlockPos origin,ProvisionRequest request){
        return request.getSeal()!=null&&origin.distSqr(request.getSeal().getSealPos().pos)<4096
            ||request.getEntity()!=null&&request.getEntity().isAlive()&&Vec3.atCenterOf(origin).distanceToSqr(request.getEntity().position())<4096
            ||request.getPos()!=null&&origin.distSqr(request.getPos())<4096;
    }
    public boolean matchesFilters(ItemStack stack){return SealInventory.matchesFilters(getInv(),isBlacklist(),stack,SealInventory.filters(props));}
    @Override public boolean onTaskCompletion(Level level,IGolemAPI golem,Task task){
        var request=task.getLinkedProvision();
        if(request==null){task.setSuspended(true);return true;}
        if(task.getData()==0)collect(level,golem,task,request);
        else if(!deliver(level,golem,task,request))return false;
        task.setSuspended(true);return true;
    }
    private void collect(Level level,IGolemAPI golem,Task task,ProvisionRequest request){
        var pos=task.getSealPos();var inventory=SealInventory.handler(level,pos.pos,pos.face);if(inventory==null)return;
        var stack=request.getStack().copy();if(props[4].value&&!stack.isEmpty())stack.setCount(1);
        if(props[5].value&&!stack.isEmpty())stack.setCount(Math.min(stack.getCount(),Math.max(0,SealInventory.count(inventory,stack,SealInventory.STRICT)-1)));
        int amount=Math.min(stack.getCount(),golem.canCarryAmount(stack));if(amount<=0)return;
        var removed=SealInventory.extract(inventory,stack.copyWithCount(amount),SealInventory.STRICT,false);if(removed.isEmpty())return;
        var remainder=golem.holdItem(removed);
        if(!remainder.isEmpty()){
            remainder=SealInventory.insert(inventory,remainder,false);
            if(!remainder.isEmpty())GolemInteractionHelper.returnStack(golem,remainder);
        }
        if(removed.getCount()<=remainder.getCount())return;
        sound(level,golem,true);golem.addRankXp(1);golem.swingArm();
        if(request.getEntity()!=null||request.getPos()!=null){
            var delivery=request.getEntity()!=null?new Task(pos,request.getEntity()):new Task(pos,request.getPos());
            delivery.setPriority(task.getPriority());delivery.setData(request.getEntity()!=null?1:2);delivery.setLifespan((short)31000);
            TaskHandler.addTask(level,delivery);request.setLinkedTask(delivery);delivery.setLinkedProvision(request);
        }
    }
    private boolean deliver(Level level,IGolemAPI golem,Task task,ProvisionRequest request){
        var expected=request.getStack();var removed=golem.dropItem(expected);int delivered=0;
        if(task.getData()==1){
            if(request.getEntity()==null||!request.getEntity().isAlive()){golem.holdItem(removed);request.setInvalid(true);return true;}
            var target=request.getEntity();var at=target.position().add(0,target.getEyeHeight()/2,0);
            if(!SealInventory.eject(level,at,removed)){golem.holdItem(removed);return false;}
            delivered=removed.getCount();
        }else{
            if(request.getPos()==null||request.getSide()==null||!level.hasChunkAt(request.getPos())){golem.holdItem(removed);return false;}
            var inventory=SealInventory.handler(level,request.getPos(),request.getSide());
            ItemStack remainder;
            if(inventory==null){
                var output=Vec3.atCenterOf(request.getPos().relative(request.getSide()));
                if(!SealInventory.eject(level,output,removed)){golem.holdItem(removed);return false;}
                remainder=ItemStack.EMPTY;
            }else remainder=SealInventory.insert(inventory,removed,false);
            delivered=removed.getCount()-remainder.getCount();
            if(!remainder.isEmpty())golem.holdItem(remainder);
        }
        request.setInvalid(true);
        // The original creates another request only when the carrier brought fewer items.
        // Remove the completed request first so an equal full-size retry can be registered.
        if(removed.getCount()<expected.getCount()){
            GolemHelper.getProvisionRequests(level).remove(request);var missing=expected.copyWithCount(expected.getCount()-removed.getCount());
            if(task.getData()==1)GolemHelper.requestProvisioning(level,request.getEntity(),missing);
            else GolemHelper.requestProvisioning(level,request.getPos(),request.getSide(),missing);
        }
        if(delivered>0){sound(level,golem,false);golem.swingArm();}
        return true;
    }
    private static void sound(Level level,IGolemAPI golem,boolean pickup){
        golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP,.125f,((level.random.nextFloat()-level.random.nextFloat())*.7f+1)*(pickup?2:1));
    }
    @Override public boolean canGolemPerformTask(IGolemAPI golem,Task task){
        var request=task.getLinkedProvision();if(request==null||request.isInvalid()||!(golem instanceof GolemEntity entity))return false;
        boolean home=request.getSeal()!=null&&entity.isWithinHome(request.getSeal().getSealPos().pos)
            ||request.getEntity()!=null&&request.getEntity().isAlive()&&entity.isWithinHome(request.getEntity().blockPosition())
            ||request.getPos()!=null&&entity.isWithinHome(request.getPos());
        if(!home||!validTargetTraits(request.getSeal(),entity))return false;
        var stack=request.getStack();return !stack.isEmpty()&&(task.getData()==0?!golem.isCarrying(stack)&&golem.canCarry(stack,true):golem.isCarrying(stack));
    }
    private static boolean validTargetTraits(ISealEntity seal,GolemEntity golem){
        if(seal==null)return true;
        if(seal.isLocked()&&(golem.getOwnerUUID()==null||!golem.getOwnerUUID().toString().equals(seal.getOwner())))return false;
        var required=seal.getSeal().getRequiredTags();var forbidden=seal.getSeal().getForbiddenTags();
        if(required!=null&&!golem.getProperties().getTraits().containsAll(Arrays.asList(required)))return false;
        return forbidden==null||Arrays.stream(forbidden).noneMatch(golem.getProperties()::hasTrait);
    }
    @Override public void onTaskSuspension(Level level,Task task){
        var request=task.getLinkedProvision();if(request!=null&&request.getLinkedTask()==task)request.setLinkedTask(null);task.setLinkedProvision(null);
    }
    @Override public boolean canPlaceAt(Level level,BlockPos pos,Direction face){return SealInventory.handler(level,pos,face)!=null;}
    @Override public ResourceLocation getSealIcon(){return ResourceLocation.parse("thaumcraft:items/seals/seal_provider");}
    @Override public int[] getGuiCategories(){return new int[]{1,3,0,4};}
    @Override public EnumGolemTrait[] getRequiredTags(){return null;}
    @Override public EnumGolemTrait[] getForbiddenTags(){return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};}
    @Override public SealToggle[] getToggles(){return props;}
    @Override public void setToggle(int index,boolean value){if(index>=0&&index<props.length)props[index].setValue(value);}
}
