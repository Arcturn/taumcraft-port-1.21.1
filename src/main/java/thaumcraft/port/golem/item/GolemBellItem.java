package thaumcraft.port.golem.item;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.golem.seal.*;
import thaumcraft.port.research.ResearchStorage;

public final class GolemBellItem extends Item {
    public GolemBellItem(Properties properties) { super(properties.stacksTo(1)); }
    private static boolean logistics(ServerPlayer player) { return ResearchStorage.get(player.getServer()).player(player.getUUID()).isResearchKnown("GOLEMLOGISTICS"); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer(); if (player == null) return InteractionResult.PASS;
        var pos = new SealPos(context.getClickedPos(), context.getClickedFace());
        if (SealHandler.getSealEntity(context.getLevel(), pos) != null) {
            if (player instanceof ServerPlayer server && SealHandler.interact(server, pos, player.isShiftKeyDown()) && player.isShiftKeyDown())
                context.getLevel().playSound(null, pos.pos, PortSounds.get("zap"), SoundSource.BLOCKS, .5f, 1);
            player.swing(context.getHand()); return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (player instanceof ServerPlayer server && player.isShiftKeyDown() && logistics(server)) {
            SealHandler.openLogistics(server, context.getClickedPos(), context.getClickedFace()); player.swing(context.getHand(), true); return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand); player.swing(hand);
        if (level.isClientSide) { player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, .6f, 1 + level.random.nextFloat() * .1f); return InteractionResultHolder.success(stack); }
        if (!(player instanceof ServerPlayer server)) return InteractionResultHolder.pass(stack);
        var seal = getSeal(player);
        if (seal != null) {
            if (SealHandler.interact(server, seal.getSealPos(), player.isShiftKeyDown()) && player.isShiftKeyDown())
                level.playSound(null, seal.getSealPos().pos, PortSounds.get("zap"), SoundSource.BLOCKS, .5f, 1);
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown() && logistics(server)) {
            var hit = player.pick(5, 0, false);
            if (hit.getType() == HitResult.Type.MISS) { SealHandler.openLogistics(server, null, null); return InteractionResultHolder.success(stack); }
        }
        return InteractionResultHolder.pass(stack);
    }
    /** Source seal picking traces block cubes, allowing selection independently of block collision models. */
    public static ISealEntity getSeal(Player player) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(5));
        BlockPos previous = null;
        for (int step = 1; step < 50; step++) {
            var pos = BlockPos.containing(start.lerp(end, step / 50.0)); if (pos.equals(previous)) continue; previous = pos;
            var hit = new AABB(pos).clip(start, end); if (hit.isEmpty()) continue;
            var point = hit.get(); Direction face = nearestFace(point, pos);
            var seal = SealHandler.getSealEntity(player.level(), new SealPos(pos, face)); if (seal != null) return seal;
        }
        return null;
    }
    private static Direction nearestFace(Vec3 point, BlockPos pos) {
        double[] distances = {Math.abs(point.x-pos.getX()),Math.abs(point.x-pos.getX()-1),Math.abs(point.y-pos.getY()),Math.abs(point.y-pos.getY()-1),Math.abs(point.z-pos.getZ()),Math.abs(point.z-pos.getZ()-1)};
        Direction[] faces = {Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH};
        int best = 0; for (int i = 1; i < distances.length; i++) if (distances[i] < distances[best]) best = i; return faces[best];
    }
}
