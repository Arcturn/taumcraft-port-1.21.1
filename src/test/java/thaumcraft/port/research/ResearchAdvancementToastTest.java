package thaumcraft.port.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Original PacketSyncKnowledge only schedules ResearchToast, never a research advancement toast. */
class ResearchAdvancementToastTest {
    private static JsonObject audit() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of(
                "verification/development-thaumonomicon-parity/research-toast-advancement-audit.json"))).getAsJsonObject();
    }

    @Test void generatedResearchLeavesKeepTheirContentAndProgressWithoutDuplicatingTheOriginalToast() throws Exception {
        var baseline = audit().getAsJsonObject("leaves");
        assertEquals(148, baseline.size());
        var researchKeys = new HashSet<String>();
        for (var entry : baseline.entrySet()) {
            var data = ResearchCatalog.read("/" + entry.getKey());
            var expected = entry.getValue().getAsJsonObject();
            var display = data.getAsJsonObject("display");
            assertTrue(display.has("show_toast"), "An omitted value defaults to another vanilla toast");
            assertFalse(display.get("show_toast").getAsBoolean(), entry.getKey());
            var criterion = data.getAsJsonObject("criteria").getAsJsonObject("completed");
            assertEquals("thaumcraft:research", criterion.get("trigger").getAsString());
            String research = criterion.getAsJsonObject("conditions").get("research").getAsString();
            assertEquals(expected.get("research").getAsString(), research);
            assertTrue(researchKeys.add(research));
            var preserved = data.deepCopy();
            preserved.getAsJsonObject("display").remove("show_toast");
            assertEquals(expected.get("preserved_content_sha256").getAsString(), digest(preserved),
                    "Only show_toast may change; preserve the existing icon, criteria, parent and node: " + entry.getKey());
        }
        assertEquals(ResearchCatalog.all().stream().map(ResearchCatalog.Entry::key).collect(Collectors.toSet()), researchKeys);

        var state = new ResearchState();
        state.stage("KNOWLEDGETYPES", ResearchCatalog.get("KNOWLEDGETYPES").stages().size() + 1);
        assertTrue(state.popupResearch().contains("KNOWLEDGETYPES"), "The original ResearchToast must remain available");
        assertTrue(ResearchMilestones.matches(state, "KNOWLEDGETYPES", ""), "The advancement still earns progress");
        state.clearPopups();
        assertTrue(state.popupResearch().isEmpty());
        assertTrue(ResearchMilestones.matches(state, "KNOWLEDGETYPES", ""), "Consuming a toast does not revoke the node");
    }

    @Test void theChangeDoesNotMuteOrRewriteAnyOtherAdvancement() throws Exception {
        var unchanged = audit().getAsJsonObject("untouched_advancements");
        assertEquals(8, unchanged.size(), "The existing root and seven category nodes are outside this change");
        for (var entry : unchanged.entrySet()) {
            var current = ResearchCatalog.read("/" + entry.getKey());
            assertEquals(entry.getValue().getAsJsonObject().get("content_sha256").getAsString(), digest(current),
                    "Nonresearch advancements must keep their own toast policy and content: " + entry.getKey());
        }
        // No assertion forces unrelated or future advancements to hide their vanilla toast.
    }

    private static String digest(JsonElement value) throws NoSuchAlgorithmException {
        byte[] bytes = canonical(value).toString().getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static JsonElement canonical(JsonElement value) {
        if (value.isJsonObject()) {
            var sorted = new JsonObject();
            for (String key : new TreeSet<>(value.getAsJsonObject().keySet()))
                sorted.add(key, canonical(value.getAsJsonObject().get(key)));
            return sorted;
        }
        if (value.isJsonArray()) {
            var sorted = new JsonArray();
            for (var member : value.getAsJsonArray()) sorted.add(canonical(member));
            return sorted;
        }
        return value.deepCopy();
    }
}
