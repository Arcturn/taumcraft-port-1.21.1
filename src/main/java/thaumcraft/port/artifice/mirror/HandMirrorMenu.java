package thaumcraft.port.artifice.mirror;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Vanilla container transactions retain custody until the destination accepts a copied stack. */
public final class HandMirrorMenu extends AbstractContainerMenu {
    public final SimpleContainer input=new SimpleContainer(1);
    public final InteractionHand hand;
    public final int selected;
    private final Player owner;
    private final ItemStack mirror;
    private boolean processing;
    public HandMirrorMenu(int id,Inventory inventory,InteractionHand hand){
        super(MirrorContent.MENU.get(),id);this.owner=inventory.player;this.hand=hand;selected=inventory.selected;mirror=owner.getItemInHand(hand);
        addSlot(new Slot(input,0,80,24){@Override public boolean mayPlace(ItemStack stack){return !(stack.getItem() instanceof HandMirrorItem);}});
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(playerSlot(inventory,9+row*9+column,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(playerSlot(inventory,column,8+column*18,142));
    }
    private Slot playerSlot(Inventory inventory,int index,int x,int y){return new Slot(inventory,index,x,y){
        @Override public boolean mayPickup(Player player){return !(getItem().getItem() instanceof HandMirrorItem);}
        @Override public boolean mayPlace(ItemStack stack){return !(getItem().getItem() instanceof HandMirrorItem);}
    };}
    /** Slot snapshots replace client stacks; only the server can enforce reference custody. */
    public static boolean matchesHeldMirror(ItemStack expected,ItemStack current,boolean client){return current.getItem() instanceof HandMirrorItem&&MirrorLink.get(current)!=null&&(client?ItemStack.isSameItemSameComponents(expected,current):current==expected);}
    @Override public boolean stillValid(Player player){return player==owner&&player.isAlive()&&matchesHeldMirror(mirror,player.getItemInHand(hand),player.level().isClientSide);}
    private void transfer(){
        if(processing||!(owner instanceof ServerPlayer player)||!stillValid(player)||input.isEmpty())return;
        processing=true;
        try{if(HandMirrorItem.transport(mirror,input.getItem(0),player))input.removeItemNoUpdate(0);}finally{processing=false;}
    }
    @Override public void broadcastChanges(){transfer();super.broadcastChanges();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        if(type==ClickType.SWAP&&(button==40&&hand==InteractionHand.OFF_HAND||button==selected&&hand==InteractionHand.MAIN_HAND))return;
        if(slot>=0&&slot<slots.size()&&getSlot(slot).getItem().getItem() instanceof HandMirrorItem)return;
        super.clicked(slot,button,type,player);transfer();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=getSlot(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(index==0?!moveItemStackTo(stack,1,slots.size(),true):!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);transfer();return before;
    }
    @Override public void removed(Player player){super.removed(player);if(!player.level().isClientSide)clearContainer(player,input);}
}
