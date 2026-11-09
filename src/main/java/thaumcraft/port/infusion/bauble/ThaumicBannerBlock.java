package thaumcraft.port.infusion.bauble;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.essentia.*;

public final class ThaumicBannerBlock extends Block implements EntityBlock {
    public static final BooleanProperty WALL=BooleanProperty.create("wall");
    public static final IntegerProperty ROTATION=BlockStateProperties.ROTATION_16;
    public final DyeColor dye;
    public final boolean crimson;
    public ThaumicBannerBlock(DyeColor dye,Properties properties){this(dye,false,properties);}
    public ThaumicBannerBlock(DyeColor dye,boolean crimson,Properties properties){super(properties);this.dye=dye;this.crimson=crimson;registerDefaultState(stateDefinition.any().setValue(WALL,false).setValue(ROTATION,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(WALL,ROTATION);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction face=context.getClickedFace();if(face==Direction.DOWN)return null;
        int rotation=face==Direction.UP?Mth.floor((context.getRotation()+180)*16/360+.5)&15:switch(face){case NORTH->8;case WEST->4;case EAST->12;default->0;};
        return defaultBlockState().setValue(WALL,face!=Direction.UP).setValue(ROTATION,rotation);
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(!state.getValue(WALL))return Block.box(5.28,0,5.28,10.56,32,10.56);
        return switch(state.getValue(ROTATION)){case 8->Block.box(0,-16,12,16,16,16);case 12->Block.box(0,-16,0,4,16,16);case 4->Block.box(12,-16,0,16,16,16);default->Block.box(0,-16,0,16,16,4);};
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ThaumicBannerEntity(pos,state);}
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        apply(level,pos,player,held);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){apply(level,pos,player,ItemStack.EMPTY);return InteractionResult.sidedSuccess(level.isClientSide);}
    public static void apply(Level level,BlockPos pos,Player player,ItemStack held){
        if(level.isClientSide||level.getBlockState(pos).getBlock() instanceof ThaumicBannerBlock block&&block.crimson||!(level.getBlockEntity(pos) instanceof ThaumicBannerEntity banner)||!player.isShiftKeyDown()&&!held.is(EssentiaContent.PHIAL.get()))return;
        if(player.isShiftKeyDown())banner.setAspect(null);else{var aspect=EssentiaItem.aspect(held);if(aspect!=null){banner.setAspect(aspect);held.shrink(1);}}
        level.playSound(null,pos,SoundEvents.WOOL_HIT,SoundSource.BLOCKS,1,1);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){super.setPlacedBy(level,pos,state,entity,stack);if(level.getBlockEntity(pos) instanceof ThaumicBannerEntity banner)banner.setAspect(EssentiaItem.aspect(stack));}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var stack=new ItemStack(this);if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ThaumicBannerEntity banner)banner.savePortable(stack);return List.of(stack);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){var stack=new ItemStack(this);if(level.getBlockEntity(pos) instanceof ThaumicBannerEntity banner)banner.savePortable(stack);return stack;}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(ROTATION,rotation.rotate(state.getValue(ROTATION),16));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.setValue(ROTATION,mirror.mirror(state.getValue(ROTATION),16));}
}
