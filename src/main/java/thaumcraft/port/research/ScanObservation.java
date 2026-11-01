package thaumcraft.port.research;

import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** A read-only scan result shared by server reward handling and client highlighting. */
public record ScanObservation(String subject, Map<Aspect, Integer> aspects, Set<String> discoveries,
                              Set<Aspect> aspectDiscoveries) {
    public ScanObservation(String subject, Map<Aspect, Integer> aspects, Set<String> discoveries) {
        this(subject, aspects, discoveries, aspects.keySet());
    }

    public ScanObservation {
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
        discoveries = Collections.unmodifiableSet(new LinkedHashSet<>(discoveries));
        aspectDiscoveries = Collections.unmodifiableSet(new LinkedHashSet<>(aspectDiscoveries));
    }

    public boolean canLearn(ResearchState state) {
        if (!subject.isBlank() && !aspects.isEmpty() && !state.scanned().contains(subject)) return true;
        if (aspectDiscoveries.stream().anyMatch(aspect -> !state.discovered().contains(aspect.id()))) return true;
        return discoveries.stream().anyMatch(key -> !state.knows(key));
    }

    public boolean learn(ResearchState state) {
        return learn(state,state::flag);
    }

    /** The server supplies ResearchManager's progression effects for catalog discoveries. */
    public boolean learn(ResearchState state, java.util.function.Predicate<String> progressDiscovery) {
        boolean changed = state.discoverAspects(aspectDiscoveries);
        if (!aspects.isEmpty() && state.recordScanSubject(subject)) {
            for (String category : ResearchCatalog.CATEGORIES)
                state.addKnowledge("OBSERVATION", category, ResearchCatalog.observation(category, aspects));
            changed = true;
        }
        for (String key : discoveries) if (!state.knows(key)) changed |= progressDiscovery.test(key);
        return changed;
    }
}
