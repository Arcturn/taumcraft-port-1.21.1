package thaumcraft.port.alchemy.devices;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.port.world.AuraSystem;

/** Original TileWaterJug: 0.1 vis per 100 mB, and one scanned position every five ticks. */
public final class EverfullUrnEntity extends BlockEntity {
    public static final int CAPACITY=1000;
    private int water,counter,zone;
    private final Set<Integer> targets=new LinkedHashSet<>();
    public int trailTicks,trailZone;
    public final IFluidHandler fluidHandler=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return tank==0&&water>0?new FluidStack(Fluids.WATER,water):FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank==0?CAPACITY:0;}
        public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&fluid.is(Fluids.WATER);}
        public int fill(FluidStack fluid,FluidAction action){
            if(!isFluidValid(0,fluid))return 0;
            int amount=Math.min(CAPACITY-water,Math.max(0,fluid.getAmount()));
            if(action.execute()&&amount>0){water+=amount;sync();}return amount;
        }
        public FluidStack drain(FluidStack fluid,FluidAction action){return fluid.is(Fluids.WATER)?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        public FluidStack drain(int amount,FluidAction action){
            int drained=Math.min(water,Math.max(0,amount));
            if(action.execute()&&drained>0){water-=drained;sync();}
            return drained>0?new FluidStack(Fluids.WATER,drained):FluidStack.EMPTY;
        }
    };
    public EverfullUrnEntity(BlockPos pos,BlockState state){super(AlchemyDevices.URN_ENTITY.get(),pos,state);}
    public int water(){return water;}
    /** The scan is bounded to the original 5×3×5 region; old TC6 zone counters grew without bound. */
    public static BlockPos offset(int zone){int index=Math.floorMod(zone,75);return new BlockPos(index/5%5-2,index/25%3-1,index%5-2);}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){DeviceVisuals.urnTick.accept(this);return;}
        if(!(level instanceof ServerLevel server)||++counter%5!=0)return;
        zone=(zone+1)%75;
        BlockPos candidate=worldPosition.offset(offset(zone));
        if(!candidate.equals(worldPosition)&&target(candidate))targets.add(zone);
        var iterator=targets.iterator();
        while(iterator.hasNext()&&water>=25){
            int index=iterator.next();BlockPos pos=worldPosition.offset(offset(index));
            if(!level.hasChunkAt(pos))continue;
            BlockState state=level.getBlockState(pos);
            // NeoForge exposes a cauldron fluid capability, which rejects 25 mB batches.
            // Preserve TC6's separate 333 mB-per-level path before consulting that capability.
            if(state.is(Blocks.CAULDRON)||state.is(Blocks.WATER_CAULDRON)){
                int amount=state.is(Blocks.WATER_CAULDRON)?state.getValue(LayeredCauldronBlock.LEVEL):0;
                if(water>=333&&amount<3){level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,amount+1));level.updateNeighbourForOutputSignal(pos,Blocks.WATER_CAULDRON);spend(333,index);}
                continue;
            }
            var handler=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP);
            if(handler!=null){
                int accepted=handler.fill(new FluidStack(Fluids.WATER,25),IFluidHandler.FluidAction.EXECUTE);
                if(accepted>0){spend(Math.min(25,accepted),index);break;}
            }else iterator.remove();
        }
        if(water<CAPACITY){
            float requested=Math.min(.1f,(CAPACITY-water)/1000f);
            int generated=(int)(1000*AuraSystem.drain(server,worldPosition,requested,false,false));
            if(generated>0){water=Math.min(CAPACITY,water+generated);sync();}
        }
    }
    private boolean target(BlockPos pos){return level.hasChunkAt(pos)&&(level.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP)!=null||level.getBlockState(pos).is(Blocks.CAULDRON)||level.getBlockState(pos).is(Blocks.WATER_CAULDRON));}
    private void spend(int amount,int index){water-=amount;sync();level.blockEvent(worldPosition,getBlockState().getBlock(),1,index);}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override public boolean triggerEvent(int id,int parameter){if(id==1){trailTicks=5;trailZone=Math.floorMod(parameter,75);return true;}return super.triggerEvent(id,parameter);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registry){super.saveAdditional(tag,registry);tag.putInt("Water",water);tag.putInt("Zone",zone);tag.putIntArray("Targets",targets.stream().mapToInt(Integer::intValue).toArray());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registry){super.loadAdditional(tag,registry);water=Math.clamp(tag.getInt("Water"),0,CAPACITY);zone=Math.floorMod(tag.getInt("Zone"),75);targets.clear();for(int index:tag.getIntArray("Targets"))if(index>=0&&index<75&&!offset(index).equals(BlockPos.ZERO))targets.add(index);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registry){return saveWithoutMetadata(registry);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
