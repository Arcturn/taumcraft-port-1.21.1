package thaumcraft.port.auromancy.focal;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.auromancy.AuromancyContent;
import thaumcraft.port.auromancy.item.FocusItem;
import thaumcraft.port.content.PortSounds;

/** A per-open nonce and serialized editing queue prevent stale/reordered editor writes. */
public final class FocalManipulatorMenu extends AbstractContainerMenu {
    public final FocalManipulatorEntity entity;
    private final Player player;
    private String token=UUID.randomUUID().toString();
    private long knownRevision=-1,nextSequence,lastSequence,inFlight;
    private CompoundTag queued,lastSent;
    private boolean startQueued;
    public FocalManipulatorMenu(int id,Inventory inventory,FocalManipulatorEntity entity){
        super(AuromancyContent.FOCAL_MENU.get(),id);this.entity=entity;player=inventory.player;
        addSlot(new Slot(entity,0,31,191){
            @Override public boolean mayPlace(ItemStack stack){return entity.vis<=0&&stack.getItem() instanceof FocusItem;}
            @Override public boolean mayPickup(Player p){return entity.vis<=0;}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int i=0;i<3;i++)for(int j=0;j<9;j++)addSlot(new Slot(inventory,j+i*9+9,i*18-62,64+j*18));
        for(int i=0;i<3;i++)for(int j=0;j<3;j++)addSlot(new Slot(inventory,i+j*3,i*18-62,j*18+7));
    }
    @Override public boolean stillValid(Player p){return entity.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof FocusItem){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(index<28){if(!moveItemStackTo(stack,28,37,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,1,28,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;
    }
    @Override public boolean clickMenuButton(Player p,int button){
        if(button!=0||p!=player||!(p instanceof ServerPlayer server)||!stillValid(p)||!p.isAlive())return false;
        boolean accepted=entity.startCraft(server);
        if(!accepted)p.level().playSound(null,entity.getBlockPos(),PortSounds.get("craftfail"),net.minecraft.sounds.SoundSource.BLOCKS,.33f,1);
        sync(0,!accepted);return accepted;
    }
    public void edit(ServerPlayer p,FocalPayloads.Edit request){
        if(p!=player||p.containerMenu!=this||request.container()!=containerId||!token.equals(request.token())||!stillValid(p)||!p.isAlive())return;
        boolean accepted=false;
        if(request.sequence()>lastSequence&&request.sequence()>0&&request.revision()==entity.revision){
            lastSequence=request.sequence();var tag=request.data();String name=tag.getString("focusName");
            try{if(name.length()<=50)accepted=entity.edit(FocalManipulatorEntity.readGraph(tag.getList("nodes",Tag.TAG_COMPOUND)),name);}catch(IllegalArgumentException ignored){}
        }
        sync(request.sequence(),!accepted);
    }
    @Override public void broadcastChanges(){
        super.broadcastChanges();if(!(player instanceof ServerPlayer))return;
        var current=entity.editorTag();if(!current.equals(lastSent))sync(0,false);
    }
    private void sync(long acknowledged,boolean rejected){
        if(!(player instanceof ServerPlayer server))return;
        var data=entity.editorTag();lastSent=data.copy();data.putLong("ack",acknowledged);data.putBoolean("rejected",rejected);
        PacketDistributor.sendToPlayer(server,new FocalPayloads.Sync(containerId,token,data));
    }
    public void sendEdit(Map<Integer,FocusElementNode> graph,String name){
        if(!player.level().isClientSide||entity.vis>0)return;
        var next=new CompoundTag();next.put("nodes",FocalManipulatorEntity.writeGraph(graph));next.putString("focusName",name.length()>50?name.substring(0,50):name);queued=next;flush();
    }
    public void sendName(String name){sendEdit(entity.data,name);}
    public void requestStart(){if(!player.level().isClientSide)return;startQueued=true;flush();}
    private void flush(){
        if(knownRevision<0||inFlight!=0)return;
        if(queued!=null){var data=queued;queued=null;inFlight=++nextSequence;PacketDistributor.sendToServer(new FocalPayloads.Edit(containerId,token,knownRevision,inFlight,data));}
        else if(startQueued){startQueued=false;sendStart.run();}
    }
    /** Assigned by the client screen; keeps the dedicated server free of Minecraft client classes. */
    public Runnable sendStart=()->{};
    public void receive(String nonce,CompoundTag tag){
        if(!player.level().isClientSide)return;
        // A menu's nonce is immutable after the initial handshake. A delayed
        // snapshot from an earlier opening must not replace this editing session.
        if(!nonce.equals(token)&&knownRevision>=0)return;
        if(tag.getLong("revision")<knownRevision)return;
        token=nonce;knownRevision=tag.getLong("revision");entity.revision=knownRevision;entity.readVisual(tag);
        long ack=tag.getLong("ack");if(inFlight!=0&&ack==inFlight)inFlight=0;
        if(tag.getBoolean("rejected")){queued=null;startQueued=false;}
        if(queued==null&&inFlight==0){
            try{entity.data=FocalManipulatorEntity.readGraph(tag.getList("nodes",Tag.TAG_COMPOUND));}catch(IllegalArgumentException ex){entity.data.clear();}
            entity.focusName=tag.getString("focusName");entity.doGuiReset=entity.data.isEmpty()&&!entity.isEmpty()&&entity.vis<=0;
        }
        entity.doGather=true;flush();
    }
}
