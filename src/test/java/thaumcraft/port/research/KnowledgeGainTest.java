package thaumcraft.port.research;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.network.KnowledgeGainPayload;
import thaumcraft.port.research.ResearchState.KnowledgeGain;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for TC6's full-point feedback, independent of journal balances. */
class KnowledgeGainTest {
    @Test void partialObservationsAccumulateSilentlyUntilTheSixteenthUnit() {
        var state = new ResearchState();
        assertTrue(state.addKnowledge("OBSERVATION", "BASICS", 15));
        assertTrue(state.drainKnowledgeGains().isEmpty());
        assertTrue(state.addKnowledge("OBSERVATION", "BASICS", 1));
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "BASICS", 1)), state.drainKnowledgeGains());
        assertTrue(state.drainKnowledgeGains().isEmpty(), "A second sync must not replay the gain");
        assertEquals(16, state.knowledge("OBSERVATION", "BASICS"));
    }

    @Test void theoryUsesThirtyTwoUnitsAndEachCrossedPointGetsAnEffect() {
        var state = new ResearchState();
        state.addKnowledge("THEORY", "ALCHEMY", 31);
        assertTrue(state.drainKnowledgeGains().isEmpty());
        state.addKnowledge("THEORY", "ALCHEMY", 65);
        assertEquals(List.of(new KnowledgeGain("THEORY", "ALCHEMY", 3)), state.drainKnowledgeGains());
        assertEquals(96, state.knowledge("THEORY", "ALCHEMY"));
    }

    @Test void remainderCarriesAcrossAwardsAndKnowledgeTypesAndCategoriesStayIndependent() {
        var state = new ResearchState();
        state.addKnowledge("OBSERVATION", "BASICS", 14);
        state.addKnowledge("OBSERVATION", "BASICS", 19);
        state.addKnowledge("OBSERVATION", "ALCHEMY", 16);
        state.addKnowledge("THEORY", "BASICS", 32);
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "BASICS", 2),
                new KnowledgeGain("OBSERVATION", "ALCHEMY", 1),
                new KnowledgeGain("THEORY", "BASICS", 1)), state.drainKnowledgeGains());
        state.addKnowledge("OBSERVATION", "BASICS", 14);
        assertTrue(state.drainKnowledgeGains().isEmpty());
        state.addKnowledge("OBSERVATION", "BASICS", 1);
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "BASICS", 1)), state.drainKnowledgeGains());
    }

    @Test void spendingAndRejectedChangesNeverInventGainEvents() {
        var state = new ResearchState();
        state.restoreKnowledge("OBSERVATION", "BASICS", 32);
        assertTrue(state.addKnowledge("OBSERVATION", "BASICS", -17));
        assertTrue(state.addKnowledge("OBSERVATION", "BASICS", 0));
        assertFalse(state.addKnowledge("OBSERVATION", "BASICS", -16));
        assertFalse(state.addKnowledge("OBSERVATION", "BASICS", 1_000_000));
        assertFalse(state.addKnowledge("UNKNOWN", "BASICS", 16));
        assertFalse(state.addKnowledge("OBSERVATION", "UNKNOWN", 16));
        assertEquals(15, state.knowledge("OBSERVATION", "BASICS"));
        assertTrue(state.drainKnowledgeGains().isEmpty());
        state.addKnowledge("OBSERVATION", "BASICS", 1);
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "BASICS", 1)), state.drainKnowledgeGains());
    }

    @Test void aNewAspectAndGenericScanCanBothCompleteObservationPointsWithoutDuplicateRewards() {
        var state = new ResearchState();
        state.restoreKnowledge("OBSERVATION", "BASICS", 15);
        state.restoreKnowledge("OBSERVATION", "AUROMANCY", 12);
        state.restoreKnowledge("OBSERVATION", "ALCHEMY", 15);
        var observation = new ScanObservation("item:test:air_sample", Map.of(Aspect.AIR, 10), Set.of());
        assertTrue(observation.learn(state));
        // First Aer adds one to all three categories; the generic formula adds three
        // to BASICS and AUROMANCY, crossing AUROMANCY's threshold independently.
        assertEquals(19, state.knowledge("OBSERVATION", "BASICS"));
        assertEquals(16, state.knowledge("OBSERVATION", "AUROMANCY"));
        assertEquals(16, state.knowledge("OBSERVATION", "ALCHEMY"));
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "BASICS", 1),
                new KnowledgeGain("OBSERVATION", "ALCHEMY", 1),
                new KnowledgeGain("OBSERVATION", "AUROMANCY", 1)), state.drainKnowledgeGains());
        var balance = Map.copyOf(state.knowledge());
        assertFalse(observation.learn(state));
        assertEquals(balance, state.knowledge());
        assertTrue(state.drainKnowledgeGains().isEmpty());
    }

    @Test void successfulSubthresholdAndDiscoveryOnlyScansDoNotPlayFullPointFeedback() {
        var state = new ResearchState();
        assertTrue(new ScanObservation("item:test:small_sample", Map.of(Aspect.AIR, 10), Set.of()).learn(state));
        assertTrue(new ScanObservation("", Map.of(), Set.of("f_arrow")).learn(state));
        assertEquals(4, state.knowledge("OBSERVATION", "BASICS"));
        assertTrue(state.drainKnowledgeGains().isEmpty());
    }

    @Test void savingAndJournalSnapshotsPreserveTheBalanceButNeverReplayOldRewards() {
        var storage = new ResearchStorage();
        var state = storage.player(UUID.randomUUID());
        state.addKnowledge("OBSERVATION", "INFUSION", 35);
        state.addKnowledge("THEORY", "ELDRITCH", 66);
        var saved = ResearchCodec.write(state);
        var firstSync = ResearchProgress.snapshot(storage, state);
        var secondSync = ResearchProgress.snapshot(storage, state);
        assertEquals(saved.getCompound("knowledge"), firstSync.getCompound("knowledge"));
        assertEquals(firstSync, secondSync);
        for (var tag : List.of(saved, firstSync, secondSync)) {
            var loaded = ResearchCodec.read(tag);
            assertEquals(state.knowledge(), loaded.knowledge());
            assertTrue(loaded.drainKnowledgeGains().isEmpty(), "Loading saved or synchronized balances isn't a new award");
        }
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "INFUSION", 2),
                new KnowledgeGain("THEORY", "ELDRITCH", 2)), state.drainKnowledgeGains(),
                "Writing a snapshot must not consume undelivered reward events");
        assertTrue(state.drainKnowledgeGains().isEmpty());
    }

    @Test void anAwardAfterLoadUsesTheRestoredRemainderRatherThanReplayingItsFullBalance() {
        var state = new ResearchState();
        state.addKnowledge("OBSERVATION", "ARTIFICE", 47);
        var loaded = ResearchCodec.read(ResearchCodec.write(state));
        assertTrue(loaded.drainKnowledgeGains().isEmpty());
        loaded.addKnowledge("OBSERVATION", "ARTIFICE", 1);
        assertEquals(List.of(new KnowledgeGain("OBSERVATION", "ARTIFICE", 1)), loaded.drainKnowledgeGains());
    }

    @Test void gainPayloadRoundTripsTypeCategoryAndFullPointCount() {
        for (String type : List.of("OBSERVATION", "THEORY")) {
            var value = new KnowledgeGainPayload(type, "GOLEMANCY", 7);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                KnowledgeGainPayload.STREAM_CODEC.encode(buffer, value);
                assertEquals(value, KnowledgeGainPayload.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally { buffer.release(); }
        }
    }

    @Test void invalidFeedbackCannotCreateAnUnknownOrEmptyAnimation() {
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("UNKNOWN", "BASICS", 1));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("OBSERVATION", "UNKNOWN", 1));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("OBSERVATION", "BASICS", 0));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("THEORY", "BASICS", -1));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("OBSERVATION", "BASICS", 62_501));
        assertThrows(IllegalArgumentException.class, () -> new KnowledgeGainPayload("THEORY", "BASICS", 31_251));
    }
}
