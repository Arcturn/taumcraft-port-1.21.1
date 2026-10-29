package thaumcraft.port.theory;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.content.*;
import thaumcraft.port.research.*;

public final class ResearchTableEntity extends BlockEntity implements Container,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    TheorySession session;
    long revision;
    private boolean clientActive;
    public ResearchTableEntity(BlockPos p,BlockState s){super(TheoryContent.ENTITY.get(),p,s);}
    public boolean active(){return session!=null||clientActive;}
    public int getContainerSize(){return 2;}public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    public ItemStack getItem(int i){return items.get(i);}
    public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())setChanged();return s;}
    public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    public void setItem(int i,ItemStack s){items.set(i,s);s.limitSize(getMaxStackSize(s));setChanged();}
    public void clearContent(){items.clear();setChanged();}public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p);}
    @Override public boolean canPlaceItem(int i,ItemStack s){return i==0?s.is(PortContent.SCRIBING_TOOLS.get()):s.is(Items.PAPER);}
    public Component getDisplayName(){return Component.translatable("block.thaumcraft.research_table");}
    public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new TheoryMenu(id,inv,this,worldPosition);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);ContainerHelper.saveAllItems(tag,items,r);tag.putLong("TheoryRevision",revision);if(session!=null)tag.putString("TheoryV1",session.encode());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);items.clear();ContainerHelper.loadAllItems(tag,items,r);revision=Math.max(0,tag.getLong("TheoryRevision"));session=null;clientActive=tag.getBoolean("Active");if(tag.contains("TheoryV1"))try{session=TheorySession.decode(tag.getString("TheoryV1"));}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(ResearchTableEntity.class).warn("Rejected damaged theory at {}",worldPosition,ex);}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,items,r);tag.putBoolean("Active",session!=null);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public Set<String> nearbyAids(){
        var found=new LinkedHashSet<String>();if(level==null)return found;
        Map<String,String> objects=new HashMap<>();TheoryAids.DEFINITIONS.forEach((id,aid)->objects.put(aid.object(),id));
        for(int x=-4;x<=4;x++)for(int y=-1;y<=1;y++)for(int z=-4;z<=4;z++){
            var p=worldPosition.offset(x,y,z);if(!level.hasChunkAt(p))continue;String id=objects.get(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString());if(id!=null)found.add(id);
        }
        if(!level.getEntitiesOfClass(thaumcraft.port.world.CultistPortal.class,new AABB(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5).inflate(5),e->e.isAlive()).isEmpty())found.add("PortalCrimson");
        return found;
    }
    public int ink(){var s=getItem(0);return s.is(PortContent.SCRIBING_TOOLS.get())?Math.max(0,s.getMaxDamage()-s.getDamageValue()):0;}
    public boolean paper(int n){return n==0||getItem(1).is(Items.PAPER)&&getItem(1).getCount()>=n;}
    /** Menu id, per-open nonce and revision are checked by TheoryMenu before entering this transaction. */
    boolean action(ServerPlayer player,int action,Set<String> aids){
        if(!stillValid(player)||!player.isAlive())return false;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        if(session!=null&&!session.owner.equals(player.getUUID()))return false;
        if(action==0){
            if(session!=null||ink()<1||!paper(1)||aids.size()>=TheorySession.availableInspiration(state)||!nearbyAids().containsAll(aids))return false;
            session=new TheorySession(player.getUUID(),TheorySession.availableInspiration(state),aids);
        }else if(session==null)return false;
        else if(action==1){
            if(!paper(1)||session.complete()||!session.choices.isEmpty())return false;
            var next=session.copy();if(!next.draw(TheoryItems.context(player),TheoryItems.random(player)))return false;
            removeItem(1,1);session=next;
        }else if(action>=2&&action<=4){
            int index=action-2;if(index>=session.choices.size()||session.selected>=0||session.complete())return false;
            var card=session.choices.get(index);int inkCost=card.kind().equals("Scripting")?2:1,paperCost=card.kind().equals("Scripting")?1:0;
            if(ink()<inkCost||!paper(paperCost))return false;
            var take=TheoryItems.reserve(player.getInventory(),card.required());if(take==null)return false;
            var outcome=TheoryCards.activate(session,index,TheoryItems.context(player),TheoryItems.random(player));if(outcome==null)return false;
            if(outcome.observation()>state.knowledge("OBSERVATION",outcome.observationCategory()))return false;
            // Every check above is read-only. The server thread now commits once.
            for(int slot=0;slot<take.length;slot++)if(take[slot]>0)player.getInventory().removeItem(slot,take[slot]);
            if(paperCost>0)removeItem(1,paperCost);getItem(0).setDamageValue(getItem(0).getDamageValue()+inkCost);
            player.giveExperienceLevels(-outcome.levels());if(outcome.observation()>0)state.addKnowledge("OBSERVATION",outcome.observationCategory(),-outcome.observation());
            state.warp(state.permanentWarp(),state.normalWarp()+outcome.normalWarp(),state.temporaryWarp()+outcome.temporaryWarp());
            for(var q:outcome.outputs()){var stack=TheoryItems.stack(q);if(!player.getInventory().add(stack))player.drop(stack,false);}
            player.getInventory().setChanged();storage.setDirty();session=outcome.next();ResearchProgress.sync(player);
        }else if(action==5){if(session.selected<0)return false;session.acknowledge();}
        else if(action==6){
            if(!session.complete())return false;var rewards=session.rewards();
            for(var e:rewards.entrySet())if((long)state.knowledge("THEORY",e.getKey())+e.getValue()>1000000)return false;
            rewards.forEach((category,n)->state.addKnowledge("THEORY",category,n));session=null;storage.setDirty();ResearchProgress.sync(player);
        }else if(action==7){if(session.complete())return false;session=null;}
        else return false;
        revision++;setChanged();if(action==6)level.playSound(null,worldPosition,PortSounds.get("learn"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
    }
}
