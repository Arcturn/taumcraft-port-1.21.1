package thaumcraft.port.theory;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ResearchTableBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    static final VoxelShape SHAPE=Shapes.block();
    public ResearchTableBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ResearchTableEntity(p,s);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof ResearchTableEntity table)((net.neoforged.neoforge.common.extensions.IPlayerExtension)sp).openMenu(table,b->b.writeBlockPos(p));
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof ResearchTableEntity table)Containers.dropContents(l,p,table);super.onRemove(s,l,p,next,moving);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){
        if(r.nextInt(5)!=0||!(l.getBlockEntity(p) instanceof ResearchTableEntity table)||!table.active())return;
        double x=r.nextGaussian()/2,z=r.nextGaussian()/2,y=1.5+r.nextFloat();
        l.addParticle(thaumcraft.port.content.PortParticles.get("theory_rune"),p.getX()+.5+x,p.getY()+y,p.getZ()+.5+z,-x,.85-y,-z);
    }
}
