package thaumcraft.port.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class NitorBlock extends Block implements EntityBlock {
    public final net.minecraft.world.item.DyeColor color;
    public NitorBlock(net.minecraft.world.item.DyeColor color,Properties p){super(p.strength(.1f).sound(SoundType.WOOL).mapColor(color.getMapColor()));this.color=color;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NitorEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide&&type==MachineContent.NITOR_ENTITY.get()?(l,p,s,e)->((NitorEntity)e).tick():null;}
    // Older alpha worlds have Nitor states without saved block entities; create the client ticker lazily.
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){level.getBlockEntity(pos);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.create(.33f,.33f,.33f,.66f,.66f,.66f);}
}
