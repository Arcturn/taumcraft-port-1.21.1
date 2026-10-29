package thaumcraft.port.world;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;

public final class CrystalClusterBlock extends Block {
    public static final IntegerProperty SIZE=IntegerProperty.create("size",0,3),GEN=IntegerProperty.create("gen",1,4);
    public static final Map<Direction,BooleanProperty> FACES=Map.of(Direction.UP,BlockStateProperties.UP,Direction.DOWN,BlockStateProperties.DOWN,
            Direction.NORTH,BlockStateProperties.NORTH,Direction.SOUTH,BlockStateProperties.SOUTH,Direction.EAST,BlockStateProperties.EAST,Direction.WEST,BlockStateProperties.WEST);
    public static final TagKey<Block> SUPPORT=TagKey.create(Registries.BLOCK,WorldContent.id("crystal_support"));
    public final Aspect aspect;
    public CrystalClusterBlock(Aspect a,Properties p){super(p);aspect=a;var s=stateDefinition.any().setValue(SIZE,0).setValue(GEN,1);for(var f:FACES.values())s=s.setValue(f,false);registerDefaultState(s);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(SIZE,GEN);FACES.values().forEach(b::add);}
    public BlockState attached(BlockGetter l,BlockPos p,BlockState s){for(var d:Direction.values())s=s.setValue(FACES.get(d),supported(l,p,d));return s;}
    private static boolean supported(BlockGetter l,BlockPos p,Direction d){var q=p.relative(d);var s=l.getBlockState(q);return s.is(SUPPORT)&&s.isFaceSturdy(l,q,d.getOpposite());}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){for(var d:Direction.values())if(supported(l,p,d))return true;return false;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=attached(c.getLevel(),c.getClickedPos(),defaultBlockState());return s.canSurvive(c.getLevel(),c.getClickedPos())?s:null;}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbour,LevelAccessor l,BlockPos p,BlockPos q){
        if(!s.canSurvive(l,p))l.scheduleTick(p,this,1);return attached(l,p,s);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(!s.canSurvive(l,p))l.destroyBlock(p,true);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        VoxelShape shape=Shapes.empty();double h=3+s.getValue(SIZE)*2;
        for(var d:Direction.values())if(s.getValue(FACES.get(d)))shape=Shapes.or(shape,switch(d){
            case DOWN->box(1,0,1,15,h,15);case UP->box(1,16-h,1,15,16,15);case EAST->box(16-h,1,1,16,15,15);
            case WEST->box(0,1,1,h,15,15);case NORTH->box(1,1,0,15,15,h);case SOUTH->box(1,1,16-h,15,15,16);});
        return shape.isEmpty()?Shapes.block():shape;
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(!WorldConfig.AURA.get()||r.nextInt(3+s.getValue(GEN))!=0)return;
        var a=AuraSystem.get(l,p);if(a==null)return;
        boolean flux=aspect==Aspect.FLUX;float energy=flux?a.flux():a.vis();int size=s.getValue(SIZE),gen=s.getValue(GEN);
        if(energy<=10){
            if(size>0){l.setBlockAndUpdate(p,s.setValue(SIZE,size-1));AuraSystem.add(l,p,10,flux);}
            else for(var d:Direction.values())if(l.hasChunkAt(p.relative(d))&&l.getBlockState(p.relative(d)).is(this)){l.removeBlock(p,false);AuraSystem.add(l,p,10,flux);break;}
        }else if(energy>a.base()+10){
            if(size<3&&size<5-gen+p.asLong()%3){if(AuraSystem.drain(l,p,10,flux,false)==10)l.setBlockAndUpdate(p,s.setValue(SIZE,size+1));}
            else if(gen<4){
                var q=p.offset(r.nextInt(3)-1,r.nextInt(3)-1,r.nextInt(3)-1);
                if(!q.equals(p)&&l.hasChunkAt(q)&&l.getBlockState(q).canBeReplaced()&&l.getFluidState(q).isEmpty()&&r.nextInt(16)==0){
                    var child=attached(l,q,defaultBlockState().setValue(GEN,gen+(r.nextInt(6)==0?0:1)));
                    if(child.canSurvive(l,q)&&AuraSystem.drain(l,p,10,flux,false)==10)l.setBlockAndUpdate(q,child);
                }
            }
        }
    }
}
