package thaumcraft.port.aspect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Arithmetic from TC6 AspectList, AspectHelper and ThaumcraftCraftingManager. */
public final class AspectMath {
    public static void merge(Map<Aspect, Integer> values, Aspect aspect, int amount) {
        if (amount > 0) values.merge(aspect, amount, Math::max);
    }

    /** AspectList.reduce leaves the amount unchanged if the subtraction cannot be paid. */
    public static void reduce(Map<Aspect, Integer> values, Aspect aspect, int amount) {
        int current = values.getOrDefault(aspect, 0);
        if (current >= amount) values.put(aspect, current - amount);
    }

    public static int scaled(int total, int output) {
        if (total <= 0 || output <= 0) return 0;
        float value = total * .75f / output;
        return value < 1 && value > .75f ? 1 : (int) value;
    }

    public static Map<Aspect, Integer> finish(Map<Aspect, Integer> source) {
        var values = new LinkedHashMap<>(source);
        values.values().removeIf(n -> n <= 0);
        while (values.size() > 7) {
            Aspect lowest = null;
            float score = Float.POSITIVE_INFINITY;
            for (var entry : values.entrySet()) {
                float candidate = entry.getValue();
                if (entry.getKey().isPrimal()) candidate *= .9f;
                else for (var component : entry.getKey().components()) {
                    if (!component.isPrimal()) {
                        candidate *= 1.1f;
                        for (var child : component.components()) if (!child.isPrimal()) candidate *= 1.05f;
                    }
                }
                if (candidate < score) { score = candidate; lowest = entry.getKey(); }
            }
            values.remove(lowest);
        }
        // TC6 culls before applying the per-aspect cap, not after it.
        values.replaceAll((aspect, amount) -> Math.min(500, amount));
        return Collections.unmodifiableMap(values);
    }

    public static Map<Aspect, Integer> cap(Map<Aspect, Integer> source) {
        var values = new LinkedHashMap<>(source);
        values.replaceAll((aspect, amount) -> Math.min(500, amount));
        return values;
    }

    public static Map<Aspect, Integer> player(String name) {
        var values = new LinkedHashMap<Aspect, Integer>();
        values.put(Aspect.MAN, 4);
        var random = new Random(name.hashCode());
        var aspects = Aspect.values();
        for (int i = 0; i < 3; i++) values.merge(aspects[random.nextInt(aspects.length)], 15, Integer::sum);
        return Collections.unmodifiableMap(values);
    }

    private AspectMath() {}
}
