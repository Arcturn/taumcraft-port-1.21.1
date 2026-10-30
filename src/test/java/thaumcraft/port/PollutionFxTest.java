package thaumcraft.port;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.port.network.PollutionFxPayload;
import static org.junit.jupiter.api.Assertions.*;

class PollutionFxTest {
    @Test void fractionalAspectLeaksRemainVisibleAndCleanupScalesWithAspectCount() {
        assertEquals(0, PollutionFxPayload.particleCount(0));
        assertEquals(1, PollutionFxPayload.particleCount(.25f));
        assertEquals(1, PollutionFxPayload.particleCount(1));
        assertEquals(3, PollutionFxPayload.particleCount(15 * .25f));
        assertEquals(40, PollutionFxPayload.particleCount(160 * .25f));
        assertEquals(40, PollutionFxPayload.particleCount(600 * .25f));
        assertEquals(40, PollutionFxPayload.particleCount(Float.MAX_VALUE));
    }

    @Test void invalidPollutionDoesNotCreateSmoke() {
        for (float amount : new float[]{-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
            assertEquals(0, PollutionFxPayload.particleCount(amount));
    }

    @Test void packetPreservesPositionAndSmokeCountWithoutWrapping() {
        var value = new PollutionFxPayload(new BlockPos(-12, 100, 99), 40);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PollutionFxPayload.STREAM_CODEC.encode(buffer, value);
            assertEquals(value, PollutionFxPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void packetRejectsOversizedBurstsAndKeepsAnImmutablePosition() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeBlockPos(BlockPos.ZERO); buffer.writeByte(255);
            assertThrows(IllegalArgumentException.class, () -> PollutionFxPayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        var pos = new BlockPos.MutableBlockPos(1, 2, 3);
        var value = new PollutionFxPayload(pos, 1);
        pos.set(4, 5, 6);
        assertEquals(new BlockPos(1, 2, 3), value.pos());
    }
}
