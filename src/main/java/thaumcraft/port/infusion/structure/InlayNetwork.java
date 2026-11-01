package thaumcraft.port.infusion.structure;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TC6's horizontal stabilizer-energy network, independent of vanilla redstone power. */
public final class InlayNetwork {
    private static final Map<Level, Work> ACTIVE = new IdentityHashMap<>();
    private static final class Work {
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> queued = new HashSet<>();
        void add(BlockPos pos) { if (queued.add(pos.immutable())) queue.add(pos.immutable()); }
        void around(BlockPos pos) { add(pos); for (Direction face : Direction.Plane.HORIZONTAL) add(pos.relative(face)); }
    }
    public static boolean isConductor(BlockState state) {
        return state.getBlock() instanceof InlayBlock || state.getBlock() instanceof PedestalBlock;
    }
    public static void update(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        Work running = ACTIVE.get(level);
        if (running != null) { running.around(origin); return; }
        Work work = new Work(); work.around(origin); ACTIVE.put(level, work);
        try {
            while (!work.queue.isEmpty()) {
                BlockPos pos = work.queue.removeFirst(); work.queued.remove(pos);
                if (!level.hasChunkAt(pos)) continue;
                BlockState old = level.getBlockState(pos);
                if (!isConductor(old)) continue;
                int source = 0, neighbor = 0;
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    BlockPos other = pos.relative(face);
                    if (!level.hasChunkAt(other)) continue;
                    if (source == 0 && level.getBlockEntity(other) instanceof StabilizerEntity stabilizer) source = stabilizer.getEnergy();
                    BlockState state = level.getBlockState(other);
                    if (isConductor(state)) neighbor = Math.max(neighbor, state.getValue(InlayBlock.CHARGE));
                }
                BlockState next = old.setValue(InlayBlock.CHARGE, Math.max(source, neighbor - 1));
                if (next.getBlock() instanceof InlayBlock) next = InlayBlock.connections(level, pos, next);
                if (!next.equals(old)) {
                    level.setBlock(pos, next, 2);
                    for (Direction face : Direction.Plane.HORIZONTAL) work.add(pos.relative(face));
                }
            }
        } finally { ACTIVE.remove(level); }
    }
    private InlayNetwork() {}
}
