package thaumcraft.port.world;

import java.util.List;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;

/** Attached films and four deterministic source growth forms, including luminous crystals. */
public final class TaintFibreBlock extends TaintBlock {
    public static final IntegerProperty GROWTH=IntegerProperty.create("growth",0,4);
    public TaintFibreBlock(Properties properties){
        super("taint_fibre",properties.replaceable().lightLevel(state->state.getValue(GROWTH)==3?12:state.getValue(GROWTH)==2||state.getValue(GROWTH)==4?6:0));
        var state=stateDefinition.any().setValue(GROWTH,0);for(var face:CrystalClusterBlock.FACES.values())state=state.setValue(face,false);registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(GROWTH);CrystalClusterBlock.FACES.values().forEach(builder::add);}
    /** Legacy BlockPos packing preserves the source's position-derived growth distribution. */
    public static long growthSeed(BlockPos pos){return ((long)pos.getX()&67108863L)<<38|((long)pos.getY()&4095L)<<26|(long)pos.getZ()&67108863L;}
    private static boolean drawAt(BlockGetter level,BlockPos pos,Direction side){var neighbour=pos.relative(side);var state=level.getBlockState(neighbour);return !(state.getBlock() instanceof TaintFibreBlock)&&!(state.getBlock() instanceof TaintFeatureBlock)&&state.isFaceSturdy(level,neighbour,side.getOpposite());}
    @Override public BlockState attached(BlockGetter level,BlockPos pos){
        var state=defaultBlockState();for(var face:Direction.values())state=state.setValue(CrystalClusterBlock.FACES.get(face),drawAt(level,pos,face));
        int roll=new Random(growthSeed(pos)).nextInt(50),growth=0;
        if(state.getValue(CrystalClusterBlock.FACES.get(Direction.DOWN)))growth=roll<4?1:roll==4||roll==5?2:roll==6?3:0;
        if(state.getValue(CrystalClusterBlock.FACES.get(Direction.UP))&&roll>47)growth=4;
        return state.setValue(GROWTH,growth);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return attached(context.getLevel(),context.getClickedPos());}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){super.onPlace(state,level,pos,old,moved);if(!level.isClientSide){var actual=attached(level,pos);if(actual!=state)level.setBlock(pos,actual,2);level.scheduleTick(pos,this,1);}}
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){
        if(!TaintSystem.neighboursLoaded(level,pos))return state;var actual=attached(level,pos);if(actual.getValue(GROWTH)==0&&TaintSystem.onlyAdjacentToTaint(level,pos))level.scheduleTick(pos,this,1);return actual;
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!TaintSystem.neighboursLoaded(level,pos))return;
        var actual=attached(level,pos);
        if(actual.getValue(GROWTH)==0&&TaintSystem.onlyAdjacentToTaint(level,pos)||!TaintSystem.nearby(level,pos)){die(level,pos,state);return;}
        if(actual!=state)level.setBlock(pos,actual,3);TaintSystem.spread(level,pos,false);
    }
    private static VoxelShape filmShape(BlockState state){
        VoxelShape shape=Shapes.empty();for(var face:Direction.values())if(state.getValue(CrystalClusterBlock.FACES.get(face)))shape=Shapes.or(shape,switch(face){
            case DOWN->box(0,0,0,16,.8,16);case UP->box(0,15.2,0,16,16,16);case WEST->box(0,0,0,.8,16,16);case EAST->box(15.2,0,0,16,16,16);case NORTH->box(0,0,0,16,16,.8);case SOUTH->box(0,0,15.2,16,16,16);});return shape;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return filmShape(attached(level,pos));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        var actual=attached(level,pos);var film=filmShape(actual);return Shapes.or(film,switch(actual.getValue(GROWTH)){
            case 1->box(1.6,0,1.6,14.4,6.4,14.4);case 2->box(3.2,0,3.2,12.8,16,12.8);case 3->box(4,0,4,12,5,12);case 4->box(1.6,4.8,1.6,14.4,16,14.4);default->Shapes.empty();});
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var origin=params.getOptionalParameter(LootContextParams.ORIGIN);if(origin==null)return List.of();
        var actual=attached(params.getLevel(),BlockPos.containing(origin));if(actual.getValue(GROWTH)!=3)return List.of();
        var tool=params.getOptionalParameter(LootContextParams.TOOL);int fortune=enchantmentLevel(params.getLevel(),tool==null?ItemStack.EMPTY:tool,Enchantments.FORTUNE);
        return params.getLevel().random.nextInt(5)<=fortune?List.of(CrystalItem.stack(Aspect.FLUX)):List.of();
    }
    @Override protected void spawnAfterBreak(BlockState state,ServerLevel level,BlockPos pos,ItemStack tool,boolean experience){super.spawnAfterBreak(state,level,pos,tool,experience);if(attached(level,pos).getValue(GROWTH)==3)AuraSystem.pollute(level,pos,1,true);}
    public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 3;}
    public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 3;}
}
