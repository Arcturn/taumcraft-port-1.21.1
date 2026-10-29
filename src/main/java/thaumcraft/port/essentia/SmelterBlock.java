package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class SmelterBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public final int tier;
    public SmelterBlock(int tier,Properties p){super(p);this.tier=tier;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SmelterEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==EssentiaContent.SMELTER_ENTITY.get()?(w,p,st,e)->((SmelterEntity)e).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof SmelterEntity be)sp.openMenu(be);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof SmelterEntity be)Containers.dropContents(l,p,be);super.onRemove(s,l,p,next,moving);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){if(s.getValue(LIT)){Direction d=s.getValue(FACING);double x=p.getX()+.5+d.getStepX()*.52,y=p.getY()+.25+r.nextDouble()*.25,z=p.getZ()+.5+d.getStepZ()*.52;l.addParticle(ParticleTypes.SMOKE,x,y,z,0,.01,0);l.addParticle(ParticleTypes.FLAME,x,y,z,0,0,0);}}
}
