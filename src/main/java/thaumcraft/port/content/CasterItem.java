package thaumcraft.port.content;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.port.essentia.BufferEntity;
import thaumcraft.port.essentia.TubeBlock;
import thaumcraft.port.essentia.TubeEntity;
import thaumcraft.port.infusion.matrix.InfusionMatrixEntity;

/** The unfocused caster's original block interactions. Focus casting is a separate system. */
public final class CasterItem extends Item {
    public CasterItem(Properties properties) { super(properties); }

    @Override public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var entity = level.getBlockEntity(pos);
        if (entity instanceof InfusionMatrixEntity matrix) {
            if (player instanceof ServerPlayer serverPlayer) matrix.activateOrStart(serverPlayer);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(entity instanceof TubeEntity tube)) return InteractionResult.PASS;
        var relative = context.getClickLocation().subtract(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        double half = tube instanceof BufferEntity ? .25 : .125;
        Direction port = Math.abs(relative.x) > half + .001 ? (relative.x > 0 ? Direction.EAST : Direction.WEST)
                : Math.abs(relative.y) > half + .001 ? (relative.y > 0 ? Direction.UP : Direction.DOWN)
                : Math.abs(relative.z) > half + .001 ? (relative.z > 0 ? Direction.SOUTH : Direction.NORTH) : null;
        if (tube instanceof BufferEntity && port == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (port != null) {
                if (tube instanceof BufferEntity buffer && player.isShiftKeyDown()) buffer.choke(port);
                else tube.toggle(port);
            } else {
                // Clicking the body rotates the tube's output/closed face to a connected side.
                int current = tube.facing().ordinal();
                for (int i = 1; i <= 6; i++) {
                    Direction next = Direction.values()[(current + i) % 6];
                    var neighbor = tube.neighbour(next.getOpposite());
                    if (neighbor == null || !neighbor.connects(next)) continue;
                    level.setBlock(pos, tube.getBlockState().setValue(TubeBlock.FACING, next), 3);
                    tube.sync();
                    break;
                }
            }
            boolean choke = tube instanceof BufferEntity && player.isShiftKeyDown();
            level.playSound(null, pos, PortSounds.get(choke ? "squeek" : "tool"), SoundSource.BLOCKS,
                    choke ? .6f : .5f, (choke ? 2 : .9f) + level.random.nextFloat() * .2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
