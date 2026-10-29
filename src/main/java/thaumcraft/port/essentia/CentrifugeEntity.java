package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortSounds;

public final class CentrifugeEntity extends EssentiaEntity {
    public Aspect incoming,outgoing;
    public int process;
    public float rotation,previousRotation,speed;
    public CentrifugeEntity(BlockPos p,BlockState s){super(EssentiaContent.CENTRIFUGE_ENTITY.get(),p,s);}
    public boolean connects(Direction d){return d==Direction.DOWN||d==Direction.UP;}
    public boolean input(Direction d){return d==Direction.DOWN;}
    public boolean output(Direction d){return d==Direction.UP;}
    public Aspect aspect(Direction d){return outgoing;}
    public int amount(Direction d){return outgoing==null?0:1;}
    public Aspect suctionType(Direction d){return null;}
    public boolean powered(){return level!=null&&level.hasNeighborSignal(worldPosition);}
    public int suction(Direction d){return d==Direction.DOWN&&!powered()?(incoming==null?128:64):0;}
    public int minimumSuction(){return 0;}
    public int insert(Aspect a,int n,Direction d,boolean simulate){
        if(!input(d)||a==null||a.isPrimal()||n<=0||incoming!=null||powered())return 0;
        if(!simulate){incoming=a;process=39;sync();}return 1;
    }
    public int extract(Aspect a,int n,Direction d,boolean simulate){
        if(!output(d)||outgoing==null||a!=outgoing||n<=0)return 0;
        if(!simulate){outgoing=null;sync();}return 1;
    }
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){previousRotation=rotation;speed=incoming!=null&&!powered()?Math.min(20,speed+2):Math.max(0,speed-.5f);rotation+=speed;return;}
        if(powered())return;
        if(outgoing==null&&incoming==null&&level.getGameTime()%5==0)EssentiaTransport.pull(this,Direction.DOWN,neighbour(Direction.DOWN));
        if(process>0){process--;setChanged();}
        if(incoming!=null&&outgoing==null&&process==0){outgoing=incoming.components().get(level.random.nextInt(2));incoming=null;sync();}
        if(incoming!=null&&level.getGameTime()%9==0)level.playSound(null,worldPosition,PortSounds.get("pump"),SoundSource.BLOCKS,.7f,1);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);if(incoming!=null)tag.putString("In",incoming.id());if(outgoing!=null)tag.putString("Out",outgoing.id());tag.putInt("Process",process);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);incoming=readAspect(tag,"In");if(incoming!=null&&incoming.isPrimal())incoming=null;outgoing=readAspect(tag,"Out");process=incoming==null?0:Math.clamp(tag.getInt("Process"),0,39);}
}
