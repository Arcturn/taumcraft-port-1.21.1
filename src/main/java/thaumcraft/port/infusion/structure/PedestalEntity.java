package thaumcraft.port.infusion.structure;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** Infusion altar item stand and its path to a powered stabilizer. */
public final class PedestalEntity extends DisplayPedestalEntity {
    public PedestalEntity(BlockPos pos, BlockState state) { super(StructureContent.PEDESTAL_ENTITY.get(), pos, state); }
    public BlockPos findInstabilityMitigator() {
        if (level == null || !getBlockState().hasProperty(InlayBlock.CHARGE) || getBlockState().getValue(InlayBlock.CHARGE) == 0) return null;
        return findSource(worldPosition, getBlockState().getValue(InlayBlock.CHARGE));
    }
    private BlockPos findSource(BlockPos pos, int charge) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(face);
            if (!level.hasChunkAt(next)) continue;
            if (level.getBlockEntity(next) instanceof StabilizerEntity stabilizer && stabilizer.getEnergy() >= 5) return next;
            BlockState state = level.getBlockState(next);
            if (state.hasProperty(InlayBlock.CHARGE) && state.getValue(InlayBlock.CHARGE) > charge) {
                BlockPos source = findSource(next, state.getValue(InlayBlock.CHARGE));
                if (source != null) return source;
            }
        }
        return null;
    }
}
