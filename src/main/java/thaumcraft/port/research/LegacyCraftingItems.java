package thaumcraft.port.research;

import java.util.Map;

/** Shared identities for TC6 metadata items now registered as separate items. */
public final class LegacyCraftingItems {
    private static final Map<Integer, String> SEALS = Map.ofEntries(
            Map.entry(0, "blank"), Map.entry(1, "pickup"), Map.entry(2, "pickup_advanced"),
            Map.entry(3, "fill"), Map.entry(4, "fill_advanced"), Map.entry(5, "empty"),
            Map.entry(6, "empty_advanced"), Map.entry(9, "guard"), Map.entry(10, "guard_advanced"),
            Map.entry(11, "lumber"), Map.entry(12, "breaker"), Map.entry(13, "use"),
            Map.entry(14, "provider"), Map.entry(15, "stock"), Map.entry(16, "breaker_advanced"));

    public static String normalize(String source) {
        var parts = source.split(";", 4);
        int meta;
        try { meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0; }
        catch (NumberFormatException ignored) { return source; }
        String item = switch (parts[0]) {
            case "thaumcraft:mind" -> meta == 0 ? "mind_clockwork" : meta == 1 ? "mind_biothaumic" : null;
            case "thaumcraft:module" -> meta == 0 ? "module_vision" : meta == 1 ? "module_aggression" : null;
            case "thaumcraft:turret" -> meta == 0 ? "turret_placer_basic" : meta == 1 ? "turret_placer_advanced" : null;
            case "thaumcraft:seal" -> SEALS.containsKey(meta) ? "seal_" + SEALS.get(meta) : null;
            // The captured book also uses a seal type token, rather than an NBT compound.
            case "thaumcraft:seals" -> parts.length == 4 && parts[3].startsWith("thaumcraft:")
                    && SEALS.containsValue(parts[3].substring(11)) ? "seal_" + parts[3].substring(11) : null;
            default -> null;
        };
        if (item == null) return source;
        return "thaumcraft:" + item + ";" + (parts.length > 1 ? parts[1] : "1") + ";0"
                + (parts.length == 4 && !parts[0].equals("thaumcraft:seals") ? ";" + parts[3] : "");
    }

    private LegacyCraftingItems() {}
}
