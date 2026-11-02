package thaumcraft.port.golem.seal;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.research.ResearchStorage;

/** Provider catalogue and provisioning requests; displayed stacks are never real inventory. */
public final class LogisticsMenu extends AbstractContainerMenu {
    public record Entry(ItemStack stack,int count) { }
    private final Player owner;
    public final BlockPos target;
    public final Direction side;
    public final List<Entry> page=new ArrayList<>();
    private final List<Entry> catalogue=new ArrayList<>();
    public int start,end,revision;
    public String search="";
    private int ticks;
    private CompoundTag previous;
    public LogisticsMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){this(id,inventory,buffer.readBoolean()?buffer.readBlockPos():null,Direction.from3DDataValue(buffer.readByte()));}
    public LogisticsMenu(int id,Inventory inventory,BlockPos target){this(id,inventory,target,target==null?null:Direction.UP);}
    public LogisticsMenu(int id,Inventory inventory,BlockPos target,Direction side){super(GolemContent.LOGISTICS_MENU.get(),id);owner=inventory.player;this.target=target==null?null:target.immutable();this.side=side;if(!owner.level().isClientSide){refresh();previous=null;}}
    @Override public boolean stillValid(Player player){
        return player==owner&&player.isAlive()&&(target==null||player.distanceToSqr(target.getCenter())<=64)
                &&(player.level().isClientSide||ResearchStorage.get(player.getServer()).player(player.getUUID()).isResearchKnown("GOLEMLOGISTICS"));
    }
    public void refresh(){
        if(!(owner instanceof ServerPlayer player))return;var next=new ArrayList<Entry>();
        for(var seal:SealHandler.getSealsInRange(player.level(),player.blockPosition(),32)){
            if(!(seal.getSeal() instanceof SealProvide provider)||!seal.getOwner().equals(player.getUUID().toString()))continue;
            var handler=SealInventory.handler(player.level(),seal.getSealPos().pos,seal.getSealPos().face);if(handler==null)continue;
            for(int i=0;i<handler.getSlots();i++){
                var stack=handler.getStackInSlot(i);if(stack.isEmpty()||!provider.matchesFilters(stack)||!stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))continue;
                int existing=-1;for(int j=0;j<next.size();j++)if(ItemStack.isSameItemSameComponents(stack,next.get(j).stack())){existing=j;break;}
                if(existing<0)next.add(new Entry(stack.copyWithCount(1),stack.getCount()));
                else {var old=next.get(existing);next.set(existing,new Entry(old.stack(),(int)Math.min(Integer.MAX_VALUE,(long)old.count()+stack.getCount())));}
            }
        }
        next.sort(Comparator.comparing((Entry e)->e.stack().getHoverName().getString()).thenComparing(e->e.stack().getItem().toString()).thenComparingInt(e->e.stack().getComponents().hashCode()));
        catalogue.clear();catalogue.addAll(next);end=Math.max(0,(catalogue.size()+8)/9-9);start=Math.clamp(start,0,end);page.clear();page.addAll(catalogue.subList(Math.min(catalogue.size(),start*9),Math.min(catalogue.size(),start*9+81)));sync();
    }
    public void action(ServerPlayer player,SealPayloads.LogisticsAction action){
        if(!stillValid(player))return;
        if(action.slot()<0){search=action.search();start=Math.max(0,action.row());refresh();return;}
        if(action.revision()!=revision||action.slot()>=page.size()||action.count()<1){sync();return;}
        var selected=page.get(action.slot());int amount=Math.min(action.count(),selected.count());
        // Each original request is one stack; bound a single packet's allocation without granting items.
        if((long)amount>4096L*selected.stack().getMaxStackSize())return;
        if(target!=null&&(side==null||!player.level().hasChunkAt(target)||!player.level().mayInteract(player,target)||SealInventory.handler(player.level(),target,side)==null))return;
        int ui=0;while(amount>0){int take=Math.min(amount,selected.stack().getMaxStackSize());var stack=selected.stack().copyWithCount(take);if(target==null)GolemHelper.requestProvisioning(player.level(),player,stack,ui++);else GolemHelper.requestProvisioning(player.level(),target,side,stack,ui++);amount-=take;}
    }
    @Override public void broadcastChanges(){super.broadcastChanges();if(owner instanceof ServerPlayer&&(++ticks%20==0||previous==null))refresh();}
    private void sync(){if(!(owner instanceof ServerPlayer player))return;var tag=new CompoundTag();tag.putInt("start",start);tag.putInt("end",end);tag.putString("search",search);var list=new ListTag();for(var entry:page){var item=new CompoundTag();item.put("item",entry.stack().save(owner.registryAccess()));item.putInt("amount",entry.count());list.add(item);}tag.put("items",list);if(!tag.equals(previous)){previous=tag.copy();tag.putInt("revision",++revision);SealPayloads.menu(player,containerId,tag);}}
    public void receive(CompoundTag tag){if(!owner.level().isClientSide)return;revision=tag.getInt("revision");start=Math.max(0,tag.getInt("start"));end=Math.max(0,tag.getInt("end"));search=tag.getString("search");page.clear();for(var value:tag.getList("items",Tag.TAG_COMPOUND)){if(page.size()>=81)break;var e=(CompoundTag)value;var stack=ItemStack.parseOptional(owner.registryAccess(),e.getCompound("item"));if(!stack.isEmpty())page.add(new Entry(stack,Math.max(0,e.getInt("amount"))));}}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
