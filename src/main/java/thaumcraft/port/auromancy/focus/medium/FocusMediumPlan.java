package thaumcraft.port.auromancy.focus.medium;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.NodeSetting;
import thaumcraft.port.auromancy.item.CasterManager;
import thaumcraft.port.content.CasterItem;

/** BETA26 architect volume/surface selection, including its face-dependent axis mapping. */
public final class FocusMediumPlan extends FocusMedium {
    @Override public String getResearch() { return "FOCUSPLAN"; }
    @Override public String getKey() { return "thaumcraft.PLAN"; }
    @Override public int getComplexity() { return 4; }
    @Override public Aspect getAspect() { return Aspect.CRAFT; }
    @Override public boolean isExclusive() { return true; }
    @Override public NodeSetting[] createSettings() { return new NodeSetting[]{new NodeSetting("method", "focus.plan.method", new NodeSetting.NodeSettingIntList(new int[]{0,1},new String[]{"focus.plan.full","focus.plan.surface"}))}; }
    @Override public HitResult[] supplyTargets() {
        if (getParent() == null || !(getPackage().getCaster() instanceof Player player)) return new HitResult[0];
        var stack = player.getItemInHand(getPackage().getCastingHand());
        if (!(stack.getItem() instanceof CasterItem)) stack = player.getMainHandItem().getItem() instanceof CasterItem ? player.getMainHandItem() : player.getOffhandItem();
        var targets = new ArrayList<HitResult>();
        var paths = getParent().supplyTrajectories();
        if (paths == null) return new HitResult[0];
        for (var path : paths) {
            var hit = getPackage().world.clip(new ClipContext(path.source,path.source.add(path.direction.normalize().scale(16)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            var blocks = getArchitectBlocks(stack,getPackage().world,hit.getBlockPos(),hit.getDirection(),player);
            blocks.sort(Comparator.comparingDouble(p -> p.distSqr(hit.getBlockPos())));
            for (var pos : blocks) targets.add(new BlockHitResult(Vec3.atCenterOf(pos),hit.getDirection(),pos,false));
        }
        return targets.toArray(HitResult[]::new);
    }
    public BlockHitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        var eye = player.getEyePosition();
        return world.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(16)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
    }
    public boolean useBlockHighlight(ItemStack stack) { return false; }
    public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, Direction.Axis axis) {
        if (stack.isEmpty()) return false;
        int dim = CasterManager.getAreaDim(stack);
        if (getSettingValue("method") == 0) return dim == 0 || dim == switch (axis) {case X -> 1; case Z -> 2; case Y -> 3;};
        return switch (side.getAxis()) {
            case Y -> (axis == Direction.Axis.X && (dim == 0 || dim == 1)) || (axis == Direction.Axis.Z && (dim == 0 || dim == 2));
            case Z -> (axis == Direction.Axis.Y && (dim == 0 || dim == 1)) || (axis == Direction.Axis.X && (dim == 0 || dim == 2));
            case X -> (axis == Direction.Axis.Y && (dim == 0 || dim == 1)) || (axis == Direction.Axis.Z && (dim == 0 || dim == 2));
        };
    }
    public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos origin, Direction side, Player player) {
        var result = new ArrayList<BlockPos>();
        if (stack == null || stack.isEmpty()) return result;
        int x = CasterManager.getAreaX(stack), y = CasterManager.getAreaY(stack), z = CasterManager.getAreaZ(stack);
        if (getSettingValue("method") == 0) {
            var center = origin.offset(-x*side.getStepX(),-y*side.getStepY(),-z*side.getStepZ());
            for (var pos : BlockPos.betweenClosed(center.offset(-x,-y,-z),center.offset(x,y,z)))
                if (!world.isEmptyBlock(pos)) result.add(pos.immutable());
            return result;
        }
        if (side.getAxis() == Direction.Axis.Z) { int swap=x; x=z; z=swap; }
        var state = world.getBlockState(origin);
        var queue = new ArrayDeque<BlockPos>(); var checked = new HashSet<BlockPos>(); queue.add(origin);
        while (!queue.isEmpty()) {
            var pos = queue.removeFirst(); if (!checked.add(pos)) continue;
            int first = side.getAxis() == Direction.Axis.X ? pos.getY()-origin.getY() : pos.getX()-origin.getX();
            int second = side.getAxis() == Direction.Axis.Z ? pos.getY()-origin.getY() : pos.getZ()-origin.getZ();
            if (Math.abs(first)>x || Math.abs(second)>z || world.getBlockState(pos)!=state || state.isAir() || !exposed(world,pos)) continue;
            result.add(pos);
            for (var direction : Direction.values()) if (direction.getAxis()!=side.getAxis()) queue.addLast(pos.relative(direction));
        }
        return result;
    }
    private static boolean exposed(Level world, BlockPos pos) {
        for (var direction : Direction.values()) {
            var adjacent = pos.relative(direction);
            if (!world.getBlockState(adjacent).isSolidRender(world,adjacent)) return true;
        }
        return false;
    }
}
