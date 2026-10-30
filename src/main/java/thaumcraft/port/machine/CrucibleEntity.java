package thaumcraft.port.machine;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;
import thaumcraft.port.research.*;
import thaumcraft.port.world.AuraSystem;

public final class CrucibleEntity extends BlockEntity {
    public static final String OUTPUT_TAG="thaumcraft_crucible_output";
    public final CrucibleState state=new CrucibleState();
    private int lastWater=-1,lastHeat=-1,lastTotal=-1;
    private Map<Aspect,Integer> lastAspects=Map.of();
    public final IFluidHandler fluidHandler=new IFluidHandler(){
        @Override public int getTanks(){return 1;}
        @Override public FluidStack getFluidInTank(int tank){return tank==0&&state.water()>0?new FluidStack(Fluids.WATER,state.water()):FluidStack.EMPTY;}
        @Override public int getTankCapacity(int tank){return tank==0?1000:0;}
        @Override public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&fluid.is(Fluids.WATER);}
        @Override public int fill(FluidStack fluid,FluidAction action){
            if(!isFluidValid(0,fluid))return 0;
            int filled=state.fill(fluid.getAmount(),action.simulate());
            if(filled>0&&action.execute())sync();
            return filled;
        }
        @Override public FluidStack drain(FluidStack fluid,FluidAction action){return fluid.is(Fluids.WATER)?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        @Override public FluidStack drain(int amount,FluidAction action){
            int drained=state.drain(amount,action.simulate());
            if(drained>0&&action.execute())sync();
            return drained==0?FluidStack.EMPTY:new FluidStack(Fluids.WATER,drained);
        }
    };
    public CrucibleEntity(BlockPos pos,BlockState block){super(MachineContent.CRUCIBLE_ENTITY.get(),pos,block);}

    /** TC6's fire/lava/nitor/magma sources, with lit modern campfires using the same rate. */
    public static boolean isHeatSource(BlockState below){
        return below.is(BlockTags.FIRE)||below.getFluidState().is(FluidTags.LAVA)||below.is(Blocks.MAGMA_BLOCK)||
            (below.getBlock() instanceof CampfireBlock&&below.getValue(CampfireBlock.LIT)&&!below.getValue(CampfireBlock.WATERLOGGED))||
            below.getBlock() instanceof NitorBlock;
    }
    public static void tick(Level world,BlockPos pos,BlockState block,CrucibleEntity be){
        if(world.isClientSide){CrucibleVisualBridge.tick.accept(be);return;}
        if(!(world instanceof ServerLevel level))return;
        boolean changed=be.state.tick(isHeatSource(level.getBlockState(pos.below())));
        if(be.state.total()>500){be.spill(level);changed=true;}
        if(be.state.periodicSpillDue()&&be.state.total()>0){be.spill(level);changed=true;}
        if(changed)be.setChanged();
        // The exact 150/151 transition must be synchronized, even within a ten-tick heat bucket.
        if(be.lastWater!=be.state.water()||be.lastTotal!=be.state.total()||!be.lastAspects.equals(be.state.aspects())||
            be.lastHeat/10!=be.state.heat()/10||(be.lastHeat>150)!=(be.state.heat()>150))be.sync();
    }
    private void spill(ServerLevel server){AuraSystem.pollute(server,worldPosition,state.spill(server.random.nextInt(state.aspects().size())),true);}

    /** Process one collision batch. TC6's shrinking loop consumes ceil(stack/2) each collision. */
    public boolean acceptItem(ItemStack stack,ServerPlayer player){
        if(!(level instanceof ServerLevel server)||stack.isEmpty()||!state.boiling())return false;
        boolean dissolved=false,crafted=false;
        int remaining=stack.getCount();
        for(int processed=0;processed<remaining;processed++){
            var recipe=server.getRecipeManager().getRecipesFor(MachineContent.CRUCIBLE_RECIPE.get(),new SingleRecipeInput(stack),server).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(r->player!=null&&ResearchStorage.get(server.getServer()).player(player.getUUID()).knows(r.research())&&state.contains(r.aspects()))
                .max(Comparator.comparingInt(r->r.aspects().values().stream().mapToInt(Integer::intValue).sum()));
            if(recipe.isPresent()&&state.craft(recipe.get().aspects())){
                ItemStack output=recipe.get().result().copy();
                ejectItem(server,output.copy());
                ResearchProgress.craft(player,output);
                crafted=true;remaining--;
            }else if(state.dissolveInBatch(RecipeAspects.get(stack,server))){dissolved=true;remaining--;}
        }
        if(!dissolved&&!crafted)return false;
        stack.setCount(remaining);
        sync();
        if(dissolved){
            server.playSound(null,worldPosition,PortSounds.get("bubble"),SoundSource.BLOCKS,.2f,1+server.random.nextFloat()*.4f);
            server.blockEvent(worldPosition,getBlockState().getBlock(),2,1);
        }
        if(crafted)server.blockEvent(worldPosition,getBlockState().getBlock(),99,0);
        return true;
    }
    private void ejectItem(ServerLevel server,ItemStack output){
        boolean first=true;
        while(!output.isEmpty()){
            ItemStack split=output.split(Math.min(output.getCount(),output.getMaxStackSize()));
            var spawned=new SpecialItemEntity(server,worldPosition.getX()+.5,worldPosition.getY()+.71,worldPosition.getZ()+.5,split);
            spawned.setDeltaMovement(first?0:(server.random.nextFloat()-server.random.nextFloat())*.01,.075,
                first?0:(server.random.nextFloat()-server.random.nextFloat())*.01);
            spawned.addTag(OUTPUT_TAG);server.addFreshEntity(spawned);first=false;
        }
    }
    public void collide(ItemEntity item){
        if(item.getTags().contains(OUTPUT_TAG)||item instanceof SpecialItemEntity||!item.isAlive())return;
        ItemStack stack=item.getItem();
        if(acceptItem(stack,item.getOwner() instanceof ServerPlayer player?player:null)){
            if(stack.isEmpty())item.discard();else item.setItem(stack);
        }
    }
    public void sync(){
        setChanged();lastWater=state.water();lastHeat=state.heat();lastTotal=state.total();lastAspects=Map.copyOf(state.aspects());
        if(level!=null){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
    }
    public void empty(){
        if(level instanceof ServerLevel server&&(state.water()>0||state.total()>0)){
            // Only the base pollution is visible; Vitium's extra flux has no second smoke burst.
            float visiblePollution=state.total()*.25f;
            float pollution=state.empty();
            AuraSystem.pollute(server,worldPosition,visiblePollution,true);
            AuraSystem.pollute(server,worldPosition,pollution-visiblePollution,false);
            sync();
            server.blockEvent(worldPosition,getBlockState().getBlock(),2,5);
        }
    }
    @Override public boolean triggerEvent(int id,int parameter){
        if(id==1||id==2||id==99)return level!=null&&(!level.isClientSide||CrucibleVisualBridge.event.handle(this,id,parameter));
        return super.triggerEvent(id,parameter);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putInt("water",state.water());tag.putInt("heat",state.heat());tag.putInt("timer",state.timer());
        var aspects=new CompoundTag();state.aspects().forEach((a,n)->aspects.putInt(a.id(),n));tag.put("aspects",aspects);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);var values=new EnumMap<Aspect,Integer>(Aspect.class);var aspects=tag.getCompound("aspects");
        aspects.getAllKeys().forEach(k->Aspect.byId(k).ifPresent(a->values.put(a,aspects.getInt(k))));
        state.restore(tag.getInt("water"),tag.getInt("heat"),tag.contains("timer")?tag.getInt("timer"):-100,values);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
