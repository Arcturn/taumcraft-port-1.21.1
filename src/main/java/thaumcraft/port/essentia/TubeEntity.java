package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;

public class TubeEntity extends EssentiaEntity {
    public final EssentiaTank tank=new EssentiaTank(1);
    public final TubeBlock.Kind kind;
    public int open=63,venting,suction;
    public Aspect suctionType;
    public boolean allowFlow=true,wasPowered;
    public float rotation,previousRotation;
    private boolean connectionsInitialized;
    public TubeEntity(BlockPos pos,BlockState state){super(EssentiaContent.TUBE_ENTITY.get(),pos,state);kind=((TubeBlock)state.getBlock()).kind;}
    public Direction facing(){return getBlockState().getValue(TubeBlock.FACING);}
    public boolean connects(Direction d){return d!=null&&(open&(1<<d.ordinal()))!=0&&(kind!=TubeBlock.Kind.VALVE||d!=facing());}
    public boolean input(Direction d){return connects(d)&&(kind!=TubeBlock.Kind.ONEWAY||d!=facing().getOpposite())&&(kind!=TubeBlock.Kind.VALVE||allowFlow);}
    public boolean output(Direction d){return connects(d)&&(kind!=TubeBlock.Kind.ONEWAY||d==facing().getOpposite())&&(kind!=TubeBlock.Kind.VALVE||allowFlow);}
    public Aspect aspect(Direction d){return tank.aspect();}
    public int amount(Direction d){return tank.amount();}
    public Aspect suctionType(Direction d){return suctionType;}
    public int suction(Direction d){return kind==TubeBlock.Kind.VALVE&&!allowFlow?0:suction;}
    public int minimumSuction(){return 0;}
    public int insert(Aspect a,int n,Direction d,boolean simulate){int value=input(d)?tank.insert(a,n,false,simulate):0;if(!simulate&&value>0)sync();return value;}
    public int extract(Aspect a,int n,Direction d,boolean simulate){int value=output(d)?tank.extract(a,n,simulate):0;if(!simulate&&value>0)sync();return value;}
    private void calculateSuction(){
        suction=0;suctionType=null;
        if(kind==TubeBlock.Kind.VALVE&&!allowFlow)return;
        var demands=new java.util.ArrayList<SuctionRules.Demand>();
        for(Direction d:Direction.values()){
            if(!connects(d)||(kind==TubeBlock.Kind.ONEWAY&&d!=facing().getOpposite()))continue;
            var other=neighbour(d);if(other==null||!other.connects(d.getOpposite()))continue;
            demands.add(new SuctionRules.Demand(other.suctionType(d.getOpposite()),other.suction(d.getOpposite()),false));
        }
        var demand=SuctionRules.calculate(tank.aspect(),tank.filter(),kind==TubeBlock.Kind.RESTRICT,demands);suction=demand.strength();suctionType=demand.aspect();
    }
    private void checkVenting(){
        if(suction<=0)return;
        for(Direction d:Direction.values()){
            if(!connects(d))continue;var other=neighbour(d);
            if(other==null||!other.connects(d.getOpposite())||(other instanceof TubeEntity t&&t.kind==TubeBlock.Kind.FILTER))continue;
            int strength=other.suction(d.getOpposite());
            if(SuctionRules.conflicts(new SuctionRules.Demand(suctionType,suction,false),new SuctionRules.Demand(other.suctionType(d.getOpposite()),strength,false))){
                venting=40;sync();level.playSound(null,worldPosition,PortSounds.get("spill"),SoundSource.BLOCKS,.3f,1.4f);return;
            }
        }
    }
    public void toggle(Direction side){
        open^=1<<side.ordinal();sync();
        if(level!=null){
            ((TubeBlock)getBlockState().getBlock()).refresh(level,worldPosition);
            BlockPos p=worldPosition.relative(side);
            if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof TubeEntity other){
                int bit=1<<side.getOpposite().ordinal();other.open=(open&(1<<side.ordinal()))==0?other.open&~bit:other.open|bit;
                other.sync();((TubeBlock)other.getBlockState().getBlock()).refresh(level,p);
            }
        }
    }
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){previousRotation=rotation;rotation=allowFlow?Math.max(0,rotation-20):Math.min(360,rotation+20);return;}
        if(!connectionsInitialized||level.getGameTime()%20==0){connectionsInitialized=true;((TubeBlock)getBlockState().getBlock()).refresh(level,worldPosition);}
        long time=level.getGameTime()+Math.floorMod(worldPosition.asLong(),5);
        if(kind==TubeBlock.Kind.VALVE&&time%5==0){
            boolean powered=level.hasNeighborSignal(worldPosition);
            if(powered!=wasPowered){allowFlow=!powered;wasPowered=powered;suction=0;sync();level.playSound(null,worldPosition,PortSounds.get("squeek"),SoundSource.BLOCKS,.7f,1);}
        }
        if(venting>0){
            --venting;setChanged();if(venting%5==0&&level instanceof ServerLevel s)s.sendParticles(PortParticles.ALL.get("taint").get(),worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,3,.12,.12,.12,.025);
            if(venting==0)sync();return;
        }
        if(time%2==0){int old=suction;Aspect type=suctionType;calculateSuction();checkVenting();if(old!=suction||type!=suctionType)sync();}
        if(venting==0&&time%5==0&&suction>0&&tank.amount()==0)for(Direction d:Direction.values())if(EssentiaTransport.pull(this,d,neighbour(d))>0){
            if(level.random.nextInt(100)==0)level.playSound(null,worldPosition,PortSounds.get("creak"),SoundSource.BLOCKS,1,1.4f);break;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);writeTank(tag,tank);tag.putInt("Open",open);tag.putInt("Suction",suction);if(suctionType!=null)tag.putString("SuctionType",suctionType.id());tag.putInt("Venting",venting);tag.putBoolean("Flow",allowFlow);tag.putBoolean("Powered",wasPowered);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);readTank(tag,tank);open=tag.contains("Open")?tag.getInt("Open")&63:63;suction=Math.clamp(tag.getInt("Suction"),0,192);suctionType=readAspect(tag,"SuctionType");venting=Math.clamp(tag.getInt("Venting"),0,40);allowFlow=!tag.contains("Flow")||tag.getBoolean("Flow");wasPowered=tag.getBoolean("Powered");}
}
