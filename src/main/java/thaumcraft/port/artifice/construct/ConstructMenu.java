package thaumcraft.port.artifice.construct;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ConstructMenu extends AbstractContainerMenu {
    private final OwnedConstruct construct;
    public ConstructMenu(int id,Inventory inventory,int entityId){this(id,inventory,inventory.player.level().getEntity(entityId) instanceof OwnedConstruct entity?entity:null);}
    public ConstructMenu(int id,Inventory inventory,OwnedConstruct construct){
        super(ConstructContent.MENU.get(),id);this.construct=construct;
        addSlot(new Slot(construct==null?new SimpleContainer(1):construct.inventory,0,construct instanceof TurretEntity turret&&turret.isAdvanced()?42:80,29){@Override public boolean mayPlace(ItemStack stack){return construct!=null&&construct.accepts(stack);}@Override public int getMaxStackSize(){return construct instanceof ArcaneBoreEntity?1:64;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+18*col,84+18*row));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+18*col,142));
    }
    public OwnedConstruct construct(){return construct;}
    @Override public boolean stillValid(Player player){return construct!=null&&construct.inventory.stillValid(player);}
    @Override public boolean clickMenuButton(Player player,int button){if(!stillValid(player)||player.level().isClientSide||!(construct instanceof TurretEntity turret)||!turret.isAdvanced()||button<1||button>4)return false;turret.setTargetFlag(button-1,!turret.targetFlag(button-1));return true;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index==0?!moveItemStackTo(stack,1,slots.size(),true):!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
