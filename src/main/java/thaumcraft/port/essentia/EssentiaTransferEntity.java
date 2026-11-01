package thaumcraft.port.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;

public final class EssentiaTransferEntity extends EssentiaEntity {
    private final RemoteEssentia network=new RemoteEssentia();
    private int ticks;
    public EssentiaTransferEntity(BlockPos pos,BlockState state){super(EssentiaTransferContent.ENTITY.get(),pos,state);}
    public boolean isInput(){return ((EssentiaTransferBlock)getBlockState().getBlock()).input;}
    public Direction facing(){return getBlockState().getValue(EssentiaTransferBlock.FACING);}
    @Override public boolean connects(Direction face){return face==facing().getOpposite();}
    @Override public boolean input(Direction face){return isInput()&&connects(face);}
    @Override public boolean output(Direction face){return !isInput()&&connects(face);}
    @Override public Aspect aspect(Direction face){return null;}
    @Override public int amount(Direction face){return 0;}
    @Override public Aspect suctionType(Direction face){return null;}
    @Override public int suction(Direction face){return isInput()?128:0;}
    @Override public int minimumSuction(){return 0;}
    // These blocks do not store essentia. Only a successful remote transaction consumes it.
    @Override public int insert(Aspect aspect,int amount,Direction face,boolean simulate){return 0;}
    @Override public int extract(Aspect aspect,int amount,Direction face,boolean simulate){return 0;}
    @Override public void tick(){
        if(!(level instanceof ServerLevel server)||++ticks%5!=0)return;
        Direction rear=facing().getOpposite(),port=rear.getOpposite();
        EssentiaTransport adjacent=neighbour(rear);
        if(adjacent==null||!adjacent.connects(port))return;
        if(isInput()){
            if(!adjacent.output(port)||adjacent.amount(port)<1||adjacent.suction(port)>=128||adjacent.minimumSuction()>128)return;
            Aspect aspect=adjacent.aspect(port);
            if(aspect==null||adjacent.extract(aspect,1,port,true)!=1)return;
            var target=network.accepting(server,worldPosition,facing(),aspect);
            if(target==null)return;
            if(adjacent.extract(aspect,1,port,false)!=1)return;
            if(target.source().addRemote(aspect,1,false)!=1)throw new IllegalStateException("Remote essentia source changed after insertion simulation");
            EssentiaStreamPayload.send(server,worldPosition,target.pos(),aspect);
        }else{
            Aspect aspect=adjacent.suctionType(port);
            if(!adjacent.input(port)||adjacent.suction(port)<=0||aspect==null||adjacent.insert(aspect,1,port,true)!=1)return;
            var target=network.providing(server,worldPosition,facing(),aspect);
            if(target==null)return;
            if(target.source().takeRemote(aspect,1,false)!=1)return;
            if(adjacent.insert(aspect,1,port,false)!=1)throw new IllegalStateException("Essentia receiver changed after insertion simulation");
            EssentiaStreamPayload.send(server,target.pos(),worldPosition,aspect);
        }
    }
}
