package thaumcraft.port.infusion.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import thaumcraft.port.artifice.mirror.*;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Six-face mounted mirror: links and paid queues belong to its server block entity. */
public final class InfusionMirrorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public InfusionMirrorBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace());return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var face=state.getValue(FACING);return level.getBlockState(pos.relative(face.getOpposite())).isFaceSturdy(level,pos.relative(face.getOpposite()),face);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide&&!state.canSurvive(level,pos))level.destroyBlock(pos,true);}
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int data){var mirror=level.getBlockEntity(pos);return mirror!=null&&mirror.triggerEvent(id,data);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MirrorEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==MirrorContent.ENTITY.get()?(l,p,s,be)->((MirrorEntity)be).tick():null;}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MirrorEntity mirror){var link=MirrorLink.get(stack);if(link!=null)mirror.bind(link);}}
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=new java.util.ArrayList<>(super.getDrops(state,params));
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MirrorEntity mirror&&mirror.destination!=null)for(var stack:drops)if(stack.is(asItem()))MirrorLink.set(stack,mirror.destination);
        return drops;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MirrorEntity mirror)mirror.recover();super.onRemove(state,level,pos,next,moving);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(!level.isClientSide&&entity instanceof ItemEntity item&&level.getBlockEntity(pos) instanceof MirrorEntity mirror)mirror.transport(item);}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){
        case DOWN->Block.box(0,14,0,16,16,16);case UP->Block.box(0,0,0,16,2,16);
        case NORTH->Block.box(0,0,14,16,16,16);case SOUTH->Block.box(0,0,0,16,16,2);
        case WEST->Block.box(14,0,0,16,16,16);case EAST->Block.box(0,0,0,2,16,16);
    };}
}
