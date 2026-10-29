package thaumcraft.port.essentia;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SmelterMenu extends AbstractContainerMenu {
    private final Container machine;
    public final ContainerData data;
    public SmelterMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(2),new SimpleContainerData(5));}
    public SmelterMenu(int id,Inventory inv,Container machine,ContainerData data){
        super(EssentiaContent.SMELTER_MENU.get(),id);this.machine=machine;this.data=data;checkContainerSize(machine,2);checkContainerDataCount(data,5);
        addSlot(new Slot(machine,0,80,8){public boolean mayPlace(ItemStack stack){return machine instanceof SimpleContainer||machine.canPlaceItem(0,stack);}});
        addSlot(new Slot(machine,1,80,48){public boolean mayPlace(ItemStack stack){return SmelterEntity.fuel(stack)>0;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));addDataSlots(data);
    }
    @Override public boolean stillValid(Player p){return machine.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();boolean moved;
        if(i<2)moved=moveItemStackTo(stack,2,38,true);
        else if(SmelterEntity.fuel(stack)>0)moved=moveItemStackTo(stack,1,2,false);
        else moved=moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
