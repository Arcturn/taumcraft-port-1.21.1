package thaumcraft.port.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CrucibleBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(box(0,0,0,16,3,16),box(0,3,0,2,16,16),box(14,3,0,16,16,16),box(2,3,0,14,16,2),box(2,3,14,14,16,16));
    public CrucibleBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CrucibleEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==MachineContent.CRUCIBLE_ENTITY.get()?(l,p,s,be)->CrucibleEntity.tick(l,p,s,(CrucibleEntity)be):null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof CrucibleEntity be))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(stack.is(Items.WATER_BUCKET)&&be.state.water()<1000){
            if(!level.isClientSide){be.state.fill(1000,false);if(!player.isCreative())player.setItemInHand(hand,new ItemStack(Items.BUCKET));be.sync();}
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(player.isShiftKeyDown()&&stack.is(thaumcraft.port.content.PortContent.THAUMOMETER.get())){if(!level.isClientSide)be.empty();return ItemInteractionResult.sidedSuccess(level.isClientSide);}
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CrucibleEntity be)player.displayClientMessage(net.minecraft.network.chat.Component.literal("Water: "+be.state.water()+"  Heat: "+be.state.heat()+"  "+thaumcraft.port.aspect.Aspect.describe(be.state.aspects())),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof CrucibleEntity be)be.empty();super.onRemove(state,level,pos,next,moving);
    }
}
