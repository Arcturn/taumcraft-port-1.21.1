package thaumcraft.port;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class ModernAspectBonusesTest {
    @Test void windAndSoulEnchantmentsRetainOriginalSplitRounding() {
        var values = new LinkedHashMap<Aspect, Integer>();
        ModernAspectBonuses.enchantment("minecraft:wind_burst", 3, values);
        assertEquals(Map.of(Aspect.AIR, 1, Aspect.MOTION, 1), values);
        ModernAspectBonuses.enchantment("minecraft:soul_speed", 9, values);
        assertEquals(Map.of(Aspect.AIR, 1, Aspect.MOTION, 4, Aspect.SOUL, 4), values);
    }
    @Test void severalBonusesMergeByMaximumWithoutInflatingTheBase() {
        var values = new LinkedHashMap<>(Map.of(Aspect.AVERSION, 24));
        ModernAspectBonuses.enchantment("minecraft:piercing", 12, values);
        ModernAspectBonuses.enchantment("minecraft:breach", 12, values);
        assertEquals(Map.of(Aspect.AVERSION, 24, Aspect.ENTROPY, 6), values);
    }
    @Test void extensionDoesNotReapplyOriginalOrInventThirdPartyEnchantments() {
        var values = new LinkedHashMap<Aspect, Integer>();
        ModernAspectBonuses.enchantment("minecraft:sharpness", 15, values);
        ModernAspectBonuses.enchantment("example:wind_burst", 9, values);
        assertTrue(values.isEmpty());
    }
    @Test void distinctModernRolesRemainDistinct() {
        var values = new LinkedHashMap<Aspect, Integer>();
        ModernAspectBonuses.enchantment("minecraft:channeling", 3, values);
        ModernAspectBonuses.enchantment("minecraft:loyalty", 9, values);
        ModernAspectBonuses.enchantment("minecraft:swift_sneak", 9, values);
        assertEquals(Map.of(Aspect.ENERGY, 3, Aspect.TRAP, 9, Aspect.MOTION, 9), values);
    }
}
