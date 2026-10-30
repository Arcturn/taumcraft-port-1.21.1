package thaumcraft.port.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.network.PollutionFxPayload;
import thaumcraft.port.world.AuraSystem;

/** Exercises the complete crucible -> aura -> nearby-player smoke packet path. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CruciblePollutionGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void timedOverflowAndSimultaneousLeaksSendSeparateNearbySmokePackets(GameTestHelper helper) {
        var be = fixture(helper);
        try (var near = new Observer(helper, be.getBlockPos(), 2);
             var far = new Observer(helper, be.getBlockPos(), 40);
             var otherDimension = new Observer(helper, be.getBlockPos(), 2)) {
            var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Fixture has no alternate dimension");
            otherDimension.player.setServerLevel(nether);
            // A timed leak still happens while cold and dry; a quarter point remains visible.
            assertLeak(helper, be, near, 1, 99, 1, 0);
            // Overflow runs before the next timer deadline.
            assertLeak(helper, be, near, 501, -100, 1, 500);
            // These are independent original triggers, not a single combined two-particle packet.
            assertLeak(helper, be, near, 502, 99, 2, 500);
            helper.assertTrue(far.connection.smoke.isEmpty(), "Crucible smoke escaped its 32-block broadcast radius");
            helper.assertTrue(otherDimension.connection.smoke.isEmpty(), "Crucible smoke crossed into another dimension");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void cleanupSmokeUsesBaseAspectCountWithoutTheExtraVitiumPollution(GameTestHelper helper) {
        var be = fixture(helper);
        try (var near = new Observer(helper, be.getBlockPos(), 2)) {
            // Sixteen aspects produce four smoke particles but seven pollution with Vitium.
            assertCleanup(helper, be, near, Map.of(Aspect.FIRE, 12, Aspect.FLUX, 4), 4, 7);
            // All-Vitium cleanup must remain one capped visual burst, despite its extra flux.
            assertCleanup(helper, be, near, Map.of(Aspect.FLUX, 200), 40, 200);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void clearingOnlyWaterDoesNotSendPollutionSmoke(GameTestHelper helper) {
        var be = fixture(helper);
        be.state.restore(1000, 200, -100, Map.of());
        var aura = AuraSystem.get(helper.getLevel(), be.getBlockPos());
        helper.assertTrue(aura != null, "Fixture has no aura");
        float before = aura.flux();
        try (var near = new Observer(helper, be.getBlockPos(), 2)) {
            be.empty();
            helper.assertValueEqual(be.state.water(), 0, "Clean water was not emptied");
            helper.assertValueEqual(be.state.heat(), 200, "Emptying reset the crucible's retained heat");
            helper.assertTrue(near.connection.smoke.isEmpty(), "Clean water emitted a pollution smoke packet");
            helper.assertValueEqual(aura.flux(), before, "Clean water increased aura pollution");
            be.empty();
            helper.assertTrue(near.connection.smoke.isEmpty(), "Repeated emptying emitted a pollution packet");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void breakingFilledCrucibleSendsSmokeEvenThoughItsBlockEntityIsRemoved(GameTestHelper helper) {
        var be = fixture(helper);
        var pos = be.getBlockPos();
        be.state.restore(1000, 200, -100, Map.of(Aspect.FIRE, 16, Aspect.FLUX, 4));
        var aura = AuraSystem.get(helper.getLevel(), pos);
        helper.assertTrue(aura != null, "Fixture has no aura");
        float before = aura.flux();
        try (var near = new Observer(helper, pos, 2)) {
            helper.getLevel().removeBlock(pos, false);
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null, "Broken crucible retained its block entity");
            assertPackets(helper, near, pos, 1, 5);
            helper.assertTrue(Math.abs(aura.flux() - before - 8) < .001f,
                    "Breaking the crucible lost its base or extra Vitium pollution");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void cleanupStillDispatchesTheOriginalSpillSoundAndBubbleBlockEvent(GameTestHelper helper) {
        var be = fixture(helper);
        be.state.restore(1000, 200, -100, Map.of(Aspect.FIRE, 4));
        var near = new Observer(helper, be.getBlockPos(), 2);
        try {
            be.empty();
            assertPackets(helper, near, be.getBlockPos(), 1, 1);
        } catch (RuntimeException | Error failure) {
            near.close();
            throw failure;
        }
        // Unlike custom smoke, vanilla block events are queued until the server ticks them.
        helper.runAfterDelay(2, () -> {
            try (near) {
                helper.assertValueEqual(near.connection.events.size(), 1, "Cleanup lost or duplicated its queued spill event");
                var event = near.connection.events.getFirst();
                helper.assertValueEqual(event.getB0(), 2, "Cleanup changed the original spill event id");
                helper.assertValueEqual(event.getB1(), 5, "Cleanup changed its five-fold bubble/spill effect");
                helper.assertTrue(event.getBlock() == MachineContent.CRUCIBLE.get(), "Spill event targeted another block");
                assertPackets(helper, near, be.getBlockPos(), 1, 1);
                helper.succeed();
            }
        });
    }

    private static void assertLeak(GameTestHelper helper, CrucibleEntity be, Observer observer,
                                   int stored, int timer, int expectedPackets, int remaining) {
        be.state.restore(0, 0, timer, Map.of(Aspect.FIRE, stored));
        observer.connection.smoke.clear();
        var aura = AuraSystem.get(helper.getLevel(), be.getBlockPos());
        helper.assertTrue(aura != null, "Fixture has no aura");
        float before = aura.flux();
        CrucibleEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
        helper.assertValueEqual(be.state.total(), remaining, "Leak removed the wrong number of aspects");
        assertPackets(helper, observer, be.getBlockPos(), expectedPackets, 1);
        helper.assertTrue(Math.abs(aura.flux() - before - expectedPackets * .25f) < .001f,
                "Smoke dispatch changed the actual pollution amount");
    }

    private static void assertCleanup(GameTestHelper helper, CrucibleEntity be, Observer observer,
                                      Map<Aspect, Integer> contents, int particles, float pollution) {
        be.state.restore(1000, 200, -100, contents);
        observer.connection.smoke.clear();
        var aura = AuraSystem.get(helper.getLevel(), be.getBlockPos());
        helper.assertTrue(aura != null, "Fixture has no aura");
        float before = aura.flux();
        be.empty();
        helper.assertValueEqual(be.state.total(), 0, "Cleanup left aspects behind");
        helper.assertValueEqual(be.state.water(), 0, "Cleanup left water behind");
        assertPackets(helper, observer, be.getBlockPos(), 1, particles);
        helper.assertTrue(Math.abs(aura.flux() - before - pollution) < .001f,
                "Cleanup lost or duplicated the separate Vitium pollution bonus");
    }

    private static void assertPackets(GameTestHelper helper, Observer observer, BlockPos pos, int count, int particles) {
        helper.assertValueEqual(observer.connection.smoke.size(), count, "Wrong number of actual outgoing smoke packets");
        for (var payload : observer.connection.smoke) {
            helper.assertValueEqual(payload.pos(), pos, "Smoke packet points to the wrong crucible");
            helper.assertValueEqual(payload.amount(), particles, "Smoke packet has the wrong particle count");
        }
    }

    private static CrucibleEntity fixture(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(relative, MachineContent.CRUCIBLE.get());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(be instanceof CrucibleEntity, "Fixture has no crucible block entity");
        return (CrucibleEntity) be;
    }

    private static final class CaptureConnection extends ServerGamePacketListenerImpl {
        final List<PollutionFxPayload> smoke = new ArrayList<>();
        final List<ClientboundBlockEventPacket> events = new ArrayList<>();
        private final BlockPos source;
        CaptureConnection(ServerPlayer player, BlockPos source) {
            super(player.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            this.source = source.immutable();
        }
        @Override public void send(Packet<?> packet) {
            if (packet instanceof ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof PollutionFxPayload pollution && pollution.pos().equals(source)) smoke.add(pollution);
            if (packet instanceof ClientboundBlockEventPacket event && event.getPos().equals(source)) events.add(event);
        }
    }

    /** Broadcast needs logged-in recipients; register only for the bounded assertion scope. */
    private static final class Observer implements AutoCloseable {
        final ServerPlayer player;
        final CaptureConnection connection;
        private final List<ServerPlayer> recipients;
        Observer(GameTestHelper helper, BlockPos pos, int distance) {
            player = GameTestPlayers.create(helper);
            player.setPos(pos.getX() + distance, pos.getY(), pos.getZ());
            connection = new CaptureConnection(player, pos);
            helper.assertTrue(player.connection == connection, "Packet listener constructor did not install the capture connection");
            try {
                // PlayerList.getPlayers() is an unmodifiable view; avoid a real login/network channel.
                var field = PlayerList.class.getDeclaredField("players");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                var players = (List<ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
                recipients = players;
                recipients.add(player);
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError("Cannot register test-only packet observer", failure);
            }
        }
        @Override public void close() { recipients.remove(player); }
    }
}
