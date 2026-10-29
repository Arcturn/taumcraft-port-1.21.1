package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;

public abstract class EssentiaEntity extends BlockEntity implements EssentiaTransport {
    protected EssentiaEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    public EssentiaTransport neighbour(Direction face){return at(level,worldPosition.relative(face),face.getOpposite());}
    public static EssentiaTransport at(Level world,BlockPos pos,Direction face){
        if(world==null||!world.hasChunkAt(pos))return null;
        return EssentiaContent.CAPABILITY.getCapability(world,pos,null,null,face);
    }
    public abstract void tick();
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    static Aspect readAspect(CompoundTag tag,String key){return Aspect.byId(tag.getString(key)).orElse(null);}
    static void writeTank(CompoundTag tag,EssentiaTank tank){
        if(tank.aspect()!=null)tag.putString("Aspect",tank.aspect().id());
        if(tank.filter()!=null)tag.putString("Filter",tank.filter().id());
        tag.putInt("Amount",tank.amount());
    }
    static void readTank(CompoundTag tag,EssentiaTank tank){tank.restore(readAspect(tag,"Aspect"),tag.getInt("Amount"),readAspect(tag,"Filter"));}
}
