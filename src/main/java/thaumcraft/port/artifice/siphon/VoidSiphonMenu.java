package thaumcraft.port.artifice.siphon;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class VoidSiphonMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public VoidSiphonMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(1),new SimpleContainerData(2));}
    public VoidSiphonMenu(int id,Inventory inventory,VoidSiphonEntity siphon){this(id,inventory,siphon,siphon.data);}
    private VoidSiphonMenu(int id,Inventory inventory,Container machine,ContainerData data){
        super(VoidSiphonContent.MENU.get(),id);this.machine=machine;this.data=data;
        addSlot(new Slot(machine,0,80,32){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+y*9+x,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));addDataSlots(data);
    }
    public int progress(){return (data.get(0)&65535)|((data.get(1)&32767)<<16);}
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        // Even an existing output stack must not accept seeds from the player's inventory.
        if(index!=0)return ItemStack.EMPTY;var slot=slots.getFirst();if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
