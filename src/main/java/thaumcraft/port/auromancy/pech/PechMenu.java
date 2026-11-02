package thaumcraft.port.auromancy.pech;

import java.util.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.content.PortSounds;

/** Original one input/four outputs barter roll, paid once and executed only on the server. */
public final class PechMenu extends AbstractContainerMenu {
    private final PechEntity pech;
    private final Player player;
    private final SimpleContainer inventory=new SimpleContainer(5);
    public PechMenu(int id,Inventory inventory,int entityId){this(id,inventory,inventory.player.level().getEntity(entityId) instanceof PechEntity pech?pech:null);}
    public PechMenu(int id,Inventory playerInventory,PechEntity pech){
        super(PechContent.MENU.get(),id);this.pech=pech;player=playerInventory.player;
        if(pech!=null){pech.trading=true;pech.tradingPlayer=player.getUUID();}
        addSlot(new Slot(inventory,0,36,29));
        for(int row=0;row<2;row++)for(int column=0;column<2;column++)addSlot(new Slot(inventory,1+column+row*2,106+18*column,20+18*row){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(playerInventory,column+row*9+9,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(playerInventory,column,8+column*18,142));
    }
    public PechEntity pech(){return pech;}
    public SimpleContainer tradeInventory(){return inventory;}
    @Override public boolean stillValid(Player player){return pech!=null&&pech.isAlive()&&pech.isTamed()&&player.distanceToSqr(pech)<64&&Objects.equals(pech.tradingPlayer,player.getUUID());}
    @Override public boolean clickMenuButton(Player player,int button){if(button!=0||player.level().isClientSide||!stillValid(player))return false;generateContents();broadcastChanges();return true;}
    public void generateContents(){
        if(player.level().isClientSide||pech==null||!stillValid(player)||inventory.getItem(0).isEmpty()||!pech.isValued(inventory.getItem(0)))return;
        for(int slot=1;slot<5;slot++)if(!inventory.getItem(slot).isEmpty())return;
        int value=pech.getValue(inventory.getItem(0));var random=player.level().random;
        if(random.nextInt(100)<=value/2){pech.setTamed(false);pech.playSound(PortSounds.get("pech_trade"),.4f,1);}
        if(random.nextInt(5)==0)value+=random.nextInt(3);else if(random.nextBoolean())value-=random.nextInt(3);
        var pool=PechTrades.entries(player.level(),pech.getPechType());
        while(value>0){
            int amount=Math.min(5,Math.max((value+1)/2,random.nextInt(value)+1));value-=amount;
            var packSlots=new ArrayList<Integer>();for(int i=0;i<pech.loot.size();i++)if(!pech.loot.get(i).isEmpty())packSlots.add(i);
            if(amount==1&&random.nextBoolean()&&!packSlots.isEmpty()){
                int slot=packSlots.get(random.nextInt(packSlots.size()));addStack(pech.loot.get(slot).copyWithCount(1));pech.loot.get(slot).shrink(1);continue;
            }
            if(amount>=4&&random.nextBoolean())continue;
            PechTrades.Entry reward;do{reward=pool.get(random.nextInt(pool.size()));}while(reward.tier()!=amount);
            var stack=reward.stack().copy();if(!stack.isEmpty())stack.onCraftedBy(player.level(),player,0);addStack(stack);
        }
        inventory.removeItem(0,1);inventory.setChanged();
    }
    private void addStack(ItemStack stack){
        // Retain the source's slot walk and strict '< max' merge check.
        for(int slot=1;slot<5;slot++){
            var current=inventory.getItem(slot);if(current.isEmpty()){inventory.setItem(slot,stack);break;}
            if(ItemStack.isSameItem(current,stack)&&current.getCount()+stack.getCount()<current.getMaxStackSize())current.grow(stack.getCount());
        }
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var current=slot.getItem();var copy=current.copy();
        if(index<5?!moveItemStackTo(current,5,41,true):!moveItemStackTo(current,0,1,true))return ItemStack.EMPTY;
        if(current.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(current.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,current);return copy;
    }
    @Override public void removed(Player player){
        super.removed(player);if(pech!=null&&Objects.equals(pech.tradingPlayer,player.getUUID())){pech.trading=false;pech.tradingPlayer=null;}
        if(!player.level().isClientSide)for(int slot=0;slot<5;slot++){var stack=inventory.removeItemNoUpdate(slot);if(!stack.isEmpty()){var entity=player.drop(stack,false);if(entity!=null)entity.getPersistentData().putBoolean("PechDrop",true);}}
    }
}
