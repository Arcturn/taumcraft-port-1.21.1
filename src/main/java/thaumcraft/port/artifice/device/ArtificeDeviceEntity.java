package thaumcraft.port.artifice.device;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.*;
import thaumcraft.port.world.AuraSystem;

import static thaumcraft.port.artifice.device.ArtificeDeviceBlock.*;

public final class ArtificeDeviceEntity extends BlockEntity implements IEnergyStorage {
    private static final Map<ServerLevel,Set<BlockPos>> EARS=new WeakHashMap<>();
    private static final int[] RANGES={4,8,16,32};
    private static final int[][] PATTERNS={{0,1,2,3,4,5,6,7,8},{0},{0,1},{0,3},{0,1,3,4},{0,1,2},{0,3,6},{0,1,2,3,4,5},{0,1,3,4,6,7},{0,1,2,3,5,6,7,8}};
    public int range=1,rangeActual,vis,input=1,output=15,note,tone,pulse,pattern,energy;
    public float power,rotation,rotationSpeed;
    public int rotationTicks,ticks;
    public final byte[] grid=new byte[169];
    private long lastHeard=Long.MIN_VALUE;
    public ArtificeDeviceEntity(BlockPos pos,BlockState state){super(ArtificeDevices.ENTITY.get(),pos,state);}
    public Kind kind(){return ((ArtificeDeviceBlock)getBlockState().getBlock()).kind;}
    public Direction facing(){return getBlockState().getValue(FACING);}
    public boolean enabled(){return getBlockState().getValue(ENABLED);}
    @Override public void setBlockState(BlockState state){boolean changed=getBlockState().getValue(FACING)!=state.getValue(FACING);super.setBlockState(state);if(changed)invalidateCapabilities();}
    public int rangeLimit(){return RANGES[range];}
    public int liftCost(){return rangeLimit()*2;}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server&&((ArtificeDeviceBlock)getBlockState().getBlock()).isEar()){EARS.computeIfAbsent(server,k->new HashSet<>()).add(worldPosition.immutable());updateTone();}}
    @Override public void setRemoved(){if(level instanceof ServerLevel server){var ears=EARS.get(server);if(ears!=null)ears.remove(worldPosition);}super.setRemoved();}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide&&kind()!=Kind.LEVITATOR)ArtificeDevices.clientTick.accept(this);
        switch(kind()){
            case LEVITATOR->levitate();
            case ARCANE_EAR,ARCANE_EAR_TOGGLE->{if(!level.isClientSide&&pulse>0&&--pulse==0){setEnabled(false);setChanged();}}
            case DIOPTRA->{if(!level.isClientSide&&++ticks%20==0)sampleAura();}
            case PATTERN_CRAFTER->{if(level.isClientSide){if(rotationTicks>0){rotationTicks--;rotationSpeed++;}else rotationSpeed*=.8f;rotation+=rotationSpeed;}
                else if(ticks++%20==0&&enabled())craftOnce();}
            case VIS_GENERATOR->{if(level instanceof ServerLevel&&enabled())generate();}
            default->{}
        }
    }
    private void levitate(){
        Direction face=facing();if(rangeActual>rangeLimit())rangeActual=0;
        int step=Math.floorMod(ticks,rangeLimit())+1;
        BlockPos check=worldPosition.relative(face,step);
        if(level.getBlockState(check).isSolidRender(level,check)){if(step<rangeActual)rangeActual=step;ticks=-1;}
        else if(step>rangeActual)rangeActual=step;
        ++ticks;
        if(level instanceof ServerLevel server&&vis<10){vis=(int)(vis+AuraSystem.drain(server,worldPosition,1,false,false)*1200);sync();}
        if(rangeActual<=0||vis<=0||!enabled())return;
        if(level.isClientSide)ArtificeDevices.clientTick.accept(this);
        AABB box=new AABB(worldPosition).expandTowards(face.getStepX()*rangeActual,face.getStepY()*rangeActual,face.getStepZ()*rangeActual);
        for(Entity e:level.getEntities((Entity)null,box,e->e instanceof ItemEntity||e.isPushable()||e instanceof AbstractHorse)){
            e.setDeltaMovement(liftVelocity(e.getDeltaMovement(),face,e.isShiftKeyDown(),e.onGround()));e.fallDistance=0;
            vis-=liftCost();if(vis<=0)break;
        }
        if(!level.isClientSide)setChanged();
    }
    public static Vec3 liftVelocity(Vec3 v,Direction face,boolean crouching,boolean grounded){
        if(crouching&&face==Direction.UP)return new Vec3(v.x,v.y<0?v.y*.9f:v.y,v.z);
        double x=v.x+.1f*face.getStepX(),y=v.y+.1f*face.getStepY(),z=v.z+.1f*face.getStepZ();
        if(face.getAxis()!=Direction.Axis.Y&&!grounded){if(y<0)y*=.9f;y+=.08f;}
        return new Vec3(Mth.clamp(x,-.35f,.35f),Mth.clamp(y,-.35f,.35f),Mth.clamp(z,-.35f,.35f));
    }
    public void generate(){
        if(!(level instanceof ServerLevel server))return;
        if(energy==0){energy=(int)(AuraSystem.drain(server,worldPosition,1,false,false)*1000);sync();}
        var target=level.getCapability(Capabilities.EnergyStorage.BLOCK,worldPosition.relative(facing()),facing().getOpposite());
        sendEnergy(target);
    }
    public void sendEnergy(IEnergyStorage target){
        if(target!=null&&target.canReceive()){
            int sent=Mth.clamp(target.receiveEnergy(Math.min(energy,20),false),0,Math.min(energy,20));
            if(sent>0){energy-=sent;setChanged();if(energy==0)sync();}
        }
    }
    public void sampleAura(){
        if(!(level instanceof ServerLevel server))return;
        Arrays.fill(grid,(byte)0);int cx=worldPosition.getX()>>4,cz=worldPosition.getZ()>>4;
        for(int z=0;z<13;z++)for(int x=0;x<13;x++){
            BlockPos p=new BlockPos((cx+x-6)<<4,worldPosition.getY(),(cz+z-6)<<4);
            if(!server.hasChunkAt(p))continue;var aura=AuraSystem.get(server,p);if(aura==null)continue;
            grid[x+z*13]=(byte)Math.min(64,(enabled()?aura.vis():aura.flux())/500*64);
        }
        sync();level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
    }
    public int comparator(){float amount=(grid[84]&255)/64f;return (int)Math.floor(amount*14)+(amount>0?1:0);}
    public static void hearNote(ServerLevel server,BlockPos source,NoteBlockInstrument instrument,int note){
        Set<BlockPos> ears=EARS.get(server);if(ears==null)return;
        for(BlockPos p:List.copyOf(ears)){
            if(!server.hasChunkAt(p))continue;
            if(!(server.getBlockEntity(p) instanceof ArtificeDeviceEntity ear)){ears.remove(p);continue;}
            if(p.distSqr(source)>4096||ear.note!=note||ear.tone!=instrument.ordinal()||ear.lastHeard==server.getGameTime())continue;
            ear.lastHeard=server.getGameTime();ear.playNote();
            if(ear.kind()==Kind.ARCANE_EAR_TOGGLE)ear.setEnabled(!ear.enabled());else{ear.pulse=10;ear.setEnabled(true);}ear.setChanged();
        }
    }
    private void setEnabled(boolean on){level.setBlock(worldPosition,getBlockState().setValue(ENABLED,on),3);ArtificeDeviceBlock.notifyOutput(level,worldPosition,getBlockState());}
    public void updateTone(){
        if(level==null)return;BlockState base=level.getBlockState(worldPosition.relative(facing().getOpposite()));
        // The first ten modern instruments are the same ten instruments supported by BETA26.
        // Later instruments keep their modern sound, rather than silently matching a different tone.
        tone=base.instrument().ordinal();sync();
    }
    public void playNote(){if(level!=null)level.blockEvent(worldPosition,getBlockState().getBlock(),100+tone,note);}
    @Override public boolean triggerEvent(int id,int value){
        if(id==1&&kind()==Kind.PATTERN_CRAFTER){if(level!=null&&level.isClientSide)rotationTicks=10;return true;}
        if(id>=100&&id<100+NoteBlockInstrument.values().length){
            if(level!=null&&level.isClientSide){var instrument=NoteBlockInstrument.values()[id-100];
                level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,instrument.getSoundEvent().value(),SoundSource.BLOCKS,3,NoteBlock.getPitchFromNote(value),false);
                level.addParticle(ParticleTypes.NOTE,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,value/24.0,0,0);}
            return true;
        }return super.triggerEvent(id,value);
    }
    public static CraftingInput patternInput(int pattern,ItemStack stack){
        List<ItemStack> grid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));for(int index:PATTERNS[Mth.clamp(pattern,0,9)])grid.set(index,stack.copyWithCount(1));
        return CraftingInput.of(3,3,grid);
    }
    /** One original one-second cycle. Items are held by adjacent inventories, never by this device. */
    public boolean craftOnce(){
        if(!(level instanceof ServerLevel server))return false;
        if(power<=0){power+=AuraSystem.drain(server,worldPosition,5,false,false);setChanged();}
        if(power<1)return false;
        IItemHandler above=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.above(),Direction.DOWN);
        IItemHandler below=level.getCapability(Capabilities.ItemHandler.BLOCK,worldPosition.below(),Direction.UP);
        if(above==null||below==null||above==below)return false;
        int amount=PATTERNS[pattern].length;
        for(int slot=0;slot<above.getSlots();slot++){
            ItemStack representative=above.getStackInSlot(slot);if(representative.isEmpty())continue;
            List<Transfer> take=reserveInputs(above,representative,amount);if(take==null)continue;
            CraftingInput input=patternInput(pattern,representative);
            var recipe=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,server);if(recipe.isEmpty())continue;
            var value=recipe.get().value();ItemStack result=value.assemble(input,server.registryAccess());if(result.isEmpty())continue;
            List<ItemStack> results=new ArrayList<>();results.add(result);for(ItemStack remaining:value.getRemainingItems(input))if(!remaining.isEmpty())results.add(remaining);
            List<Transfer> insert=reserveOutputs(below,results);if(insert==null)continue;
            List<ItemStack> removed=new ArrayList<>();boolean paid=true;
            for(Transfer transfer:take){ItemStack got=above.extractItem(transfer.slot,transfer.stack.getCount(),false);removed.add(got);
                if(got.getCount()!=transfer.stack.getCount()||!ItemStack.isSameItemSameComponents(got,transfer.stack)){paid=false;break;}}
            if(!paid){for(ItemStack stack:removed)spill(ItemHandlerHelper.insertItemStacked(above,stack,false));return false;}
            // A handler that changes its contract between simulation and insertion cannot delete items.
            for(Transfer transfer:insert)spill(below.insertItem(transfer.slot,transfer.stack.copy(),false));
            power-=1;sync();level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);return true;
        }return false;
    }
    private void spill(ItemStack stack){if(!stack.isEmpty())level.addFreshEntity(new ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY()-.1,worldPosition.getZ()+.5,stack));}
    public record Transfer(int slot,ItemStack stack) {}
    public static List<Transfer> reserveInputs(IItemHandler handler,ItemStack sample,int needed){
        List<Transfer> plan=new ArrayList<>();for(int i=0;i<handler.getSlots()&&needed>0;i++){
            ItemStack stack=handler.getStackInSlot(i);if(!sameMaterial(sample,stack))continue;
            ItemStack extraction=handler.extractItem(i,needed,true);if(!sameMaterial(sample,extraction))continue;
            int n=Math.min(needed,extraction.getCount());if(n>0){plan.add(new Transfer(i,extraction.copyWithCount(n)));needed-=n;}
        }return needed==0?plan:null;
    }
    /** TC6 BASEORE accepts ore-equivalent ingredients, while preserving components for ordinary stacks. */
    public static boolean sameMaterial(ItemStack a,ItemStack b){
        if(a.isEmpty()||b.isEmpty())return false;if(ItemStack.isSameItemSameComponents(a,b))return true;
        // The old OreDictionary alternative is a plain entry: containsMatch compares
        // its NBT with the candidate, so named/enchanted/custom stacks cannot pay it.
        if(!b.getComponentsPatch().isEmpty())return false;
        return a.getTags().filter(tag->{String ns=tag.location().getNamespace(),p=tag.location().getPath();
            return (ns.equals("c")&&(p.startsWith("ingots/")||p.startsWith("nuggets/")||p.startsWith("gems/")||p.startsWith("dusts/")||p.startsWith("storage_blocks/")||p.equals("rods/wooden")))
                ||ns.equals("minecraft")&&(p.equals("planks")||p.equals("logs")||p.equals("wool")||p.equals("sand"));}).anyMatch(b::is);
    }
    /** Reserve all products together; separately simulated container returns must not overbook a slot. */
    public static List<Transfer> reserveOutputs(IItemHandler handler,List<ItemStack> products){
        ItemStack[] shadow=new ItemStack[handler.getSlots()];int[] reserved=new int[shadow.length];for(int s=0;s<shadow.length;s++)shadow[s]=handler.getStackInSlot(s).copy();
        List<Transfer> plan=new ArrayList<>();
        for(ItemStack product:products){int left=product.getCount();
            for(int pass=0;pass<2&&left>0;pass++)for(int s=0;s<shadow.length&&left>0;s++){
                ItemStack virtual=shadow[s];if((pass==0)==virtual.isEmpty()||!virtual.isEmpty()&&!ItemStack.isSameItemSameComponents(virtual,product))continue;
                int probe=Math.min(product.getMaxStackSize(),handler.getSlotLimit(s));
                ItemStack request=product.copyWithCount(probe),remainder=handler.insertItem(s,request,true);
                int available=Math.max(0,probe-remainder.getCount()-reserved[s]);int n=Math.min(left,available);
                if(n>0){plan.add(new Transfer(s,product.copyWithCount(n)));reserved[s]+=n;left-=n;if(virtual.isEmpty())shadow[s]=product.copyWithCount(n);else virtual.grow(n);}
            }if(left>0)return null;
        }return plan;
    }
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){super.saveAdditional(n,registries);write(n);}
    private void write(CompoundTag n){n.putInt("range",range);n.putInt("vis",vis);n.putInt("in",input);n.putInt("out",output);n.putInt("note",note);n.putInt("tone",tone);n.putInt("pulse",pulse);n.putInt("type",pattern);n.putFloat("power",power);n.putInt("energy",energy);n.putByteArray("grid_a",grid);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){super.loadAdditional(n,registries);
        range=Mth.clamp(n.getInt("range"),0,3);vis=Mth.clamp(n.getInt("vis"),-64,1209);input=Mth.clamp(n.getInt("in"),1,15);output=Mth.clamp(n.getInt("out"),1,15);
        note=Mth.clamp(n.getInt("note"),0,24);tone=Mth.clamp(n.getInt("tone"),0,NoteBlockInstrument.values().length-1);pulse=Mth.clamp(n.getInt("pulse"),0,10);
        pattern=Mth.clamp(n.getInt("type"),0,9);float stored=n.getFloat("power");power=Float.isFinite(stored)?Mth.clamp(stored,0,5):0;energy=Mth.clamp(n.getInt("energy"),0,1000);
        byte[] data=n.getByteArray("grid_a");Arrays.fill(grid,(byte)0);for(int i=0;i<Math.min(grid.length,data.length);i++)grid[i]=(byte)Mth.clamp(data[i],0,64);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){CompoundTag n=new CompoundTag();write(n);return n;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public int receiveEnergy(int amount,boolean simulate){return 0;}
    public int extractEnergy(int amount,boolean simulate){return 0;}
    public int getEnergyStored(){return energy;}
    public int getMaxEnergyStored(){return 1000;}
    public boolean canExtract(){return true;}
    public boolean canReceive(){return false;}
}
