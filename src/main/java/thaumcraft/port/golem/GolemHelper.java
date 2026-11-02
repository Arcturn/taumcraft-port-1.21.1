package thaumcraft.port.golem;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.golem.task.*;

public final class GolemHelper {
    private static final Map<Level,ArrayList<ProvisionRequest>> REQUESTS=Collections.synchronizedMap(new WeakHashMap<>());
    public static ArrayList<ProvisionRequest> getProvisionRequests(Level level){
        if(level.isClientSide)throw new IllegalArgumentException("Provision requests belong to the logical server");
        synchronized(REQUESTS){return REQUESTS.computeIfAbsent(level,ignored->new ArrayList<>());}
    }
    public static ISealEntity getSealEntity(Level level,SealPos pos){return SealHandler.getSealEntity(level,pos);}
    public static void addGolemTask(Level level,Task task){TaskHandler.addTask(level,task);}
    public static void requestProvisioning(Level level,ISealEntity seal,ItemStack stack){addRequest(level,new ProvisionRequest(seal,stack));}
    public static void requestProvisioning(Level level,BlockPos pos,Direction side,ItemStack stack){requestProvisioning(level,pos,side,stack,0);}
    public static void requestProvisioning(Level level,BlockPos pos,Direction side,ItemStack stack,int ui){var request=new ProvisionRequest(pos,side,stack);request.setUI(ui);addRequest(level,request);}
    public static void requestProvisioning(Level level,Entity entity,ItemStack stack){requestProvisioning(level,entity,stack,0);}
    public static void requestProvisioning(Level level,Entity entity,ItemStack stack,int ui){var request=new ProvisionRequest(entity,stack);request.setUI(ui);addRequest(level,request);}
    private static void addRequest(Level level,ProvisionRequest request){
        if(request.getStack().isEmpty())return;
        var requests=getProvisionRequests(level);if(!requests.contains(request))requests.add(request);if(requests.size()>1000)requests.removeFirst();
    }
    public static void clearExpiredRequests(Level level){getProvisionRequests(level).removeIf(r->r.isInvalid()||r.getTimeout()<System.currentTimeMillis()||r.getEntity()!=null&&r.getEntity().isRemoved());}
    public static void clear(Level level){REQUESTS.remove(level);TaskHandler.clear(level);}
    public static void clear(){REQUESTS.clear();TaskHandler.clear();}
    public static BlockPos getPosInArea(ISealEntity seal,int count){
        var face=seal.getSealPos().face;var area=seal.getArea();
        int xx=1+(area.getX()-1)*(face.getStepX()==0?2:1),yy=1+(area.getY()-1)*(face.getStepY()==0?2:1),zz=1+(area.getZ()-1)*(face.getStepZ()==0?2:1);
        int qx=face.getStepX()!=0?face.getStepX():1,qy=face.getStepY()!=0?face.getStepY():1,qz=face.getStepZ()!=0?face.getStepZ():1;
        int y=qy*(count/zz/xx)%yy+face.getStepY(),x=qx*(count/zz)%xx+face.getStepX(),z=qz*count%zz+face.getStepZ();
        return seal.getSealPos().pos.offset(x-(face.getStepX()==0?xx/2:0),y-(face.getStepY()==0?yy/2:0),z-(face.getStepZ()==0?zz/2:0));
    }
    public static AABB getBoundsForArea(ISealEntity seal){
        var face=seal.getSealPos().face;var area=seal.getArea();
        return new AABB(seal.getSealPos().pos).move(face.getStepX(),face.getStepY(),face.getStepZ())
                .expandTowards((area.getX()-1)*face.getStepX(),(area.getY()-1)*face.getStepY(),(area.getZ()-1)*face.getStepZ())
                .inflate(face.getStepX()==0?area.getX()-1:0,face.getStepY()==0?area.getY()-1:0,face.getStepZ()==0?area.getZ()-1:0);
    }
    private GolemHelper(){}
}
