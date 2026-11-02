package thaumcraft.port.golem.builder;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.essentia.*;
import thaumcraft.port.golem.*;
import thaumcraft.port.research.ResearchStorage;

/** Original press: paid parts, one Machina per five ticks, one output slot. */
public final class GolemBuilderEntity extends BlockEntity implements Container,MenuProvider,EssentiaTransport {
    private ItemStack output=ItemStack.EMPTY;
    public long golem=-1;
    public int cost,maxCost,press,previousPress;
    private boolean bufferedEssentia;
    private int ticks;
    public static java.util.function.Consumer<GolemBuilderEntity> clientTick=builder->{};
    public GolemBuilderEntity(BlockPos pos,BlockState state){super(GolemContent.BUILDER_ENTITY.get(),pos,state);}
    public boolean busy(){return golem>=0;}
    public GolemProperties properties(){try{return GolemProperties.fromLong(golem);}catch(IllegalArgumentException e){return null;}}
    public static ItemStack placer(long properties){var out=new ItemStack(GolemContent.GOLEM_ITEM.get());CustomData.update(DataComponents.CUSTOM_DATA,out,t->t.putLong("props",properties));return out;}
    public boolean mayBuild(ServerPlayer player,GolemProperties props){var knowledge=ResearchStorage.get(player.getServer()).player(player.getUUID());for(String key:props.requiredResearch())if(!knowledge.knows(key))return false;return props.getRank()==0;}
    private record Take(IItemHandler inventory,int slot,int count,ItemStack expected){}
    private List<IItemHandler> adjacent(){var out=new ArrayList<IItemHandler>();if(level==null)return out;for(Direction face:Direction.values())if(face!=Direction.UP){var other=worldPosition.relative(face);if(!level.hasChunkAt(other))continue;var inventory=level.getCapability(Capabilities.ItemHandler.BLOCK,other,face.getOpposite());if(inventory!=null)out.add(inventory);}return out;}
    private static boolean matches(ItemStack required,ItemStack actual){
        if(thaumcraft.port.artifice.device.ArtificeDeviceEntity.sameMaterial(required,actual))return true;
        // Original plateIron/Brass/Thaumium/Void OreDictionary entries also accept other mods' plates.
        return !actual.isEmpty()&&actual.getComponentsPatch().isEmpty()&&required.getTags().filter(t->t.location().getNamespace().equals("c")&&t.location().getPath().startsWith("plates/")).anyMatch(actual::is);
    }
    private static List<Take> reserve(ItemStack required,List<IItemHandler> inventories,List<Take> already){
        int need=required.getCount();var out=new ArrayList<Take>();
        for(var inv:inventories)for(int slot=0;slot<inv.getSlots()&&need>0;slot++){
            int used=0;for(var r:already)if(r.inventory==inv&&r.slot==slot)used+=r.count;
            var stack=inv.extractItem(slot,need+used,true);if(!matches(required,stack))continue;
            int amount=Math.min(need,Math.max(0,stack.getCount()-used));if(amount>0){out.add(new Take(inv,slot,amount,inv.getStackInSlot(slot).copy()));need-=amount;}
        }
        return need==0?out:null;
    }
    public boolean[] checkCraft(long id,Player player){
        final GolemProperties props;try{props=GolemProperties.fromLong(id);}catch(IllegalArgumentException e){return new boolean[0];}
        var parts=props.generateComponents();var found=new boolean[parts.length];var adjacent=adjacent();var personal=List.<IItemHandler>of(new PlayerMainInvWrapper(player.getInventory()));
        for(int i=0;i<parts.length;i++)found[i]=reserve(parts[i],adjacent,List.of())!=null||reserve(parts[i],personal,List.of())!=null;
        return found;
    }
    public boolean startCraft(long id,ServerPlayer player){
        if(level==null||level.isClientSide||busy()||!stillValid(player)||!player.isAlive())return false;
        final GolemProperties props;try{props=GolemProperties.fromLong(id);}catch(IllegalArgumentException e){return false;}
        if(!mayBuild(player,props))return false;var result=placer(id);
        if(!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()>=output.getMaxStackSize()))return false;
        var adjacent=adjacent();var personal=List.<IItemHandler>of(new PlayerMainInvWrapper(player.getInventory()));var reservation=new ArrayList<Take>();
        for(var part:props.generateComponents()){
            var take=reserve(part,adjacent,reservation);if(take==null)take=reserve(part,personal,reservation);if(take==null)return false;reservation.addAll(take);
        }
        for(var take:reservation)if(!ItemStack.isSameItemSameComponents(take.inventory.getStackInSlot(take.slot),take.expected)||take.inventory.getStackInSlot(take.slot).getCount()!=take.expected.getCount())return false;
        var paid=new ArrayList<Take>();
        for(var take:reservation){var extracted=take.inventory.extractItem(take.slot,take.count,false);if(!extracted.isEmpty())paid.add(new Take(take.inventory,take.slot,extracted.getCount(),extracted));
            if(extracted.getCount()!=take.count||!ItemStack.isSameItemSameComponents(extracted,take.expected)){
                for(var restore:paid){var left=restore.inventory.insertItem(restore.slot,restore.expected.copyWithCount(restore.count),false);if(!left.isEmpty()&&!player.getInventory().add(left))player.drop(left,false);}return false;
            }
        }
        golem=id;cost=maxCost=props.constructionCost();bufferedEssentia=false;sync();sound("wand",.25f);return true;
    }
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){previousPress=press;if(press<90&&cost>0&&golem>=0)press=Math.min(90,press+6);if(press>0&&(cost<=0||golem<0))press=Math.max(0,press-3);clientTick.accept(this);return;}
        if(++ticks%5!=0||golem<0)return;
        if(cost>0){if(!bufferedEssentia)drawEssentia();if(bufferedEssentia){bufferedEssentia=false;--cost;sync();}}
        // Keep a fully paid result pending if external automation temporarily blocks the output.
        if(cost==0){var result=placer(golem);if(output.isEmpty()){output=result;golem=-1;sync();sound("wand",1);}
            else if(ItemStack.isSameItemSameComponents(output,result)&&output.getCount()<output.getMaxStackSize()){output.grow(1);golem=-1;sync();sound("wand",1);}}
    }
    private void drawEssentia(){for(Direction face:Direction.values()){if(!connects(face))continue;var pos=worldPosition.relative(face);if(!level.hasChunkAt(pos))continue;var source=level.getCapability(EssentiaContent.CAPABILITY,pos,face.getOpposite());if(source==null)continue;if(!source.output(face.getOpposite()))return;if(EssentiaTransport.pull(this,face,source)==1)return;}}
    private void sound(String sound,float volume){level.playSound(null,worldPosition,PortSounds.get(sound),SoundSource.BLOCKS,volume,1);}
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean connects(Direction side){return side!=null&&side!=Direction.UP;}public boolean input(Direction side){return connects(side);}public boolean output(Direction side){return false;}
    public Aspect aspect(Direction side){return null;}public int amount(Direction side){return 0;}public Aspect suctionType(Direction side){return Aspect.MECHANISM;}
    public int suction(Direction side){return cost>0&&golem>=0?128:0;}public int minimumSuction(){return 0;}
    public int insert(Aspect aspect,int amount,Direction side,boolean simulate){if(!connects(side)||amount<=0||aspect!=Aspect.MECHANISM||bufferedEssentia||cost<=0||golem<0)return 0;if(!simulate){bufferedEssentia=true;setChanged();}return 1;}
    public int extract(Aspect aspect,int amount,Direction side,boolean simulate){return 0;}
    @Override public int getContainerSize(){return 1;}@Override public boolean isEmpty(){return output.isEmpty();}@Override public ItemStack getItem(int slot){return slot==0?output:ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){if(slot!=0)return ItemStack.EMPTY;var result=output.split(count);sync();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(slot!=0)return ItemStack.EMPTY;var result=output;output=ItemStack.EMPTY;sync();return result;}
    @Override public void setItem(int slot,ItemStack stack){if(slot==0){output=stack;sync();}}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&stack.is(GolemContent.GOLEM_ITEM.get());}@Override public void clearContent(){output=ItemStack.EMPTY;sync();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    @Override public Component getDisplayName(){return Component.translatable("tile.golem_builder.name");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new GolemBuilderMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putLong("golem",golem);tag.putInt("cost",cost);tag.putInt("mcost",maxCost);tag.putBoolean("buffered",bufferedEssentia);if(!output.isEmpty())tag.put("output",output.save(registries));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);golem=tag.contains("golem")?tag.getLong("golem"):-1;cost=Math.max(0,tag.getInt("cost"));maxCost=Math.max(cost,tag.getInt("mcost"));bufferedEssentia=tag.getBoolean("buffered");output=ItemStack.parseOptional(registries,tag.getCompound("output"));if(golem>=0&&properties()==null){golem=-1;cost=0;bufferedEssentia=false;}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries){loadAdditional(tag,registries);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries){handleUpdateTag(packet.getTag(),registries);}
}
