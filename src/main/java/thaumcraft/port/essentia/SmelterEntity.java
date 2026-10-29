package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import thaumcraft.port.aspect.*;
import thaumcraft.port.world.AuraSystem;

public final class SmelterEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    public final SmelterState store=new SmelterState();
    public int burn,fuelTime,cook,smeltTime=1;
    public boolean boost;
    public final int tier;
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->burn;case 1->fuelTime;case 2->cook;case 3->smeltTime;case 4->store.total();default->0;};}
        public void set(int i,int v){} public int getCount(){return 5;}
    };
    public SmelterEntity(BlockPos p,BlockState s){super(EssentiaContent.SMELTER_ENTITY.get(),p,s);tier=((SmelterBlock)s.getBlock()).tier;}
    public static int fuel(ItemStack stack){return stack.isEmpty()?0:((IItemStackExtension)(Object)stack).getBurnTime(RecipeType.SMELTING);}
    public void tick(){
        if(!(level instanceof ServerLevel server))return;
        boolean dirty=false;if(burn>0){burn--;dirty=true;}
        int period=tier==1?10:15;if(boost)period=(int)(period*.8);
        if(server.getGameTime()%period==0){
            var outlets=new ArrayList<BlockPos>();outlets.add(worldPosition);outlets.addAll(attachments(EssentiaContent.AUX.get()));
            for(BlockPos outlet:outlets)for(Aspect a:new ArrayList<>(store.aspects().keySet()))if(distill(a,outlet)){store.take(a);dirty=true;break;}
        }
        Map<Aspect,Integer> tags=RecipeAspects.get(items.get(0),server);
        boolean can=store.fits(tags);smeltTime=can?SmelterUpgrades.cookTime(tags.values().stream().mapToInt(Integer::intValue).sum(),BellowsEntity.count(level,worldPosition,getBlockState().getValue(SmelterBlock.FACING))):1;
        if(burn==0&&can){
            int duration=fuel(items.get(1));
            if(duration>0){
                ItemStack stack=items.get(1);boost=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("alumentum");
                var remainder=((IItemStackExtension)(Object)stack).getCraftingRemainingItem();
                stack.shrink(1);if(stack.isEmpty())items.set(1,remainder);burn=fuelTime=duration;dirty=true;
            }
        }
        if(burn>0&&can){
            cook++;dirty=true;
            if(cook>=smeltTime){int pollution=store.smelt(tags,tier,()->server.random.nextFloat());if(pollution>=0){items.get(0).shrink(1);if(pollution>0){var vents=attachments(EssentiaContent.VENT.get());int remaining=SmelterUpgrades.pollution(pollution,vents.size(),()->server.random.nextFloat());if(remaining>0)AuraSystem.add(server,worldPosition,remaining,true);if(remaining<pollution)for(BlockPos vent:vents)server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,vent.getX()+.5,vent.getY()+.5,vent.getZ()+.5,5,.2,.2,.2,.03);}}cook=0;}
        }else if(cook!=0){cook=0;dirty=true;}
        if(getBlockState().getValue(SmelterBlock.LIT)!=(burn>0))server.setBlock(worldPosition,getBlockState().setValue(SmelterBlock.LIT,burn>0),3);
        if(dirty)setChanged();
    }
    private List<BlockPos> attachments(net.minecraft.world.level.block.Block block){
        var found=new ArrayList<BlockPos>();Direction front=getBlockState().getValue(SmelterBlock.FACING);
        for(Direction d:Direction.Plane.HORIZONTAL)if(d!=front){BlockPos p=worldPosition.relative(d);if(!level.hasChunkAt(p))continue;var s=level.getBlockState(p);if(s.is(block)&&s.getValue(SmelterAttachmentBlock.FACING)==d.getOpposite())found.add(p);}return found;
    }
    private boolean distill(Aspect a,BlockPos outlet){
        var alembics=new ArrayList<JarEntity>();
        for(int n=1;n<level.getMaxBuildHeight()-worldPosition.getY();n++){
            BlockPos p=outlet.above(n);if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof JarEntity jar)||!jar.alembic)break;alembics.add(jar);
        }
        for(var jar:alembics)if(jar.tank.aspect()==a&&jar.fill(a,1,false)==1)return true;
        for(var jar:alembics)if(jar.fill(a,1,false)==1)return true;
        return false;
    }
    public int getContainerSize(){return 2;}
    public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    public ItemStack getItem(int i){return items.get(i);}
    public ItemStack removeItem(int i,int n){var stack=ContainerHelper.removeItem(items,i,n);if(!stack.isEmpty())setChanged();return stack;}
    public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    public void setItem(int i,ItemStack stack){items.set(i,stack);stack.limitSize(getMaxStackSize(stack));setChanged();}
    public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p);}
    public void clearContent(){items.clear();setChanged();}
    public int[] getSlotsForFace(Direction d){return d==Direction.UP?new int[0]:d==Direction.DOWN?new int[]{1}:new int[]{0};}
    public boolean canPlaceItem(int i,ItemStack stack){return i==1?fuel(stack)>0:level instanceof ServerLevel server&&!RecipeAspects.get(stack,server).isEmpty();}
    public boolean canPlaceItemThroughFace(int i,ItemStack stack,Direction d){return d!=Direction.UP&&canPlaceItem(i,stack);}
    public boolean canTakeItemThroughFace(int i,ItemStack stack,Direction d){return true;}
    public Component getDisplayName(){return getBlockState().getBlock().getName();}
    public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new SmelterMenu(id,inv,this,data);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){
        super.saveAdditional(tag,r);ContainerHelper.saveAllItems(tag,items,r);tag.putInt("Burn",burn);tag.putInt("FuelTime",fuelTime);tag.putInt("Cook",cook);tag.putInt("SmeltTime",smeltTime);tag.putBoolean("Boost",boost);
        var values=new CompoundTag();store.aspects().forEach((a,n)->values.putInt(a.id(),n));tag.put("Aspects",values);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){
        super.loadAdditional(tag,r);items.clear();ContainerHelper.loadAllItems(tag,items,r);burn=Math.clamp(tag.getInt("Burn"),0,1000000);fuelTime=Math.clamp(tag.getInt("FuelTime"),burn,1000000);cook=Math.clamp(tag.getInt("Cook"),0,512);smeltTime=Math.clamp(tag.getInt("SmeltTime"),1,512);boost=tag.getBoolean("Boost");
        var values=tag.getCompound("Aspects");var map=new EnumMap<Aspect,Integer>(Aspect.class);for(Aspect a:Aspect.values())map.put(a,values.getInt(a.id()));store.restore(map);
    }
}
