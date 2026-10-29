package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ThaumatoriumMenu extends AbstractContainerMenu {
    private final Container machine;
    private final SimpleContainer previews=new SimpleContainer(9);
    public final ContainerData data=new SimpleContainerData(15);
    private final Player viewer;
    private final List<ResourceLocation> visible=new ArrayList<>();
    private int page;
    public ThaumatoriumMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(1));}
    public ThaumatoriumMenu(int id,Inventory inv,Container machine){
        super(EssentiaContent.THAUMATORIUM_MENU.get(),id);this.machine=machine;viewer=inv.player;
        addSlot(new Slot(machine,0,26,34));
        for(int i=0;i<9;i++)addSlot(new Slot(previews,i,84+i%3*18,16+i/3*18){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,104+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,162));addDataSlots(data);refresh();
    }
    private void refresh(){
        if(!(machine instanceof ThaumatoriumEntity be)||!(viewer instanceof ServerPlayer player))return;
        var choices=be.choices(player);int pages=Math.max(1,(choices.size()+8)/9);page=Math.clamp(page,0,pages-1);visible.clear();
        for(int i=0;i<9;i++){int index=page*9+i;if(index<choices.size()){var h=choices.get(index);visible.add(h.id());previews.setItem(i,h.value().result().copy());data.set(i,be.selected.containsKey(h.id())?1:0);}else{previews.setItem(i,ItemStack.EMPTY);data.set(i,0);}}
        data.set(9,page+1);data.set(10,pages);data.set(11,be.capacity);data.set(12,be.heated?1:0);data.set(13,be.contents.total());data.set(14,be.selected.size());
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(slot>=1&&slot<=9){if(type==ClickType.PICKUP&&button==0&&stillValid(player)&&player instanceof ServerPlayer sp&&machine instanceof ThaumatoriumEntity be&&slot-1<visible.size()){be.choose(sp,visible.get(slot-1));broadcastChanges();}return;}
        super.clicked(slot,button,type,player);
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!stillValid(player)||!(machine instanceof ThaumatoriumEntity be)||button<0||button>2)return false;
        if(button==0)page=Math.max(0,page-1);else if(button==1)page=Math.min(data.get(10)-1,page+1);else{be.selected.clear();be.demand=null;be.sync();}
        broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size()||(i>=1&&i<=9))return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(!(i==0?moveItemStackTo(stack,10,46,true):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
