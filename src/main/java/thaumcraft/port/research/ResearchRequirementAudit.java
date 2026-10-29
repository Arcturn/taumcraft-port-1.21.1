package thaumcraft.port.research;

import java.util.ArrayList;
import java.util.List;

/** Read-only audit of requirement translation, not a claim of survival availability. */
public final class ResearchRequirementAudit {
    public record Requirement(String research, int stage, String field, String source, String craftKey, boolean supported) {}

    public static List<Requirement> collect() {
        List<Requirement> result = new ArrayList<>();
        for (var entry : ResearchCatalog.all()) {
            for (int stage = 0; stage < entry.stages().size(); stage++) {
                for (String field : List.of("required_item", "required_craft")) {
                    for (String source : ResearchCatalog.strings(entry.stages().get(stage), field)) {
                        var parsed = LegacyRequirement.parse(source);
                        result.add(new Requirement(entry.key(), stage + 1, field, source, parsed.craftKey(),
                                !parsed.craftKey().startsWith("unsupported:")));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private ResearchRequirementAudit() {}
}
