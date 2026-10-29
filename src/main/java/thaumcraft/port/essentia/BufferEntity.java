package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;

public final class BufferEntity extends TubeEntity {
    public final MixedEssentia contents=new MixedEssentia(10);
    public final byte[] chokes=new byte[6];
    public int bellows;
    private boolean initialized;
    public BufferEntity(BlockPos p,BlockState s){super(p,s);}
    @Override public Aspect aspect(Direction d){var keys=new ArrayList<>(contents.values().keySet());return keys.isEmpty()?null:keys.get(level==null?0:level.random.nextInt(keys.size()));}
    @Override public int amount(Direction d){return contents.total();}
    @Override public Aspect suctionType(Direction d){return null;}
    @Override public int suction(Direction d){return d==null?0:MixedEssentia.bufferSuction(bellows,chokes[d.ordinal()]);}
    @Override public int insert(Aspect a,int n,Direction d,boolean simulate){int accepted=input(d)&&n==1?contents.insert(a,1,simulate):0;if(!simulate&&accepted>0)sync();return accepted;}
    @Override public int extract(Aspect a,int n,Direction d,boolean simulate){
        if(!output(d)||a==null||n<=0)return 0;
        var requester=neighbour(d);int pressure=requester==null?0:requester.suction(d.getOpposite());
        for(Direction side:Direction.values())if(side!=d&&output(side)){
            var other=neighbour(side);Direction face=side.getOpposite();
            if(other!=null&&other.connects(face)&&other.input(face)&&MixedEssentia.higherDemand(a,pressure,suction(side),other.suctionType(face),other.suction(face)))return 0;
        }
        int taken=contents.extract(a,n,simulate);if(taken>0&&!simulate)sync();return taken;
    }
    @Override public void tick(){
        if(level==null||level.isClientSide)return;
        if(!initialized||level.getGameTime()%20==0){int count=BellowsEntity.count(level,worldPosition,null);if(count!=bellows){bellows=count;sync();}}
        if(!initialized||level.getGameTime()%20==0){initialized=true;((TubeBlock)getBlockState().getBlock()).refresh(level,worldPosition);}
        if(level.getGameTime()%5==0&&contents.total()<10)for(Direction d:Direction.values())if(EssentiaTransport.pull(this,d,neighbour(d))>0)break;
    }
    public void choke(Direction d){chokes[d.ordinal()]=(byte)((chokes[d.ordinal()]+1)%3);sync();}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);var a=new CompoundTag();contents.values().forEach((k,v)->a.putInt(k.id(),v));t.put("Contents",a);t.putByteArray("Chokes",chokes);t.putInt("Bellows",bellows);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);var a=t.getCompound("Contents");var values=new EnumMap<Aspect,Integer>(Aspect.class);for(Aspect aspect:Aspect.values())values.put(aspect,a.getInt(aspect.id()));contents.restore(values);var saved=t.getByteArray("Chokes");for(int i=0;i<6;i++)chokes[i]=(byte)(i<saved.length?Math.clamp(saved[i],0,2):0);bellows=Math.clamp(t.getInt("Bellows"),0,6);}
}
