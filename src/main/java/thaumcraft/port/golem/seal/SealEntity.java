package thaumcraft.port.golem.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import thaumcraft.port.golem.task.TaskHandler;

/** Saved face attachment; task state is transient and rebuilt after loading. */
public final class SealEntity implements ISealEntity {
    private SealPos sealPos;
    private ISeal seal;
    private byte priority,color;
    private boolean locked,redstone,stopped;
    private String owner="";
    private BlockPos area=new BlockPos(1,1,1);
    public SealEntity(){ }
    public SealEntity(Level level,SealPos pos,ISeal seal){
        this.sealPos=pos;this.seal=seal;
        if(seal instanceof ISealConfigArea)area=new BlockPos(pos.face.getStepX()==0?3:1,pos.face.getStepY()==0?3:1,pos.face.getStepZ()==0?3:1);
    }
    public void tickSealEntity(Level level){
        if(seal==null)return;
        if(isStoppedByRedstone(level)){
            if(!stopped)for(var task:TaskHandler.getTasks(level).values())if(sealPos.equals(task.getSealPos()))task.setSuspended(true);
            stopped=true;return;
        }
        stopped=false;seal.tickSeal(level,this);
    }
    public boolean isStoppedByRedstone(Level level){
        if(!redstone)return false;
        // SignalGetter also checks strong power through conducting neighbours. Its reads must
        // stay loaded-only, including that second layer and a delivery's unloaded source seal.
        var signals=new LoadedSignals(level);
        return signals.hasNeighborSignal(sealPos.pos)||signals.hasNeighborSignal(sealPos.pos.relative(sealPos.face));
    }
    private record LoadedSignals(Level level) implements SignalGetter {
        @Override public BlockState getBlockState(BlockPos pos){return level.hasChunkAt(pos)?level.getBlockState(pos):Blocks.AIR.defaultBlockState();}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return level.hasChunkAt(pos)?level.getBlockEntity(pos):null;}
        @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    }
    public ISeal getSeal(){return seal;}
    public SealPos getSealPos(){return sealPos;}
    public byte getPriority(){return priority;}
    public void setPriority(byte value){priority=(byte)Math.clamp(value,-5,5);}
    public byte getColor(){return color;}
    public void setColor(byte value){color=(byte)Math.clamp(value,0,16);}
    public String getOwner(){return owner;}
    public void setOwner(String value){owner=value==null?"":value;}
    public boolean isLocked(){return locked;}
    public void setLocked(boolean value){locked=value;}
    public boolean isRedstoneSensitive(){return redstone;}
    public void setRedstoneSensitive(boolean value){redstone=value;}
    public BlockPos getArea(){return area;}
    public void setArea(BlockPos value){area=new BlockPos(Math.clamp(value.getX(),1,8),Math.clamp(value.getY(),1,8),Math.clamp(value.getZ(),1,8));}
    public void readNBT(CompoundTag tag,HolderLookup.Provider registries){
        int face=tag.getByte("face");if(face<0||face>=6)throw new IllegalArgumentException("Invalid seal face");
        sealPos=new SealPos(BlockPos.of(tag.getLong("pos")),Direction.values()[face]);
        setPriority(tag.getByte("priority"));setColor(tag.getByte("color"));setLocked(tag.getBoolean("locked"));setRedstoneSensitive(tag.getBoolean("redstone"));setOwner(tag.getString("owner"));
        seal=SealHandler.createSeal(tag.getString("type"));
        if(seal==null)throw new IllegalArgumentException("Unknown seal type");
        seal.readCustomNBT(tag,registries);
        if(seal instanceof ISealConfigArea)setArea(BlockPos.of(tag.getLong("area")));
        if(seal instanceof ISealConfigToggles toggles)for(var toggle:toggles.getToggles())if(tag.contains(toggle.getKey()))toggle.setValue(tag.getBoolean(toggle.getKey()));
    }
    public CompoundTag writeNBT(HolderLookup.Provider registries){
        var tag=new CompoundTag();tag.putLong("pos",sealPos.pos.asLong());tag.putByte("face",(byte)sealPos.face.ordinal());
        tag.putString("type",seal==null?"":seal.getKey());tag.putByte("priority",priority);tag.putByte("color",color);tag.putBoolean("locked",locked);tag.putBoolean("redstone",redstone);tag.putString("owner",owner);
        if(seal!=null){seal.writeCustomNBT(tag,registries);if(seal instanceof ISealConfigArea)tag.putLong("area",area.asLong());if(seal instanceof ISealConfigToggles toggles)for(var toggle:toggles.getToggles())tag.putBoolean(toggle.getKey(),toggle.getValue());}
        return tag;
    }
    public void syncToClient(Level level){SealHandler.changed(level,this);}
}
