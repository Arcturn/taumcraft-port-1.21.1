package thaumcraft.port.research;

import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** A read-only scan result shared by server reward handling and client highlighting. */
public record ScanObservation(String subject, Map<Aspect, Integer> aspects, Set<String> discoveries) {
    public ScanObservation {
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
        discoveries = Collections.unmodifiableSet(new LinkedHashSet<>(discoveries));
    }

    public boolean canLearn(ResearchState state) {
        if (!subject.isBlank() && !aspects.isEmpty() && !state.scanned().contains(subject)) return true;
        if (aspects.keySet().stream().anyMatch(aspect -> !state.discovered().contains(aspect.id()))) return true;
        return discoveries.stream().anyMatch(key -> !state.knows(key));
    }

    public boolean learn(ResearchState state) {
        boolean changed = state.discoverAspects(aspects.keySet());
        if (state.recordScan(subject, aspects.keySet())) {
            for (String category : ResearchCatalog.CATEGORIES)
                state.addKnowledge("OBSERVATION", category, ResearchCatalog.observation(category, aspects));
            changed = true;
        }
        for (String key : discoveries) if (!state.knows(key)) changed |= state.flag(key);
        return changed;
    }
}
