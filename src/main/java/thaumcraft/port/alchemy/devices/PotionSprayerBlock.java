package thaumcraft.port.alchemy.devices;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class PotionSprayerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public PotionSprayerBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var player=context.getPlayer();var pos=context.getClickedPos();Direction facing=context.getHorizontalDirection().getOpposite();
        if(player!=null&&Math.abs(pos.getX()+.5-player.getX())<2&&Math.abs(pos.getZ()+.5-player.getZ())<2){
            if(player.getEyeY()-pos.getY()>2)facing=Direction.UP;else if(pos.getY()-player.getEyeY()>0)facing=Direction.DOWN;
        }
        return defaultBlockState().setValue(FACING,player!=null&&player.isShiftKeyDown()?facing.getOpposite():facing);
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PotionSprayerEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==AlchemyDevices.SPRAYER_ENTITY.get()?(l,p,s,e)->((PotionSprayerEntity)e).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof PotionSprayerEntity sprayer)server.openMenu(sprayer);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){useWithoutItem(state,level,pos,player,hit);return ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter){return level.getBlockEntity(pos) instanceof PotionSprayerEntity sprayer&&sprayer.triggerEvent(id,parameter);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof PotionSprayerEntity sprayer)Containers.dropContents(level,pos,sprayer);super.onRemove(state,level,pos,next,moving);}
}
