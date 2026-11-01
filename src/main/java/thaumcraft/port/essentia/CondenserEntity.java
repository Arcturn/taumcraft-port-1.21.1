package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.world.AuraSystem;

/** BETA26 condenser: sacrificial essentia in the sides, condensed Vitium from below. */
public final class CondenserEntity extends EssentiaEntity implements ResonatorItem.CondenserReadout {
    public static final int CAPACITY=100;
    private int essentia,flux,counter;
    private float latticeCount=-1;
    private int interval,cost;
    private final List<BlockPos> lattice=new ArrayList<>(),cleanLattice=new ArrayList<>();
    public CondenserEntity(BlockPos pos,BlockState state){super(CondenserContent.ENTITY.get(),pos,state);}
    public int storedEssentia(){return essentia;}
    public int storedFlux(){return flux;}
    public float latticeEfficiency(){if(latticeCount<0)triggerCheck();return latticeCount;}
    public int latticeBlocks(){if(latticeCount<0)triggerCheck();return lattice.size();}
    public int essentiaCost(){if(latticeCount<0)triggerCheck();return cost;}
    public int cycleTicks(){if(latticeCount<0)triggerCheck();return interval;}
    public void invalidateLattice(){latticeCount=-1;}
    @Override public void tick(){
        if(level==null)return;
        if(latticeCount<0)triggerCheck();counter++;
        if(!getBlockState().getValue(CondenserBlock.ENABLED)||latticeCount<=0)return;
        if(level.isClientSide){
            if(essentia>0&&!cleanLattice.isEmpty()&&counter%Math.max(3,interval/50)==0)
                CondenserVisualBridge.spark.accept(this,cleanLattice.get(level.random.nextInt(cleanLattice.size())));
            return;
        }
        if(counter%5==0&&essentia<CAPACITY)fill();
        if(interval>0&&essentia>=cost&&flux<CAPACITY&&counter%interval==0&&level instanceof ServerLevel server){
            if(AuraSystem.drain(server,worldPosition,1,true,true)<1)return;
            if(AuraSystem.drain(server,worldPosition,1,true,false)<1)return;
            essentia-=cost;flux++;
            if(level.random.nextInt(50)==0)clogLattice();
            sync();
        }
    }
    private void fill(){
        for(Direction side:Direction.Plane.HORIZONTAL){
            var source=neighbour(side);var opposite=side.getOpposite();
            if(source==null||!source.connects(opposite))continue;
            if(!source.output(opposite))return;
            if(source.amount(opposite)<=0||source.suction(opposite)>=suction(side)||suction(side)<source.minimumSuction())continue;
            var aspect=source.aspect(opposite);if(aspect==null)continue;
            if(aspect==Aspect.FLUX)clogLattice();
            else EssentiaTransport.pull(this,side,source);
            if(essentia>=CAPACITY)break;
        }
    }
    /** Flux is not consumed: feeding it clogs a filter instead of fueling purification. */
    public void clogLattice(){
        if(level==null||level.isClientSide||cleanLattice.isEmpty())return;
        int choice=level.random.nextInt(cleanLattice.size());if(choice==0)choice=level.random.nextInt(cleanLattice.size());
        var pos=cleanLattice.get(choice);
        if(level.getBlockState(pos).is(CondenserContent.LATTICE.get())){
            level.setBlock(pos,CondenserLatticeBlock.connections(CondenserContent.DIRTY_LATTICE.get().defaultBlockState(),level,pos),3);
            invalidateLattice();
        }
    }
    public void triggerCheck(){
        lattice.clear();cleanLattice.clear();latticeCount=0;interval=0;cost=0;if(level==null)return;
        // Explicit DFS preserves TC6's face order and post-order weighting without overflowing
        // the Java call stack on dense connected lattice structures.
        Set<BlockPos> visited=new HashSet<>();var stack=new ArrayDeque<CheckFrame>();
        visited.add(worldPosition);stack.push(new CheckFrame(worldPosition,true,false));boolean invalid=false;
        while(!stack.isEmpty()&&!invalid){
            var frame=stack.peek();
            if(frame.next>=Direction.values().length){
                if(frame.found&&!frame.clogged)latticeCount+=1-.15f*frame.sides;
                stack.pop();continue;
            }
            var side=Direction.values()[frame.next++];if(frame.root&&side!=Direction.UP)continue;
            var pos=frame.pos.relative(side);if(!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);boolean clean=state.is(CondenserContent.LATTICE.get()),dirty=state.is(CondenserContent.DIRTY_LATTICE.get());
            if(frame.root&&dirty)frame.clogged=true;if(clean||dirty)frame.sides++;
            if(visited.contains(pos))continue;
            if(side==Direction.DOWN&&state.is(CondenserContent.CONDENSER.get())){invalid=true;break;}
            if(pos.getY()<=worldPosition.getY()||worldPosition.distSqr(pos)>74||!clean&&!dirty)continue;
            lattice.add(pos);if(clean)cleanLattice.add(pos);frame.found=true;visited.add(pos);
            if(frame.root)frame.next=Direction.values().length;
            stack.push(new CheckFrame(pos,false,frame.clogged||dirty));
        }
        if(invalid||latticeCount<=0){latticeCount=0;return;}
        latticeCount=Math.min(40,latticeCount);interval=Math.max(5,Math.round(600-latticeCount*15));cost=(int)(4+Math.sqrt(lattice.size()));
    }
    private static final class CheckFrame {
        final BlockPos pos;final boolean root;boolean clogged,found;int next,sides;
        CheckFrame(BlockPos pos,boolean root,boolean clogged){this.pos=pos;this.root=root;this.clogged=clogged;}
    }
    public boolean connects(Direction side){return side!=Direction.UP;}
    public boolean input(Direction side){return side!=null&&side.getAxis().isHorizontal();}
    public boolean output(Direction side){return side==Direction.DOWN;}
    public Aspect aspect(Direction side){return Aspect.FLUX;}
    public int amount(Direction side){return flux;}
    public Aspect suctionType(Direction side){return null;}
    public int suction(Direction side){return side==Direction.DOWN||essentia>=CAPACITY?0:128;}
    public int minimumSuction(){return 0;}
    public int insert(Aspect aspect,int amount,Direction side,boolean simulate){
        if(!input(side)||aspect==null||amount<=0)return 0;
        if(aspect==Aspect.FLUX){if(!simulate)clogLattice();return 0;}
        int accepted=Math.min(amount,CAPACITY-essentia);if(accepted>0&&!simulate){essentia+=accepted;sync();}return accepted;
    }
    public int extract(Aspect aspect,int amount,Direction side,boolean simulate){
        if(!output(side)||aspect!=null&&aspect!=Aspect.FLUX||amount<=0)return 0;
        int taken=Math.min(amount,flux);if(taken>0&&!simulate){flux-=taken;sync();}return taken;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("Essentia",essentia);tag.putInt("Flux",flux);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);essentia=Math.clamp(tag.getInt("Essentia"),0,CAPACITY);flux=Math.clamp(tag.getInt("Flux"),0,CAPACITY);invalidateLattice();}
}
