package thaumcraft.port.aspect;

import java.util.Map;
import net.minecraft.world.item.*;

/** Deliberate 1.13–1.21.1 extensions; original equipment/enchantment rules stay unchanged. */
public final class ModernAspectBonuses {
    public static void item(ItemStack stack, Map<Aspect, Integer> values) {
        var item = stack.getItem();
        if (item instanceof CrossbowItem) {
            merge(values, Aspect.AVERSION, 10);
            merge(values, Aspect.FLIGHT, 5);
            merge(values, Aspect.MECHANISM, 5);
        } else if (item instanceof TridentItem) {
            merge(values, Aspect.AVERSION, 24);
            merge(values, Aspect.FLIGHT, 5);
        } else if (item instanceof MaceItem) {
            merge(values, Aspect.AVERSION, 24);
        } else if (item instanceof BrushItem) {
            merge(values, Aspect.TOOL, 4);
        }
        if (stack.is(Items.SPYGLASS)) merge(values, Aspect.SENSES, 10);
    }

    /** level is already multiplied by three, exactly as in original getBonusTags. */
    public static void enchantment(String id, int level, Map<Aspect, Integer> values) {
        switch (id) {
            case "minecraft:loyalty" -> merge(values, Aspect.TRAP, level);
            case "minecraft:impaling" -> split(values, Aspect.WATER, Aspect.AVERSION, level);
            case "minecraft:riptide" -> split(values, Aspect.WATER, Aspect.MOTION, level);
            case "minecraft:channeling" -> merge(values, Aspect.ENERGY, level);
            case "minecraft:piercing" -> merge(values, Aspect.AVERSION, level);
            case "minecraft:multishot" -> split(values, Aspect.FLIGHT, Aspect.AVERSION, level);
            case "minecraft:quick_charge", "minecraft:swift_sneak" -> merge(values, Aspect.MOTION, level);
            case "minecraft:soul_speed" -> split(values, Aspect.SOUL, Aspect.MOTION, level);
            case "minecraft:density" -> split(values, Aspect.EARTH, Aspect.AVERSION, level);
            case "minecraft:breach" -> split(values, Aspect.ENTROPY, Aspect.AVERSION, level);
            case "minecraft:wind_burst" -> split(values, Aspect.AIR, Aspect.MOTION, level);
        }
    }

    private static void merge(Map<Aspect, Integer> values, Aspect aspect, int amount) {
        AspectMath.merge(values, aspect, amount);
    }
    private static void split(Map<Aspect, Integer> values, Aspect a, Aspect b, int amount) {
        merge(values, a, amount / 2);
        merge(values, b, amount / 2);
    }
    private ModernAspectBonuses() {}
}
