package thaumcraft.port.world;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class LootContainerBlock extends Block {
    private final VoxelShape shape;
    public LootContainerBlock(boolean urn,Properties p){super(p);shape=urn?box(2,0,2,14,16,14):box(1,0,1,15,14,15);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape;}
}
