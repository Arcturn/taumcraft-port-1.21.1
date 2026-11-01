package thaumcraft.port.infusion.matrix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Interaction is deliberately through the caster, as in BETA26. */
public final class InfusionMatrixBlock extends Block implements EntityBlock {
    public InfusionMatrixBlock(Properties properties){super(properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfusionMatrixEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return type==MatrixContent.ENTITY.get()?(world,pos,block,entity)->((InfusionMatrixEntity)entity).tick():null;
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(level.getBlockEntity(pos) instanceof InfusionMatrixEntity matrix)matrix.checkSurroundings=true;
    }
}
