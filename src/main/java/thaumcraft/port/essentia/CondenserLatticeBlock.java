package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;

public final class CondenserLatticeBlock extends Block {
    public static final Map<Direction,BooleanProperty> CONNECTIONS=Map.of(Direction.DOWN,BlockStateProperties.DOWN,
            Direction.UP,BlockStateProperties.UP,Direction.NORTH,BlockStateProperties.NORTH,Direction.SOUTH,BlockStateProperties.SOUTH,
            Direction.WEST,BlockStateProperties.WEST,Direction.EAST,BlockStateProperties.EAST);
    public final boolean dirty;
    public CondenserLatticeBlock(boolean dirty,Properties properties){
        super(properties);this.dirty=dirty;var state=stateDefinition.any();for(var property:CONNECTIONS.values())state=state.setValue(property,false);registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){CONNECTIONS.values().forEach(builder::add);}
    public static BlockState connections(BlockState state,BlockGetter level,BlockPos pos){for(Direction side:Direction.values())state=state.setValue(CONNECTIONS.get(side),connects(level.getBlockState(pos.relative(side)),side));return state;}
    private static boolean connects(BlockState state,Direction side){return state.getBlock() instanceof CondenserLatticeBlock||side==Direction.DOWN&&state.getBlock() instanceof CondenserBlock;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return connections(defaultBlockState(),context.getLevel(),context.getClickedPos());}
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos from){return state.setValue(CONNECTIONS.get(side),connects(neighbor,side));}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){if(!level.isClientSide&&!state.is(previous.getBlock()))level.scheduleTick(pos,this,1);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide)level.scheduleTick(pos,this,1);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))for(Direction side:Direction.values()){
            var adjacent=pos.relative(side);var block=level.getBlockState(adjacent).getBlock();
            if(!level.isClientSide&&block instanceof CondenserLatticeBlock)level.scheduleTick(adjacent,block,1);
            if(level.getBlockEntity(adjacent) instanceof CondenserEntity condenser)condenser.invalidateLattice();
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        var visited=new HashSet<BlockPos>();var pending=new ArrayDeque<BlockPos>();var roots=new LinkedHashSet<BlockPos>();boolean unloaded=false;
        pending.push(pos);visited.add(pos);
        while(!pending.isEmpty()){
            var current=pending.pop();
            for(Direction side:Direction.values()){
                var next=current.relative(side);if(!level.hasChunkAt(next)){unloaded=true;continue;}
                var block=level.getBlockState(next).getBlock();
                if(block instanceof CondenserLatticeBlock&&visited.add(next))pending.push(next);
                if(side==Direction.DOWN&&block instanceof CondenserBlock){roots.add(next);if(level.getBlockEntity(next) instanceof CondenserEntity condenser)condenser.invalidateLattice();}
            }
        }
        if(roots.isEmpty()&&unloaded){level.scheduleTick(pos,this,20);return;}
        if(roots.isEmpty()||roots.stream().noneMatch(root->root.distSqr(pos)<=74))level.destroyBlock(pos,true);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!dirty||!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse("thaumcraft:filter")))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide){
            // Original filter replacement consumes the filter even in creative mode.
            stack.shrink(1);
            if(level.random.nextBoolean()){
                var side=hit.getDirection();level.addFreshEntity(new ItemEntity(level,pos.getX()+.5+side.getStepX()/3.0,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()/3.0,CrystalItem.stack(Aspect.FLUX)));
            }
            level.playSound(null,pos,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.2f,((level.random.nextFloat()-level.random.nextFloat())*.7f+1)*1.6f);
            level.setBlock(pos,connections(CondenserContent.LATTICE.get().defaultBlockState(),level,pos),3);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        double x0=5,y0=5,z0=5,x1=11,y1=11,z1=11;
        for(Direction side:Direction.values())if(connects(level.getBlockState(pos.relative(side)),side))switch(side){
            case DOWN->y0=0;case UP->y1=16;case NORTH->z0=0;case SOUTH->z1=16;case WEST->x0=0;case EAST->x1=16;
        }
        return Block.box(x0,y0,z0,x1,y1,z1);
    }
}
