package thaumcraft.port.world;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
/** Finite eight-quanta goo: transfer preserves volume and never creates water-style infinite sources. */
public final class FluxGooBlock extends Block {
    public static final IntegerProperty LEVEL=IntegerProperty.create("level",0,7);
    public FluxGooBlock(Properties p){super(p.noCollission().noOcclusion().randomTicks());registerDefaultState(stateDefinition.any().setValue(LEVEL,7));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(LEVEL);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(0,0,0,16,(s.getValue(LEVEL)+1)*2,16);}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){l.scheduleTick(p,this,10);}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(!WorldConfig.TAINT.get())return;
        int units=s.getValue(LEVEL)+1;
        var down=p.below();if(l.hasChunkAt(down)&&receivable(l,down))units=transfer(l,p,down,units,8);
        if(units>1)for(var d:Direction.Plane.HORIZONTAL){var q=p.relative(d);if(!l.hasChunkAt(q)||!receivable(l,q))continue;int other=amount(l,q);if(other+1<units)units=transfer(l,p,q,units,(units-other)/2);}
        if(units>0){l.setBlock(p,defaultBlockState().setValue(LEVEL,units-1),2);l.scheduleTick(p,this,10);}else l.removeBlock(p,false);
    }
    private int amount(ServerLevel l,BlockPos p){var s=l.getBlockState(p);return s.is(this)?s.getValue(LEVEL)+1:0;}
    private boolean receivable(ServerLevel l,BlockPos p){var s=l.getBlockState(p);return !l.isOutsideBuildHeight(p)&&l.getFluidState(p).isEmpty()&&(s.is(this)||s.isAir()||s.is(WorldContent.block("taint_fibre")));}
    private int transfer(ServerLevel l,BlockPos from,BlockPos to,int units,int limit){int there=amount(l,to),m=Math.min(Math.min(units,limit),8-there);if(m>0){l.setBlock(to,defaultBlockState().setValue(LEVEL,there+m-1),2);l.scheduleTick(to,this,10);}return units-m;}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(!WorldConfig.TAINT.get())return;int meta=s.getValue(LEVEL);
        if(meta>=2&&l.isEmptyBlock(p.above())&&r.nextInt(50)==0){var slime=WorldEntities.SLIME.get().create(l);if(slime!=null){slime.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);slime.setSize(meta>=6?2:1,true);if(l.addFreshEntity(slime))l.removeBlock(p,false);}return;}
        if(r.nextInt(4)==0){if(meta==0)l.setBlockAndUpdate(p,r.nextBoolean()?Blocks.AIR.defaultBlockState():((TaintBlock)WorldContent.block("taint_fibre")).attached(l,p));else l.setBlockAndUpdate(p,s.setValue(LEVEL,meta-1));AuraSystem.add(l,p,1,true);}
    }
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){
        if(l.isClientSide)return;int meta=s.getValue(LEVEL);
        if(e instanceof ThaumicSlime slime){if(slime.getSize()<meta&&l.random.nextBoolean()){slime.setSize(slime.getSize()+1,true);if(meta>1)l.setBlockAndUpdate(p,s.setValue(LEVEL,meta-1));else l.removeBlock(p,false);}}
        else{double factor=1-(meta+1)/8.0;e.setDeltaMovement(e.getDeltaMovement().multiply(factor,1,factor));if(e instanceof LivingEntity living)living.addEffect(new MobEffectInstance(WorldEntities.VIS_EXHAUST,600,meta/3));}
    }
}
