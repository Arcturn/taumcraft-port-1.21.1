package thaumcraft.port.machine;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class CrucibleBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(box(0,0,0,16,5,16),box(0,0,0,2,16,16),box(14,0,0,16,16,16),box(2,0,0,14,16,2),box(2,0,14,14,16,16));
    public CrucibleBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CrucibleEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==MachineContent.CRUCIBLE_ENTITY.get()?(l,p,s,be)->CrucibleEntity.tick(l,p,s,(CrucibleEntity)be):null;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof CrucibleEntity be))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var fluid=FluidUtil.getFluidContained(stack);
        if(fluid.isPresent()&&fluid.get().is(Fluids.WATER)&&fluid.get().getAmount()>=1000){
            if(be.state.water()>=1000)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if(!level.isClientSide&&FluidUtil.interactWithFluidHandler(player,hand,be.fluidHandler)){
                player.containerMenu.broadcastChanges();be.sync();
                level.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Keep the port's established temporary wrench gesture until the caster is implemented.
        if(player.isShiftKeyDown()&&stack.is(thaumcraft.port.content.PortContent.THAUMOMETER.get())){
            if(!level.isClientSide)be.empty();return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!player.isShiftKeyDown()&&hit.getDirection()==Direction.UP&&!stack.isEmpty()&&!isCaster(stack)&&be.state.boiling()){
            if(level.isClientSide)return ItemInteractionResult.SUCCESS;
            if(player instanceof ServerPlayer serverPlayer&&be.acceptItem(stack.copyWithCount(1),serverPlayer)){
                // The original consumes the inserted ingredient even in creative mode.
                stack.shrink(1);return ItemInteractionResult.CONSUME;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    private static boolean isCaster(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("thaumcraft")&&(id.getPath().equals("caster_basic")||id.getPath().equals("caster_advanced"));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player.isShiftKeyDown()&&player.getMainHandItem().isEmpty()&&level.getBlockEntity(pos) instanceof CrucibleEntity be){
            if(!level.isClientSide)be.empty();return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CrucibleEntity be&&be.state.boiling()){
            if(entity instanceof ItemEntity item)be.collide(item);
            else if(entity instanceof LivingEntity&&entity.tickCount%10==0){
                entity.hurt(level.damageSources().inFire(),1);
                level.playSound(null,pos,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.4f,2+level.random.nextFloat()*.4f);
            }
        }
        super.entityInside(state,level,pos,entity);
    }
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int param){
        return level.getBlockEntity(pos) instanceof CrucibleEntity be&&be.triggerEvent(id,param);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof CrucibleEntity be?be.state.comparatorSignal():0;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(random.nextInt(10)==0&&level.getBlockEntity(pos) instanceof CrucibleEntity be&&be.state.boiling())
            level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,1.2f+random.nextFloat()*.2f,false);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof CrucibleEntity be)be.empty();super.onRemove(state,level,pos,next,moving);
    }
}
