package thaumcraft.port.auromancy.focal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.port.auromancy.AuromancyContent;

public final class FocalManipulatorBlock extends Block implements EntityBlock {
    public FocalManipulatorBlock(Properties properties){super(properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FocalManipulatorEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return type==AuromancyContent.FOCAL_ENTITY.get()?(l,p,s,e)->FocalManipulatorEntity.tick((FocalManipulatorEntity)e):null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof FocalManipulatorEntity focal)sp.openMenu(focal,b->b.writeBlockPos(p));
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value){
        super.triggerEvent(state,level,pos,id,value);var be=level.getBlockEntity(pos);return be!=null&&be.triggerEvent(id,value);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof FocalManipulatorEntity focal)Containers.dropContents(l,p,focal);
        super.onRemove(s,l,p,next,moving);
    }
}
