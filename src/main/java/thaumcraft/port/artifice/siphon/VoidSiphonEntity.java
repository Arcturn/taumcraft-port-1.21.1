package thaumcraft.port.artifice.siphon;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import thaumcraft.port.world.FluxRift;
import thaumcraft.port.world.WorldContent;

/** BETA26 void-seed extraction: visible rifts buy progress at the cost of their stability. */
public final class VoidSiphonEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    public static final int REQUIRED_PROGRESS=2000;
    private static final int[] SLOTS={0};
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private int counter,progress;
    public final ContainerData data=new ContainerData(){
        public int get(int index){return index==0?progress&65535:index==1?progress>>>16:0;}
        public void set(int index,int value){}public int getCount(){return 2;}
    };
    public VoidSiphonEntity(BlockPos pos,BlockState state){super(VoidSiphonContent.ENTITY.get(),pos,state);}
    public int progress(){return progress;}
    private static Item seed(){return BuiltInRegistries.ITEM.get(WorldContent.id("void_seed"));}
    private boolean canOutput(){var stack=items.getFirst();return stack.isEmpty()||stack.is(seed())&&stack.getCount()<getMaxStackSize(stack);}
    public void tick(){
        if(!(level instanceof ServerLevel server)||++counter%20!=0||!getBlockState().getValue(VoidSiphonBlock.ENABLED)||!canOutput())return;
        int previous=progress;
        // A full stack can leave completed progress pending. Flush it without charging rifts again.
        if(progress<REQUIRED_PROGRESS){
            var rifts=validRifts();
            for(var rift:rifts){
                double amount=Math.sqrt(rift.size());
                progress=(int)Math.min(Integer.MAX_VALUE,(long)progress+(int)amount);
                rift.addStability((float)(-amount/15));
                if(server.random.nextInt(33)==0)rift.shrinkForSiphon();
            }
            if(!rifts.isEmpty()&&counter%40==0)server.blockEvent(worldPosition,getBlockState().getBlock(),5,counter);
        }
        boolean produced=false;
        while(progress>=REQUIRED_PROGRESS&&canOutput()){
            progress-=REQUIRED_PROGRESS;
            if(items.getFirst().isEmpty())items.set(0,new ItemStack(seed()));else items.getFirst().grow(1);
            produced=true;
        }
        // Preserve partial progress on unload, not only after a seed has been produced.
        if(progress!=previous||produced)setChanged();
    }
    public List<FluxRift> validRifts(){
        if(level==null)return List.of();
        var center=Vec3.atCenterOf(worldPosition);
        return level.getEntitiesOfClass(FluxRift.class,new AABB(center,center).inflate(8),rift->rift.isAlive()&&rift.size()>=2&&visible(rift));
    }
    private boolean visible(FluxRift rift){
        var origin=Vec3.atBottomCenterOf(worldPosition).add(0,1,0);
        var end=rift.position();var start=origin.add(end.subtract(origin).normalize());
        // Do not make a line-of-sight query load chunks across an unloaded boundary.
        var bounds=new AABB(start,end);
        if(!level.hasChunksAt(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ)))return false;
        return level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,rift)).getType()==HitResult.Type.MISS;
    }
    @Override public int[] getSlotsForFace(Direction side){return SLOTS;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return false;}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.getFirst().isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?items.getFirst():ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){if(slot!=0)return ItemStack.EMPTY;var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return slot==0?ContainerHelper.takeItem(items,slot):ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack stack){if(slot!=0)return;items.set(0,stack);stack.limitSize(getMaxStackSize(stack));setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new VoidSiphonMenu(id,inventory,this);}
    @Override public boolean triggerEvent(int id,int parameter){if(id==5){if(level!=null&&level.isClientSide)VoidSiphonContent.stream.accept(this,parameter);return true;}return super.triggerEvent(id,parameter);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);ContainerHelper.saveAllItems(tag,items,lookup);tag.putInt("progress",progress);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);items.clear();ContainerHelper.loadAllItems(tag,items,lookup);progress=Math.max(0,tag.getInt("progress"));}
}
