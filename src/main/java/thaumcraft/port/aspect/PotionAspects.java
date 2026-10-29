package thaumcraft.port.aspect;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/** ConfigAspects.getPotionAspects, using the active world's potion-conversion graph. */
public final class PotionAspects {
    public static boolean handles(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION) || stack.is(Items.TIPPED_ARROW);
    }

    public static Map<Aspect, Integer> base(ItemStack stack, PotionBrewing brewing,
                                            Function<ItemStack, Map<Aspect, Integer>> resolver) {
        var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        var values = new LinkedHashMap<Aspect, Integer>();
        if (potion.isPresent() && potion.get().is(Potions.WATER)) values.put(Aspect.WATER, 5);
        else {
            // TC6 used ItemStack identity in a HashSet, not ingredient/item-id equality.
            var reagents = new ArrayList<ItemStack>();
            var seenReagents = Collections.newSetFromMap(new IdentityHashMap<ItemStack, Boolean>());
            var visited = new HashSet<Holder<Potion>>();
            var current = potion.orElse(null);
            while (current != null && visited.size() < 100 && visited.add(current)) {
                PotionBrewing.Mix<Potion> found = null;
                for (var mix : brewing.potionMixes) if (mix.to().is(current)) { found = mix; break; }
                if (found == null) break;
                var options = found.ingredient().getItems();
                if (options.length > 0 && seenReagents.add(options[0])) reagents.add(options[0]);
                current = found.from().is(Potions.WATER) ? null : found.from();
            }
            if (reagents.isEmpty()) { values.put(Aspect.MAGIC, 5); values.put(Aspect.ALCHEMY, 5); }
            else {
                for (var reagent : reagents) {
                    resolver.apply(reagent).forEach((aspect, amount) -> values.merge(aspect, amount, Integer::sum));
                    values.merge(Aspect.ALCHEMY, 3, Integer::sum);
                }
                values.replaceAll((aspect, amount) -> amount - (int) (amount * .66));
            }
        }
        values.merge(stack.is(Items.POTION) ? Aspect.WATER : stack.is(Items.SPLASH_POTION)
                ? Aspect.AVERSION : stack.is(Items.LINGERING_POTION) ? Aspect.ENERGY : Aspect.TRAP, 5, Integer::sum);
        return values;
    }

    private PotionAspects() {}
}
