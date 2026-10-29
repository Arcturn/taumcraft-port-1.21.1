package thaumcraft.port.research;

import com.google.gson.JsonObject;
import java.util.*;

/** Exact ScanEnchantment/ScanPotion keys; imported from the original Minecraft language assets. */
public final class LegacyScanKeys {
    private static final JsonObject KEYS = ResearchCatalog.read("/thaumcraft-port/scan-keys.json");
    private static final Map<String, String> ALIASES = aliases();

    public static String enchantment(String id, String translationKey) { return key("enchantments", id, translationKey); }
    public static String effect(String id, String translationKey) { return key("effects", id, translationKey); }

    private static String key(String family, String id, String fallback) {
        var entries = KEYS.getAsJsonObject(family);
        return "!" + (entries.has(id) ? entries.get(id).getAsString() : fallback);
    }

    private static Map<String, String> aliases() {
        var result = new LinkedHashMap<String, String>();
        KEYS.getAsJsonObject("enchantments").entrySet().forEach(e ->
                result.put("!enchantment." + e.getKey().substring("minecraft:".length()), "!" + e.getValue().getAsString()));
        KEYS.getAsJsonObject("effects").entrySet().forEach(e -> {
            String canonical = "!" + e.getValue().getAsString();
            result.put("!potion." + e.getKey().substring("minecraft:".length()), canonical);
            result.put("!potion." + e.getValue().getAsString().substring("effect.".length()), canonical);
        });
        return Collections.unmodifiableMap(result);
    }

    public static void migrate(ResearchState state) {
        for (String previous : List.copyOf(state.flags())) {
            String canonical = ALIASES.get(previous);
            if (canonical != null) state.flag(canonical);
        }
    }

    private LegacyScanKeys() {}
}
