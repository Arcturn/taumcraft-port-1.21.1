package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.world.AuraSystem;

public final class JarEntity extends EssentiaEntity implements EssentiaSource {
    public boolean braced;
    public Direction labelFacing=Direction.NORTH;
    public final EssentiaTank tank;
    public final boolean alembic,voidJar;
    public JarEntity(BlockPos pos,BlockState state){
        super(EssentiaContent.JAR_ENTITY.get(),pos,state);
        alembic=state.is(EssentiaContent.ALEMBIC.get());voidJar=state.is(EssentiaContent.VOID_JAR.get());
        tank=new EssentiaTank(alembic?128:250);
    }
    public boolean blocked(){return alembic||braced;}
    public Aspect storedAspect(){return tank.aspect();}
    public int storedAmount(){return tank.amount();}
    public int takeRemote(Aspect a,int n,boolean simulate){if(blocked())return 0;int taken=tank.extract(a,n,simulate);if(taken>0&&!simulate)sync();return taken;}
    public int addRemote(Aspect a,int n,boolean simulate){return blocked()?0:fill(a,n,simulate);}
    public boolean attachBrace(){if(alembic||braced)return false;braced=true;sync();return true;}
    public void savePortable(net.minecraft.world.item.ItemStack stack,HolderLookup.Provider registries){
        saveToItem(stack,registries);var tag=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag();tag.remove("Braced");tag.remove("LabelFacing");stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
    }
    public boolean connects(Direction d){return d!=null&&(alembic?d!=Direction.DOWN&&(tank.filter()==null||d!=labelFacing):d==Direction.UP);}
    public boolean input(Direction d){return !alembic&&connects(d);}
    public boolean output(Direction d){return connects(d);}
    public Aspect aspect(Direction d){return tank.aspect();}
    public int amount(Direction d){return tank.amount();}
    public Aspect suctionType(Direction d){return alembic?null:(tank.filter()!=null?tank.filter():tank.aspect());}
    public int suction(Direction d){return alembic?0:tank.suction(voidJar);}
    public int minimumSuction(){return alembic?0:tank.minimum(voidJar);}
    public int insert(Aspect a,int n,Direction d,boolean simulate){return input(d)?fill(a,n,simulate):0;}
    public int fill(Aspect a,int n,boolean simulate){
        int before=tank.amount(),accepted=tank.insert(a,n,voidJar,simulate);
        if(!simulate&&accepted>0){
            if(voidJar&&(long)before+accepted>250&&level instanceof ServerLevel server&&server.random.nextInt(250)==0)AuraSystem.add(server,worldPosition,1,true);
            sync();
        }
        return accepted;
    }
    public int extract(Aspect a,int n,Direction d,boolean simulate){
        if(!output(d))return 0;int taken=tank.extract(a,n,simulate);if(!simulate&&taken>0)sync();return taken;
    }
    public void tick(){if(!alembic&&level!=null&&!level.isClientSide&&level.getGameTime()%5==0)EssentiaTransport.pull(this,Direction.UP,neighbour(Direction.UP));}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);writeTank(tag,tank);tag.putBoolean("Braced",braced&&!alembic);tag.putInt("LabelFacing",labelFacing.ordinal());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);readTank(tag,tank);braced=!alembic&&tag.getBoolean("Braced");int facing=tag.getInt("LabelFacing");labelFacing=facing>=2&&facing<6?Direction.values()[facing]:Direction.NORTH;}
}
