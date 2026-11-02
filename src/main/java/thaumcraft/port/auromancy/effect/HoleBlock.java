package thaumcraft.port.auromancy.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class HoleBlock extends Block implements EntityBlock {
    public HoleBlock(Properties properties){super(properties);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    /** Source rock material and isSideSolid survive even though entities walk through the hole. */
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.block();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HoleBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==FocusEffectContent.HOLE_ENTITY.get()?(world,pos,block,entity)->((HoleBlockEntity)entity).tick():null;}
}
