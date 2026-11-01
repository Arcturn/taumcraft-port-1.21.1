package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public class PedestalBlock extends DisplayPedestalBlock implements InfusionStabilizer {
    private final boolean eldritch;
    public PedestalBlock(boolean eldritch, Properties properties) {
        super(properties); this.eldritch = eldritch;
        registerDefaultState(stateDefinition.any().setValue(InlayBlock.CHARGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(InlayBlock.CHARGE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PedestalEntity(pos, state); }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) { super.onPlace(state, level, pos, old, moving); InlayNetwork.update(level, pos); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) { InlayNetwork.update(level, pos); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) InlayNetwork.update(level, pos.relative(face));
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(InlayBlock.CHARGE) > 0) StructureEffects.ambient.accept(level, pos, state.getValue(InlayBlock.CHARGE), true);
    }
    @Override public float stabilization(Level level, BlockPos pos) { return eldritch ? .1f : 0; }
    @Override public boolean symmetryPenalty(Level level, BlockPos first, BlockPos opposite) {
        return level.getBlockEntity(first) instanceof PedestalEntity a && level.getBlockEntity(opposite) instanceof PedestalEntity b && a.isEmpty() != b.isEmpty();
    }
    @Override public float penalty(Level level, BlockPos pos) { return .1f; }
}
