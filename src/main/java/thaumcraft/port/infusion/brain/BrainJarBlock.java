package thaumcraft.port.infusion.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The glass shell is a block model; its floating contents are a separate renderer. */
public final class BrainJarBlock extends Block implements EntityBlock {
    public BrainJarBlock(Properties properties){super(properties.noOcclusion());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,0,3,13,12,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BrainJarEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide&&type==BrainJarContent.ENTITY.get()?(world,pos,current,entity)->((BrainJarEntity)entity).clientTick():null;
    }
}
