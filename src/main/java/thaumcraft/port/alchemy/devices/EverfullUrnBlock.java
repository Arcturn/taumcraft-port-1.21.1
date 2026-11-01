package thaumcraft.port.alchemy.devices;

import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class EverfullUrnBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=box(3,0,3,13,16,13);
    public EverfullUrnBlock(Properties properties){super(properties);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EverfullUrnEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==AlchemyDevices.URN_ENTITY.get()?(l,p,s,e)->((EverfullUrnEntity)e).tick():null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof EverfullUrnEntity urn))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        if(FluidUtil.interactWithFluidHandler(player,hand,urn.fluidHandler)){player.containerMenu.broadcastChanges();level.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);return ItemInteractionResult.CONSUME;}
        if(stack.is(Items.GLASS_BOTTLE)&&urn.water()>=333){
            var potion=PotionContents.createItemStack(Items.POTION,Potions.WATER);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            if(stack.isEmpty())player.setItemInHand(hand,potion);else if(!player.getInventory().add(potion))player.drop(potion,false);
            urn.fluidHandler.drain(333,FluidAction.EXECUTE);
            level.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
            player.containerMenu.broadcastChanges();
        }
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof EverfullUrnEntity urn&&urn.water()==1000)DeviceVisuals.urnSplash.accept(urn);}
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter){return level.getBlockEntity(pos) instanceof EverfullUrnEntity urn&&urn.triggerEvent(id,parameter);}
}
