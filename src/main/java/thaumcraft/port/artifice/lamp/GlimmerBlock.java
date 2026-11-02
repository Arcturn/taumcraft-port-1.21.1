package thaumcraft.port.artifice.lamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Invisible, replaceable light placed by an arcane lamp; never a player inventory item. */
public final class GlimmerBlock extends Block {
    public GlimmerBlock(Properties properties) { super(properties); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }
}
