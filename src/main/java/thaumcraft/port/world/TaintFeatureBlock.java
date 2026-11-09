package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.port.mob.MobContent;

/** The six-sided swollen taint growth from BETA26, including its living contents. */
public final class TaintFeatureBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;

    public TaintFeatureBlock(Properties properties){
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return switch(state.getValue(FACING)){
            case DOWN->box(2,10,2,14,16,14);
            case UP->box(2,0,2,14,6,14);
            case NORTH->box(2,2,10,14,14,16);
            case SOUTH->box(2,2,0,14,14,6);
            case WEST->box(10,2,2,16,14,14);
            case EAST->box(0,2,2,6,14,14);
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        Direction facing=state.getValue(FACING);BlockPos support=pos.relative(facing.getOpposite());
        return level.hasChunkAt(support)&&level.getBlockState(support).isFaceSturdy(level,support,facing);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var state=defaultBlockState().setValue(FACING,context.getClickedFace());
        return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){
        BlockPos support=pos.relative(state.getValue(FACING).getOpposite());
        if(level.hasChunkAt(support)&&!state.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}

    /** Replacement, collapse and harvesting all release the contents, as in breakBlock. */
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!replacement.is(this)&&level instanceof ServerLevel server){
            if(level.random.nextFloat()<.333f){
                var crawler=MobContent.CRAWLER.get().create(server);
                if(crawler!=null){crawler.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,level.random.nextInt(360),0);server.addFreshEntity(crawler);}
            }else AuraSystem.pollute(server,pos,1,true);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    public void die(Level level,BlockPos pos,BlockState state){level.setBlockAndUpdate(pos,WorldContent.block("flux_goo").defaultBlockState());}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!level.hasChunkAt(pos))return;
        if(!TaintSystem.nearby(level,pos)&&random.nextInt(10)==0){die(level,pos,state);return;}
        TaintSystem.spread(level,pos,false);
        if(!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!level.hasChunkAt(pos.below()))return;
        var below=level.getBlockState(pos.below());
        if(below.is(WorldContent.block("taint_log"))&&below.hasProperty(BlockStateProperties.AXIS)
                &&below.getValue(BlockStateProperties.AXIS)==Direction.Axis.Y&&random.nextInt(100)==0)
            level.setBlockAndUpdate(pos,WorldContent.block("taint_geyser").defaultBlockState());
    }
}
