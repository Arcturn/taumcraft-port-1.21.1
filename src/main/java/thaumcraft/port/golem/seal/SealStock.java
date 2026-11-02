package thaumcraft.port.golem.seal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.task.Task;

/** Requests only the missing, insertable part of each of its nine stock levels. */
public final class SealStock extends SealFiltered implements ISealConfigToggles {
    private int delay = new Random().nextInt(50);
    protected SealToggle[] props = {
        new SealToggle(true,"pmeta","golem.prop.meta"),new SealToggle(true,"pnbt","golem.prop.nbt"),
        new SealToggle(false,"pore","golem.prop.ore"),new SealToggle(false,"pmod","golem.prop.mod")};
    @Override public String getKey() { return "thaumcraft:stock"; }
    @Override public int getFilterSize() { return 9; }
    @Override public void tickSeal(Level level,ISealEntity seal) {
        if(level.isClientSide||delay++%20!=0)return;
        var pos=seal.getSealPos();var inventory=SealInventory.handler(level,pos.pos,pos.face);if(inventory==null)return;
        var filter=SealInventory.filters(props);
        for(int i=0;i<getFilterSize();i++) {
            var template=getFilterSlot(i);if(template.isEmpty())continue;
            int missing=getFilterSlotSize(i)-SealInventory.count(inventory,template,filter);if(missing<=0)continue;
            var request=template.copyWithCount(Math.min(template.getMaxStackSize(),missing));
            int room=SealInventory.availableRoom(inventory,request);
            if(room>0)GolemHelper.requestProvisioning(level,pos.pos,pos.face,request.copyWithCount(room));
        }
    }
    @Override public boolean onTaskCompletion(Level level,IGolemAPI golem,Task task){return true;}
    @Override public boolean canGolemPerformTask(IGolemAPI golem,Task task){return false;}
    @Override public boolean canPlaceAt(Level level,BlockPos pos,Direction face){return SealInventory.handler(level,pos,face)!=null;}
    @Override public ResourceLocation getSealIcon(){return ResourceLocation.parse("thaumcraft:items/seals/seal_stock");}
    @Override public int[] getGuiCategories(){return new int[]{1,3,0,4};}
    @Override public EnumGolemTrait[] getRequiredTags(){return null;}
    @Override public EnumGolemTrait[] getForbiddenTags(){return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};}
    @Override public boolean hasStacksizeLimiters(){return true;}
    @Override public boolean isBlacklist(){return false;}
    @Override public SealToggle[] getToggles(){return props;}
    @Override public void setToggle(int index,boolean value){if(index>=0&&index<props.length)props[index].setValue(value);}
}
