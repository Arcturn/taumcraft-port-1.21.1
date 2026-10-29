package thaumcraft.port.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WorkbenchBlock extends Block implements EntityBlock {
    public WorkbenchBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WorkbenchEntity(pos,state);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer sp&&level.getBlockEntity(pos) instanceof WorkbenchEntity bench)((net.neoforged.neoforge.common.extensions.IPlayerExtension)sp).openMenu(bench,b->b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof WorkbenchEntity bench)Containers.dropContents(level,pos,bench);
        super.onRemove(state,level,pos,next,moving);
    }
}
