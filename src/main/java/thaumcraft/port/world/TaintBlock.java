package thaumcraft.port.world;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
public final class TaintBlock extends Block{
    private final String kind;
    public TaintBlock(String kind,Properties p){super(p.randomTicks());this.kind=kind;var s=stateDefinition.any();for(var f:CrystalClusterBlock.FACES.values())s=s.setValue(f,false);registerDefaultState(s);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){CrystalClusterBlock.FACES.values().forEach(b::add);}
    public BlockState attached(BlockGetter l,BlockPos p){var s=defaultBlockState();for(var d:Direction.values()){var q=p.relative(d);var n=l.getBlockState(q);s=s.setValue(CrystalClusterBlock.FACES.get(d),!TaintSystem.tainted(n)&&n.isFaceSturdy(l,q,d.getOpposite()));}return s;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return kind.equals("taint_fibre")?attached(c.getLevel(),c.getClickedPos()):defaultBlockState();}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){return kind.equals("taint_fibre")?attached(l,p):s;}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind.equals("taint_fibre")?Shapes.empty():Shapes.block();}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(!kind.equals("taint_fibre"))return Shapes.block();var shape=Shapes.empty();
        for(var d:Direction.values())if(s.getValue(CrystalClusterBlock.FACES.get(d)))shape=Shapes.or(shape,switch(d){case DOWN->box(0,0,0,16,1,16);case UP->box(0,15,0,16,16,16);case WEST->box(0,0,0,1,16,16);case EAST->box(15,0,0,16,16,16);case NORTH->box(0,0,0,16,16,1);case SOUTH->box(0,0,15,16,16,16);});
        return shape;
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(!WorldConfig.TAINT.get())return;
        if(!TaintSystem.nearby(l,p)){
            if(r.nextInt(10)==0)l.setBlockAndUpdate(p,switch(kind){case "taint_fibre","taint_log"->Blocks.AIR.defaultBlockState();case "taint_crust"->WorldContent.block("flux_goo").defaultBlockState();case "taint_soil"->Blocks.DIRT.defaultBlockState();case "taint_rock"->WorldContent.block("stone_porous").defaultBlockState();default->s;});return;
        }
        if(kind.equals("taint_fibre")){
            var attached=attached(l,p);boolean support=CrystalClusterBlock.FACES.values().stream().anyMatch(attached::getValue);
            if(!support){l.removeBlock(p,false);return;}if(attached!=s)l.setBlock(p,attached,2);
        }
        if(kind.equals("taint_crust")&&l.isEmptyBlock(p.below())){l.setBlockAndUpdate(p.below(),s);l.removeBlock(p,false);return;}
        TaintSystem.spread(l,p,false);
    }
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){if(!l.isClientSide&&e instanceof LivingEntity living&&!(e instanceof TaintSeed)&&l.random.nextInt(kind.equals("taint_fibre")?750:250)==0)living.addEffect(new MobEffectInstance(WorldEntities.TAINT,200));}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){entityInside(s,l,p,e);super.stepOn(l,p,s,e);}
}
