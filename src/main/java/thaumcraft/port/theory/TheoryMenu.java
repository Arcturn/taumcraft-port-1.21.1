package thaumcraft.port.theory;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.ResearchStorage;

public final class TheoryMenu extends AbstractContainerMenu {
    public final Container table;public final BlockPos pos;
    private final Player player;
    public String token=UUID.randomUUID().toString();public long revision=-1;
    public TheorySession view;public List<String> aids=List.of();public int inspiration=5;public boolean owned=true;
    private CompoundTag lastSent;private long aidCheck=-100;
    public TheoryMenu(int id,Inventory inv,Container table,BlockPos pos){
        super(TheoryContent.MENU.get(),id);this.table=table;this.pos=pos;this.player=inv.player;
        addSlot(new Slot(table,0,16,15){public boolean mayPlace(ItemStack s){return s.is(PortContent.SCRIBING_TOOLS.get());}public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(table,1,224,16){public boolean mayPlace(ItemStack s){return s.is(Items.PAPER);}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+x+y*9,77+x*18,190+y*18));
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)addSlot(new Slot(inv,x+y*3,20+x*18,190+y*18));
    }
    @Override public boolean stillValid(Player p){return table instanceof ResearchTableEntity?table.stillValid(p):p.level().getBlockState(pos).is(TheoryContent.TABLE.get())&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(stack.is(PortContent.SCRIBING_TOOLS.get())){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(stack.is(Items.PAPER)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<29){if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
    public void action(ServerPlayer p,TheoryActionPayload request){
        if(p!=player||p.containerMenu!=this||request.container()!=containerId||!token.equals(request.token())||!(table instanceof ResearchTableEntity entity)||entity.revision!=request.revision()||!stillValid(p))return;
        var selected=new LinkedHashSet<String>();if(!request.aids().isEmpty())selected.addAll(Arrays.asList(request.aids().split(",")));
        if(selected.size()>14||!TheoryAids.DEFINITIONS.keySet().containsAll(selected))return;
        if(!entity.action(p,request.action(),selected))p.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumcraft.theory.unavailable"),true);
        broadcastChanges();
    }
    @Override public void broadcastChanges(){
        super.broadcastChanges();if(!(player instanceof ServerPlayer sp)||!(table instanceof ResearchTableEntity entity))return;
        if(entity.session!=null&&entity.session.owner.equals(player.getUUID())){
            var context=TheoryItems.context(sp);boolean changed=false;
            for(int i=0;i<entity.session.choices.size();i++){var old=entity.session.choices.get(i);var fresh=TheoryCards.refreshValue(old,context);if(!old.equals(fresh)){entity.session.choices.set(i,fresh);changed=true;}}
            var last=TheoryCards.refreshValue(entity.session.lastDraw,context);if(!Objects.equals(last,entity.session.lastDraw)){entity.session.lastDraw=last;changed=true;}
            if(changed){entity.revision++;entity.setChanged();}
        }
        if(player.level().getGameTime()-aidCheck>=100){aids=List.copyOf(entity.nearbyAids());aidCheck=player.level().getGameTime();}
        var tag=new CompoundTag();if(entity.session!=null)tag.putString("session",entity.session.encode());
        tag.putString("aids",String.join(",",aids));tag.putInt("inspiration",TheorySession.availableInspiration(ResearchStorage.get(sp.getServer()).player(sp.getUUID())));tag.putLong("revision",entity.revision);tag.putBoolean("owned",entity.session==null||entity.session.owner.equals(player.getUUID()));
        if(!tag.equals(lastSent)){PacketDistributor.sendToPlayer(sp,new TheorySyncPayload(containerId,token,tag));lastSent=tag.copy();}
    }
    public void receive(String nonce,CompoundTag tag){token=nonce;revision=tag.getLong("revision");view=tag.contains("session")?TheorySession.decode(tag.getString("session")):null;aids=tag.getString("aids").isEmpty()?List.of():List.of(tag.getString("aids").split(","));inspiration=tag.getInt("inspiration");owned=tag.getBoolean("owned");}
}
