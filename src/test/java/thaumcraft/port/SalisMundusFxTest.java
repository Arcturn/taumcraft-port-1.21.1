package thaumcraft.port;

import java.util.ArrayList;
import java.util.List;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.port.network.SalisMundusFxPayload;
import static org.junit.jupiter.api.Assertions.*;

class SalisMundusFxTest {
    @Test void roundTripPreservesOffhandExactClickAndMultiblockOrder() {
        BlockPos base = new BlockPos(-12, 100, 99);
        var value = new SalisMundusFxPayload(SalisMundusFxPayload.START, 73, InteractionHand.OFF_HAND,
                base.above(), new Vec3(-11.8, 101.2, 100), List.of(base, base.above()));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            SalisMundusFxPayload.STREAM_CODEC.encode(buffer, value);
            var decoded = SalisMundusFxPayload.STREAM_CODEC.decode(buffer);
            assertEquals(value, decoded);
            assertTrue(decoded.valid());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void invalidEffectsCannotAllocateUnboundedClientParticles() {
        BlockPos target = BlockPos.ZERO;
        assertFalse(new SalisMundusFxPayload(9, 1, InteractionHand.MAIN_HAND, target, Vec3.ZERO, List.of(target)).valid());
        assertFalse(new SalisMundusFxPayload(0, 1, InteractionHand.MAIN_HAND, target, new Vec3(Double.NaN, 0, 0), List.of(target)).valid());
        assertFalse(new SalisMundusFxPayload(0, 1, InteractionHand.MAIN_HAND, target, Vec3.ZERO, List.of(target.offset(40, 0, 0))).valid());
        assertFalse(new SalisMundusFxPayload(0, 1, InteractionHand.MAIN_HAND, target, Vec3.ZERO, List.of()).valid());
        var blocks = new ArrayList<BlockPos>();
        for (int i = 0; i < 17; i++) blocks.add(target);
        assertFalse(new SalisMundusFxPayload(0, 1, InteractionHand.MAIN_HAND, target, Vec3.ZERO, blocks).valid());
    }

    @Test void decoderRejectsOversizedCountBeforeReadingPositions() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeByte(0); buffer.writeVarInt(1); buffer.writeEnum(InteractionHand.MAIN_HAND);
            buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeDouble(0); buffer.writeDouble(0); buffer.writeDouble(0);
            buffer.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> SalisMundusFxPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void sourceListCannotMutateAnAlreadyCreatedEffect() {
        var blocks = new ArrayList<>(List.of(BlockPos.ZERO));
        var value = new SalisMundusFxPayload(1, 1, InteractionHand.MAIN_HAND, BlockPos.ZERO, Vec3.ZERO, blocks);
        blocks.clear();
        assertEquals(List.of(BlockPos.ZERO), value.blocks());
        assertThrows(UnsupportedOperationException.class, () -> value.blocks().clear());
    }
}
