package thaumcraft.port.auromancy.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Source charger sits above either crafting station and forwards interaction to that station. */
public final class WorkbenchChargerBlock extends Block {
    public WorkbenchChargerBlock(Properties properties){super(properties.noOcclusion());}
    public static boolean supported(BlockState state) {
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("thaumcraft")&&(id.getPath().equals("arcane_workbench")||id.getPath().equals("wand_workbench"));
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return supported(level.getBlockState(pos.below()));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().canSurvive(context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);syncBelow(level,pos);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        super.onRemove(state,level,pos,next,moving);if(!state.is(next.getBlock()))syncBelow(level,pos);
    }
    private static void syncBelow(Level level,BlockPos pos) {
        if(level.isClientSide)return;var below=pos.below();var entity=level.getBlockEntity(below);
        if(entity!=null){entity.setChanged();var state=level.getBlockState(below);level.sendBlockUpdated(below,state,state,3);}
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos neighborPos,boolean moving) {
        if(!level.isClientSide&&!state.canSurvive(level,pos))level.destroyBlock(pos,true);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var below=pos.below();
        if(player instanceof ServerPlayer server&&supported(level.getBlockState(below))&&level.getBlockEntity(below) instanceof MenuProvider menu)
            server.openMenu(menu,buffer->buffer.writeBlockPos(below));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
