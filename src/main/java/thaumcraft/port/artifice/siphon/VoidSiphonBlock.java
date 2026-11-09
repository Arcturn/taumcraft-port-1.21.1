package thaumcraft.port.artifice.siphon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** The BETA26 device has no facing; redstone disables harvesting and the star fields. */
public final class VoidSiphonBlock extends Block implements EntityBlock {
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    private static final VoxelShape OUTLINE=box(3,0,3,13,16,13);
    private static final VoxelShape COLLISION=Shapes.or(box(3,0,3,13,2,13),box(4,2,4,12,11,12),box(5,12,5,10,16,10));
    public VoidSiphonBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ENABLED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(ENABLED,!context.getLevel().hasNeighborSignal(context.getClickedPos()));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return OUTLINE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return COLLISION;}
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VoidSiphonEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==VoidSiphonContent.ENTITY.get()&&!level.isClientSide?(l,p,s,e)->((VoidSiphonEntity)e).tick():null;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);updateEnabled(state,level,pos);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbour,BlockPos from,boolean moving){updateEnabled(state,level,pos);super.neighborChanged(state,level,pos,neighbour,from,moving);}
    private void updateEnabled(BlockState state,Level level,BlockPos pos){if(!level.isClientSide){boolean enabled=!level.hasNeighborSignal(pos);if(state.getValue(ENABLED)!=enabled)level.setBlock(pos,state.setValue(ENABLED,enabled),3);}}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof VoidSiphonEntity siphon)server.openMenu(siphon);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){useWithoutItem(state,level,pos,player,hit);return ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter){return level.getBlockEntity(pos) instanceof VoidSiphonEntity siphon&&siphon.triggerEvent(id,parameter);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof VoidSiphonEntity siphon)Containers.dropContents(level,pos,siphon);
        super.onRemove(state,level,pos,next,moving);
    }
}
