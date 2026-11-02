package thaumcraft.port.infusion.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

/** The glass shell is a block model; its floating contents are a separate renderer. */
public final class BrainJarBlock extends Block implements EntityBlock {
    public BrainJarBlock(Properties properties){super(properties.noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,0,3,13,12,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BrainJarEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return type==BrainJarContent.ENTITY.get()?(world,pos,current,entity)->((BrainJarEntity)entity).tick():null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof BrainJarEntity jar&&!level.isClientSide)jar.releaseExperience();return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof BrainJarEntity jar&&!level.isClientSide)jar.releaseExperience();return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof BrainJarEntity jar?jar.comparator():0;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){super.onRemove(state,level,pos,next,moving);if(!state.is(next.getBlock()))level.updateNeighbourForOutputSignal(pos,this);}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){var drops=super.getDrops(state,params);if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BrainJarEntity jar)for(var stack:drops)if(stack.is(asItem()))jar.savePortable(stack);return drops;}
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){var stack=new ItemStack(this);if(level.getBlockEntity(pos) instanceof BrainJarEntity jar)jar.savePortable(stack);return stack;}
    @Override public float getEnchantPowerBonus(BlockState state,LevelReader level,BlockPos pos){return 5;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof BrainJarEntity jar&&jar.experience()>=BrainJarEntity.CAPACITY)BrainJarContent.fullSpark.accept(level,pos);}
}
