package thaumcraft.port.alchemy.devices;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.aspect.Aspect;

public final class PotionSprayerMenu extends AbstractContainerMenu {
    private final Container machine;
    public final ContainerData data;
    public PotionSprayerMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(1),new SimpleContainerData(PotionSprayerEntity.DATA_COUNT));}
    public PotionSprayerMenu(int id,Inventory inventory,Container machine,ContainerData data){
        super(AlchemyDevices.SPRAYER_MENU.get(),id);this.machine=machine;this.data=data;checkContainerSize(machine,1);checkContainerDataCount(data,PotionSprayerEntity.DATA_COUNT);
        addSlot(new Slot(machine,0,56,64){@Override public boolean mayPlace(ItemStack stack){return PotionSprayerEntity.validPotion(stack);}@Override public int getMaxStackSize(){return 1;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+y*9+x,16+x*18,151+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,16+x*18,209));addDataSlots(data);
    }
    public int charges(){return Math.clamp(data.get(0),0,8);}
    public int color(){return (data.get(1)&65535)|((data.get(2)&255)<<16);}
    public int required(Aspect aspect){return Math.max(0,data.get(3+aspect.ordinal()*2));}
    public int progress(Aspect aspect){return Math.clamp(data.get(4+aspect.ordinal()*2),0,required(aspect));}
    public List<Aspect> recipe(){return Arrays.stream(Aspect.values()).filter(a->required(a)>0).sorted(Comparator.comparing(Aspect::displayName)).toList();}
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        boolean moved=index==0?moveItemStackTo(stack,1,37,true):PotionSprayerEntity.validPotion(stack)&&moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
