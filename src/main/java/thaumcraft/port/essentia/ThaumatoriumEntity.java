package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.machine.*;
import thaumcraft.port.research.*;

public final class ThaumatoriumEntity extends EssentiaEntity implements WorldlyContainer,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public final MixedEssentia contents=new MixedEssentia(18500);
    public final LinkedHashMap<ResourceLocation,UUID> selected=new LinkedHashMap<>();
    public Aspect demand;
    public int capacity=1;
    public boolean heated;
    private CrucibleRecipe active;
    public ThaumatoriumEntity(BlockPos p,BlockState s){super(EssentiaContent.THAUMATORIUM_ENTITY.get(),p,s);}
    public boolean top(){return ((ThaumatoriumBlock)getBlockState().getBlock()).top;}
    public ThaumatoriumEntity main(){if(!top())return this;return level!=null&&level.hasChunkAt(worldPosition.below())&&level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumEntity be&&!be.top()?be:null;}
    public Direction front(){return getBlockState().getValue(ThaumatoriumBlock.FACING);}
    public boolean formed(){return level!=null&&level.hasChunkAt(worldPosition.above())&&level.getBlockState(worldPosition.above()).is(EssentiaContent.THAUMATORIUM_TOP.get())&&level.getBlockState(worldPosition.below()).is(MachineContent.CRUCIBLE.get());}
    public boolean powered(){return level.hasNeighborSignal(worldPosition)||level.hasNeighborSignal(worldPosition.above())||level.hasNeighborSignal(worldPosition.below());}
    public boolean connects(Direction d){return d!=null&&d!=front();}
    public boolean input(Direction d){return connects(d)&&d!=Direction.DOWN&&(top()||d!=Direction.UP);}
    public boolean output(Direction d){return false;}
    public Aspect aspect(Direction d){return null;}
    public int amount(Direction d){return 0;}
    public Aspect suctionType(Direction d){var be=main();return be==null?null:be.demand;}
    public int suction(Direction d){var be=main();return input(d)&&be!=null&&be.demand!=null&&be.heated&&be.formed()&&!be.powered()?128:0;}
    public int minimumSuction(){return 0;}
    public int extract(Aspect a,int n,Direction d,boolean simulate){return 0;}
    public int insert(Aspect a,int n,Direction d,boolean simulate){
        var be=main();if(!input(d)||be==null||be.active==null||a==null||a!=be.demand||suction(d)==0)return 0;
        int accepted=be.contents.insert(a,Math.min(n,be.active.aspects().getOrDefault(a.id(),0)-be.contents.amount(a)),simulate);if(accepted>0&&!simulate)be.sync();return accepted;
    }
    public RecipeHolder<CrucibleRecipe> recipe(ResourceLocation id){if(level==null)return null;var holder=level.getRecipeManager().byKey(id).orElse(null);return holder!=null&&holder.value() instanceof CrucibleRecipe r?new RecipeHolder<>(id,r):null;}
    public boolean permitted(UUID player,CrucibleRecipe recipe){return level instanceof ServerLevel s&&ResearchStorage.get(s.getServer()).player(player).knows(recipe.research());}
    public List<RecipeHolder<CrucibleRecipe>> choices(ServerPlayer player){
        return level.getRecipeManager().getAllRecipesFor(MachineContent.CRUCIBLE_RECIPE.get()).stream().filter(h->selected.containsKey(h.id())||(h.value().catalyst().test(items.get(0))&&permitted(player.getUUID(),h.value()))).sorted(Comparator.comparing(h->h.id().toString())).toList();
    }
    public void choose(ServerPlayer player,ResourceLocation id){
        if(!stillValid(player))return;
        if(selected.remove(id)!=null){demand=null;active=null;sync();return;}
        var r=recipe(id);if(r==null||!r.value().catalyst().test(items.get(0))||!permitted(player.getUUID(),r.value())||selected.size()>=capacity)return;
        selected.put(id,player.getUUID());demand=null;active=null;sync();
    }
    private void updateEnvironment(){
        heated=CrucibleEntity.isHeatSource(level.getBlockState(worldPosition.below(2)));
        int upgrades=0;for(int y=0;y<2;y++)for(Direction d:Direction.values())if(d!=Direction.DOWN&&d!=front()){
            var p=worldPosition.above(y).relative(d);if(!level.hasChunkAt(p))continue;var state=level.getBlockState(p);if(state.is(EssentiaContent.BRAIN_BOX.get())&&state.getValue(BrainBoxBlock.FACING)==d.getOpposite())upgrades++;
        }
        capacity=1+upgrades*2;while(selected.size()>capacity){var last=new ArrayList<>(selected.keySet()).get(selected.size()-1);selected.remove(last);}
    }
    public static Aspect missing(CrucibleRecipe recipe,MixedEssentia contents){return recipe.aspects().keySet().stream().sorted().map(a->Aspect.byId(a).orElseThrow()).filter(a->contents.amount(a)<recipe.aspects().get(a.id())).findFirst().orElse(null);}
    @Override public void tick(){
        if(top()||!(level instanceof ServerLevel s)||s.getGameTime()%5!=0)return;
        Aspect old=demand;boolean wasHeated=heated;int oldCapacity=capacity;updateEnvironment();active=null;demand=null;
        RecipeHolder<CrucibleRecipe> chosen=null;UUID owner=null;
        if(formed()&&heated&&!powered()&&!items.get(0).isEmpty())for(var e:selected.entrySet()){
            var r=recipe(e.getKey());if(r!=null&&r.value().catalyst().test(items.get(0))&&permitted(e.getValue(),r.value())){chosen=r;owner=e.getValue();break;}
        }
        if(chosen!=null){
            active=chosen.value();demand=missing(active,contents);
            if(demand==null){
                ItemStack result=active.assemble(new SingleRecipeInput(items.get(0)),s.registryAccess());
                var facing=front();var drop=new ItemEntity(s,worldPosition.getX()+.5+facing.getStepX()*.8,worldPosition.getY()+.4,worldPosition.getZ()+.5+facing.getStepZ()*.8,result);
                drop.setDeltaMovement(facing.getStepX()*.1,.05,facing.getStepZ()*.1);
                // Only consume after the server accepts the output entity (mods may cancel spawning).
                if(s.addFreshEntity(drop)){items.get(0).shrink(1);contents.clear();var player=s.getServer().getPlayerList().getPlayer(owner);if(player!=null)ResearchProgress.craft(player,result);s.playSound(null,worldPosition,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.25f,2);s.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,drop.getX(),drop.getY(),drop.getZ(),4,.1,.1,.1,.02);sync();}
                active=null;
            }else {
                boolean pulled=false;
                for(int y=0;y<2&&!pulled;y++)if(level.getBlockEntity(worldPosition.above(y)) instanceof ThaumatoriumEntity sink)for(Direction d:Direction.values())if(EssentiaTransport.pull(sink,d,sink.neighbour(d))>0){pulled=true;break;}
            }
        }
        if(old!=demand||wasHeated!=heated||oldCapacity!=capacity)sync();
    }
    public int getContainerSize(){return 1;}
    public boolean isEmpty(){return items.get(0).isEmpty();}
    public ItemStack getItem(int i){return items.get(i);}
    public ItemStack removeItem(int i,int n){var stack=ContainerHelper.removeItem(items,i,n);if(!stack.isEmpty())sync();return stack;}
    public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    public void setItem(int i,ItemStack stack){items.set(i,stack);stack.limitSize(getMaxStackSize(stack));demand=null;active=null;sync();}
    public boolean stillValid(Player p){return !top()&&Container.stillValidBlockEntity(this,p);}
    public void clearContent(){items.clear();sync();}
    public int[] getSlotsForFace(Direction d){return new int[]{0};}
    public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction d){return true;}
    public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction d){return true;}
    public Component getDisplayName(){return EssentiaContent.THAUMATORIUM.get().getName();}
    public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new ThaumatoriumMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){
        super.saveAdditional(tag,r);if(top())return;ContainerHelper.saveAllItems(tag,items,r);var aspects=new CompoundTag();contents.values().forEach((a,n)->aspects.putInt(a.id(),n));tag.put("Contents",aspects);
        var list=new ListTag();selected.forEach((id,uuid)->{var value=new CompoundTag();value.putString("Recipe",id.toString());value.putUUID("Player",uuid);list.add(value);});tag.put("Recipes",list);tag.putInt("Capacity",capacity);tag.putBoolean("Heated",heated);if(demand!=null)tag.putString("Demand",demand.id());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){
        super.loadAdditional(tag,r);items.clear();ContainerHelper.loadAllItems(tag,items,r);var values=new EnumMap<Aspect,Integer>(Aspect.class);var aspects=tag.getCompound("Contents");for(Aspect a:Aspect.values())values.put(a,aspects.getInt(a.id()));contents.restore(values);
        selected.clear();var list=tag.getList("Recipes",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(19,list.size());i++){var v=list.getCompound(i);var id=ResourceLocation.tryParse(v.getString("Recipe"));if(id!=null&&v.hasUUID("Player"))selected.put(id,v.getUUID("Player"));}capacity=Math.clamp(tag.getInt("Capacity"),1,19);heated=tag.getBoolean("Heated");demand=readAspect(tag,"Demand");active=null;
    }
}
