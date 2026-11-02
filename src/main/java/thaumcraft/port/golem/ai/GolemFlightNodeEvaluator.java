package thaumcraft.port.golem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.*;

/** BETA26 flight expands six axial neighbours and tests the whole body's cell volume. */
public final class GolemFlightNodeEvaluator extends NodeEvaluator {
    @Override public Node getStart() {
        var box = mob.getBoundingBox();
        return getNode(Mth.floor(box.minX), Mth.floor(box.minY + .5), Mth.floor(box.minZ));
    }
    @Override public Target getTarget(double x, double y, double z) {
        return getTargetNodeAt(Mth.floor(x - mob.getBbWidth() / 2), Mth.floor(y + .5), Mth.floor(z - mob.getBbWidth() / 2));
    }
    @Override public int getNeighbors(Node[] neighbours, Node current) {
        int count = 0;
        for (var face : Direction.values()) {
            int x = current.x + face.getStepX(), y = current.y + face.getStepY(), z = current.z + face.getStepZ();
            if (!clear(x, y, z)) continue;
            var node = getNode(x, y, z);
            if (!node.closed) { node.type = PathType.WALKABLE; neighbours[count++] = node; }
        }
        return count;
    }
    private boolean clear(int x, int y, int z) {
        var pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < entityWidth; dx++) for (int dy = 0; dy < entityHeight; dy++) for (int dz = 0; dz < entityDepth; dz++) {
            pos.set(x + dx, y + dy, z + dz);
            var state = currentContext.getBlockState(pos);
            if (!state.isAir() && (!state.getCollisionShape(currentContext.level(), pos).isEmpty() || state.getFluidState().is(FluidTags.LAVA))) return false;
        }
        return true;
    }
    @Override public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) { return PathType.WATER; }
    @Override public PathType getPathType(PathfindingContext context, int x, int y, int z) { return PathType.WATER; }
}
