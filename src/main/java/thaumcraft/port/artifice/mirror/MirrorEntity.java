package thaumcraft.port.artifice.mirror;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.content.InfusionMirrorBlock;
import thaumcraft.port.world.AuraSystem;

/** TC6 mirror pair, delayed item ejection and remote, one-unit essentia transactions. */
public final class MirrorEntity extends BlockEntity implements EssentiaSource {
    public static Consumer<MirrorEntity> effect=mirror->{};
    public MirrorLink destination;
    public boolean linked;
    public int instability;
    private int ticks,interval=40;
    private final List<ItemStack> output=new ArrayList<>();
    private final RemoteEssentia remote=new RemoteEssentia(8,source->!(source instanceof MirrorEntity));
    public MirrorEntity(BlockPos pos,BlockState state){super(MirrorContent.ENTITY.get(),pos,state);}
    public boolean essentia(){return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath().equals("mirror_essentia");}
    public Direction facing(){return getBlockState().getValue(InfusionMirrorBlock.FACING);}
    public MirrorLink address(){return level==null?null:new MirrorLink(level.dimension(),worldPosition);}
    public static MirrorEntity resolve(ServerLevel source,MirrorLink link){
        if(link==null)return null;var world=source.getServer().getLevel(link.dimension());
        // An unloaded target is unavailable, never an instruction to load or discard its link.
        return world!=null&&world.hasChunkAt(link.pos())&&world.getBlockEntity(link.pos()) instanceof MirrorEntity mirror?mirror:null;
    }
    private boolean targetLoaded(){if(!(level instanceof ServerLevel server)||destination==null)return false;var world=server.getServer().getLevel(destination.dimension());return world!=null&&world.hasChunkAt(destination.pos());}
    public MirrorEntity peer(){
        if(!(level instanceof ServerLevel server)||!linked)return null;
        var other=resolve(server,destination);
        return other!=null&&other!=this&&other.essentia()==essentia()&&other.linked&&address().equals(other.destination)?other:null;
    }
    public void bind(MirrorLink link){destination=link;linked=false;sync();restoreLink();}
    public boolean restoreLink(){
        if(!(level instanceof ServerLevel server)||destination==null||destination.equals(address()))return false;
        var other=resolve(server,destination);
        if(other==null||other.essentia()!=essentia())return false;
        if(other.linked&&!address().equals(other.destination)){
            if(other.peer()!=null||!other.targetLoaded())return false;
        }
        other.destination=address();other.linked=true;linked=true;other.sync();sync();return true;
    }
    public void unlink(){
        if(level instanceof ServerLevel server){var other=resolve(server,destination);if(other!=null&&address().equals(other.destination)){other.linked=false;other.sync();}}
        linked=false;sync();
    }
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        if(!essentia()&&ticks>20&&!output.isEmpty()){
            int index=server.random.nextInt(output.size());var stack=output.get(index);
            if(spawn(stack.copyWithCount(1))){stack.shrink(1);if(stack.isEmpty())output.remove(index);addInstability(1);pulse();}
        }
        int threshold=essentia()?64:128;
        if(instability>threshold){AuraSystem.pollute(server,worldPosition,1,true);instability-=threshold;setChanged();}
        if(instability>0&&ticks%100==0){instability--;setChanged();}
        if(ticks++%interval==0){
            if(peer()!=null)interval=40;
            else {if(linked&&targetLoaded()){linked=false;sync();}interval=Math.min(600,interval+20);restoreLink();}
        }
    }
    private void addInstability(int amount){instability=(int)Math.min(Integer.MAX_VALUE,(long)instability+amount);setChanged();}
    private void pulse(){if(level!=null)level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);}
    public boolean transport(ItemEntity entity){
        if(essentia()||entity.isRemoved()||entity.hasPickUpDelay()||entity.level()!=level)return false;
        var target=peer();var stack=entity.getItem();if(target==null||stack.isEmpty()||!target.enqueue(stack))return false;
        addInstability(stack.getCount());entity.discard();pulse();return true;
    }
    /** Hand mirrors deposit directly, even when this receiving mirror has no placed partner. */
    public boolean enqueue(ItemStack stack){
        if(!(level instanceof ServerLevel)||essentia()||stack.isEmpty())return false;
        // Only owned copies enter the persisted queue; callers keep ownership on failure.
        output.add(stack.copy());setChanged();return true;
    }
    public int queuedCount(){return output.stream().mapToInt(ItemStack::getCount).sum();}
    private boolean spawn(ItemStack stack){
        if(!(level instanceof ServerLevel server)||stack.isEmpty())return false;
        var f=facing();var item=new ItemEntity(server,worldPosition.getX()+.5,worldPosition.getY()+.25,worldPosition.getZ()+.5,stack);
        item.setDeltaMovement(f.getStepX()*.15,f.getStepY()*.15,f.getStepZ()*.15);item.setPickUpDelay(20);
        return server.addFreshEntity(item);
    }
    /** Breaking/replacing a mirror refunds its paid queue instead of deleting in-flight items. */
    public void recover(){
        if(!(level instanceof ServerLevel))return;
        unlink();for(var stack:output)spawn(stack.copy());output.clear();setChanged();
    }
    public final IItemHandler items=new IItemHandler(){
        public int getSlots(){return 1;}
        public ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 64;}
        public boolean isItemValid(int slot,ItemStack stack){return slot==0&&!stack.isEmpty()&&peer()!=null;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            var target=slot==0?peer():null;if(target==null||stack.isEmpty())return stack;
            if(!simulate){if(!target.enqueue(stack))return stack;addInstability(stack.getCount());pulse();}
            return ItemStack.EMPTY;
        }
    };
    public boolean blocked(){return false;}
    public Aspect storedAspect(){return null;}
    public int storedAmount(){return 0;}
    public int takeRemote(Aspect aspect,int amount,boolean simulate){return transfer(aspect,amount,simulate,false);}
    public int addRemote(Aspect aspect,int amount,boolean simulate){return transfer(aspect,amount,simulate,true);}
    private int transfer(Aspect aspect,int amount,boolean simulate,boolean insert){
        if(!essentia()||aspect==null||amount!=1)return 0;var target=peer();
        if(target==null||!(target.level instanceof ServerLevel world))return 0;
        var provider=insert?target.remote.accepting(world,target.worldPosition,target.facing(),aspect):target.remote.providing(world,target.worldPosition,target.facing(),aspect);
        if(provider==null)return 0;
        if(simulate)return 1;
        int moved=insert?provider.source().addRemote(aspect,1,false):provider.source().takeRemote(aspect,1,false);
        if(moved==1){addInstability(1);EssentiaStreamPayload.send(world,insert?target.worldPosition:provider.pos(),insert?provider.pos():target.worldPosition,aspect);}
        return moved;
    }
    public void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public boolean triggerEvent(int id,int data){if(id==1){if(level!=null&&level.isClientSide)effect.accept(this);return true;}return super.triggerEvent(id,data);}
    private void writeVisual(CompoundTag tag){tag.putBoolean("linked",linked);tag.putInt("instability",instability);if(destination!=null)tag.put("destination",destination.save());}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();writeVisual(tag);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);writeVisual(tag);var list=new ListTag();for(var stack:output)if(!stack.isEmpty())list.add(stack.save(registries));tag.put("Items",list);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);linked=tag.getBoolean("linked");instability=Math.max(0,tag.getInt("instability"));destination=MirrorLink.read(tag.getCompound("destination"));
        output.clear();for(var saved:tag.getList("Items",Tag.TAG_COMPOUND))ItemStack.parse(registries,saved).filter(s->!s.isEmpty()).ifPresent(output::add);
    }
}
