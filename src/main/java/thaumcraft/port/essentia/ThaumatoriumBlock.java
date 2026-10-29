package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.port.machine.MachineContent;

public final class ThaumatoriumBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public final boolean top;
    public ThaumatoriumBlock(boolean top,Properties p){super(p);this.top=top;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ThaumatoriumEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T>t){return t==EssentiaContent.THAUMATORIUM_ENTITY.get()?(w,p,st,e)->((ThaumatoriumEntity)e).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof ThaumatoriumEntity be&&be.main()!=null)sp.openMenu(be.main());return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){
        if(l.isClientSide)return;boolean valid=top?l.getBlockState(p.below()).is(EssentiaContent.THAUMATORIUM.get()):l.getBlockState(p.below()).is(MachineContent.CRUCIBLE.get())&&l.getBlockState(p.above()).is(EssentiaContent.THAUMATORIUM_TOP.get());
        if(!valid)l.setBlock(p,EssentiaContent.CONSTRUCT.get().defaultBlockState(),3);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof ThaumatoriumEntity be&&!top){Containers.dropContents(l,p,be);if(l instanceof net.minecraft.server.level.ServerLevel server&&be.contents.total()>0)thaumcraft.port.world.AuraSystem.add(server,p,be.contents.total(),true);}
        super.onRemove(s,l,p,next,moving);
        if(!s.is(next.getBlock())&&!l.isClientSide){BlockPos q=top?p.below():p.above();if(l.getBlockState(q).is(top?EssentiaContent.THAUMATORIUM.get():EssentiaContent.THAUMATORIUM_TOP.get()))l.setBlock(q,EssentiaContent.CONSTRUCT.get().defaultBlockState(),3);}
    }
}
