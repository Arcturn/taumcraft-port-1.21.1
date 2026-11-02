package thaumcraft.port.golem.seal;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.GolemContent;

/** Original categories and ghost counts, with server-authoritative permissions and custody. */
public final class SealMenu extends AbstractContainerMenu {
    public final SealEntity seal;
    public final int[] categories;
    public int category;
    public final int ghosts;
    private final Player owner;
    private CompoundTag previous;
    public SealMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){this(id,inventory,decode(buffer));}
    private static SealEntity decode(RegistryFriendlyByteBuf buffer){var seal=new SealEntity();var tag=buffer.readNbt();if(tag==null)throw new IllegalArgumentException("Missing seal");seal.readNBT(tag,buffer.registryAccess());return seal;}
    public SealMenu(int id,Inventory inventory,SealEntity seal){
        super(GolemContent.SEAL_MENU.get(),id);this.seal=seal;owner=inventory.player;categories=seal.getSeal() instanceof ISealGui gui?gui.getGuiCategories():new int[]{0,4};category=categories.length==0?0:categories[0];
        ghosts=seal.getSeal() instanceof ISealConfigFilter filter?filter.getFilterSize():0;
        var fake=new SimpleContainer(ghosts){
            @Override public ItemStack getItem(int slot){return ((ISealConfigFilter)seal.getSeal()).getFilterSlot(slot);}
            @Override public void setItem(int slot,ItemStack stack){((ISealConfigFilter)seal.getSeal()).setFilterSlot(slot,stack);}
        };
        int sx=16+(ghosts-1)%3*12,sy=16+(ghosts-1)/3*12;
        for(int i=0;i<ghosts;i++)addSlot(new Slot(fake,i,88+i%3*24-sx+8,72+i/3*24-sy+8){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
            @Override public boolean isActive(){return category==1;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,150+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,208));
    }
    @Override public boolean stillValid(Player player){
        if(player!=owner||!player.isAlive()||player.distanceToSqr(seal.getSealPos().pos.getCenter())>64)return false;
        return player.level().isClientSide||SealHandler.getSealEntity(player.level(),seal.getSealPos())==seal&&SealHandler.canConfigure(player,seal)&&player.level().mayInteract(player,seal.getSealPos().pos);
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!stillValid(player))return false;boolean changed=true;
        if(button>=25&&button<=28&&!seal.getOwner().equals(player.getUUID().toString()))return false;
        if(button>=0&&button<categories.length){category=categories[button];sync();return true;}
        if(category==3&&seal.getSeal() instanceof ISealConfigToggles toggles&&button>=30&&button<30+toggles.getToggles().length)toggles.setToggle(button-30,true);
        else if(category==3&&seal.getSeal() instanceof ISealConfigToggles toggles&&button>=60&&button<60+toggles.getToggles().length)toggles.setToggle(button-60,false);
        else if(category==0&&(button==25||button==26))seal.setLocked(button==25);
        else if(button==27||button==28)seal.setRedstoneSensitive(button==27);
        else if(category==1&&seal.getSeal() instanceof ISealConfigFilter filter&&(button==20||button==21))filter.setBlacklist(button==20);
        else if(category==0&&(button==80||button==81))seal.setPriority((byte)(seal.getPriority()+(button==80?-1:1)));
        else if(category==0&&(button==82||button==83))seal.setColor((byte)(seal.getColor()+(button==82?-1:1)));
        else if(category==2&&seal.getSeal() instanceof ISealConfigArea&&button>=90&&button<=95){var area=seal.getArea();int delta=button%2==0?-1:1;seal.setArea(button<92?area.offset(0,delta,0):button<94?area.offset(delta,0,0):area.offset(0,0,delta));}
        else changed=false;
        if(changed){seal.syncToClient(player.level());sync();}return changed;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        if(slot>=0&&slot<ghosts){if(category!=1||(button!=0&&button!=1)||(type!=ClickType.PICKUP&&type!=ClickType.QUICK_MOVE))return;
            if(!player.level().isClientSide){editGhost((ISealConfigFilter)seal.getSeal(),slot,getCarried(),button,type==ClickType.QUICK_MOVE);seal.syncToClient(player.level());sync();}return;
        }
        super.clicked(slot,button,type,player);
    }
    public static void editGhost(ISealConfigFilter filter,int slot,ItemStack carried,int button,boolean shift){
        if(slot<0||slot>=filter.getFilterSize())return;var old=filter.getFilterSlot(slot);boolean counted=filter.hasStacksizeLimiters();int amount=filter.getFilterSlotSize(slot);
        if(button==1){
            if(!counted){filter.setFilterSlot(slot,ItemStack.EMPTY);filter.setFilterSlotSize(slot,0);return;}
            if(!old.isEmpty()&&(carried.isEmpty()||ItemStack.isSameItemSameComponents(carried,old))){long next=(long)amount-(carried.isEmpty()?(shift?10:1):carried.getCount());if(next<0){filter.setFilterSlot(slot,ItemStack.EMPTY);next=0;}filter.setFilterSlotSize(slot,(int)next);}return;
        }
        if(button!=0)return;
        if(carried.isEmpty()){if(counted&&!old.isEmpty())filter.setFilterSlotSize(slot,(int)Math.min(Integer.MAX_VALUE,(long)amount+(shift?10:1)));return;}
        if(!counted)filter.setFilterSlotSize(slot,0);
        else if(!old.isEmpty()&&ItemStack.isSameItemSameComponents(carried,old))filter.setFilterSlotSize(slot,(int)Math.min(Integer.MAX_VALUE,(long)amount+carried.getCount()));
        else filter.setFilterSlotSize(slot,0);
        filter.setFilterSlot(slot,carried);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<ghosts||index>=slots.size())return ItemStack.EMPTY;var slot=getSlot(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
        if(index<ghosts+27?!moveItemStackTo(stack,ghosts+27,slots.size(),false):!moveItemStackTo(stack,ghosts,ghosts+27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return before;
    }
    private void sync(){if(owner instanceof ServerPlayer player){var tag=seal.writeNBT(owner.registryAccess());tag.putInt("category",category);previous=tag.copy();SealPayloads.menu(player,containerId,tag);}}
    @Override public void broadcastChanges(){super.broadcastChanges();if(owner instanceof ServerPlayer){var tag=seal.writeNBT(owner.registryAccess());tag.putInt("category",category);if(!tag.equals(previous))sync();}}
    public void receive(CompoundTag tag){if(!owner.level().isClientSide)return;try{seal.readNBT(tag,owner.registryAccess());int c=tag.getInt("category");if(Arrays.stream(categories).anyMatch(i->i==c))category=c;}catch(IllegalArgumentException ignored){}}
}
