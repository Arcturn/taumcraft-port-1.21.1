package thaumcraft.port.aspect;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.crafting.*;
import org.slf4j.Logger;

/** TC6 recipe derivation; reload clears the per-manager caches on both logical sides. */
public final class RecipeAspects {
    private static final Map<RecipeManager, Calculator> CACHES = new WeakHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void clear() { synchronized (CACHES) { CACHES.clear(); } }

    public static Map<Aspect, Integer> get(ItemStack stack, Level level) {
        Calculator calculator;
        synchronized (CACHES) {
            calculator = CACHES.computeIfAbsent(level.getRecipeManager(), manager -> new Calculator(
                    level.registryAccess(), level.potionBrewing(), manager.getRecipes().stream()
                    .sorted(Comparator.comparing(holder -> holder.id().toString())).map(RecipeHolder::value).toList()));
        }
        return calculator.get(stack);
    }

    /** Also usable with real loaded registries/recipes in data validation, without creating a world. */
    public static final class Calculator {
        private record Key(Item item, DataComponentPatch components) {
            static Key of(ItemStack stack) { return new Key(stack.getItem(), stack.getComponentsPatch()); }
        }
        private final HolderLookup.Provider registries;
        private final PotionBrewing brewing;
        private record Candidate(Recipe<?> recipe, ItemStack output, List<Ingredient> ingredients, boolean modern) {}
        private record Selected(ItemStack stack, Map<Aspect, Integer> aspects, boolean blocked) {}
        private static final int MAX_DEPTH = 100, MAX_WORK = 4096;
        private static final class Work {
            private int visits;
            private int cycles;
            private boolean exhausted;
            private final Map<Key, Map<Aspect, Integer>> completed = new HashMap<>();
            private boolean visit(int depth) {
                if (depth >= MAX_DEPTH || ++visits > MAX_WORK) { exhausted = true; return false; }
                return true;
            }
        }
        private final Map<Item, List<Candidate>> recipes = new HashMap<>();
        private final Set<Recipe<?>> invalidRecipes = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<Key, Map<Aspect, Integer>> cache = new LinkedHashMap<>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, Map<Aspect, Integer>> entry) { return size() > 4096; }
        };
        private final Map<Key, Boolean> limited = new LinkedHashMap<>(16, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, Boolean> entry) { return size() > 4096; }
        };

        public Calculator(HolderLookup.Provider registries, PotionBrewing brewing, Collection<? extends Recipe<?>> source) {
            this.registries = registries;
            this.brewing = brewing;
            for (var recipe : source) {
                boolean modern = recipe instanceof AbstractCookingRecipe || recipe instanceof StonecutterRecipe
                        || recipe instanceof SmithingTransformRecipe;
                if (!(recipe instanceof CraftingRecipe || recipe instanceof ArcaneRecipe || recipe instanceof CrucibleRecipe) && !modern) continue;
                try {
                    var output = recipe.getResultItem(registries).copy();
                    if (output.isEmpty()) continue; // Dynamic/special outputs need their own explicit integration.
                    List<Ingredient> ingredients;
                    if (recipe instanceof SmithingTransformRecipe smithing) {
                        // Smithing does not expose its ingredients through Recipe.getIngredients().
                        // Its public codec preserves NeoForge custom ingredients without reflection.
                        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
                        var json = new SmithingTransformRecipe.Serializer().codec().codec()
                                .encodeStart(ops, smithing).getOrThrow().getAsJsonObject();
                        ingredients = new ArrayList<>();
                        for (String slot : List.of("template", "base", "addition"))
                            ingredients.add(Ingredient.CODEC.parse(ops, json.get(slot)).getOrThrow());
                    } else ingredients = List.copyOf(recipe.getIngredients());
                    recipes.computeIfAbsent(output.getItem(), key -> new ArrayList<>())
                            .add(new Candidate(recipe, output, ingredients, modern));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Cannot inspect aspect ingredients for recipe class {}", recipe.getClass().getName(), exception);
                }
            }
            // Resolve ordinary construction before broad recoloring/conversion alternatives.
            // The latter form dense cycles in 1.21 (all sixteen bed/carpet/wool colors).
            for (var candidates : recipes.values()) candidates.sort(Comparator
                    .comparingInt((Candidate candidate) -> candidate.recipe() instanceof ShapedRecipe
                            || candidate.recipe() instanceof ArcaneRecipe arcane && !arcane.shapeless() ? 0 : 1)
                    .thenComparingInt(Calculator::complexity));
        }

        private static int complexity(Candidate candidate) {
            try { return candidate.ingredients().stream().mapToInt(ingredient -> ingredient.getItems().length).sum(); }
            catch (RuntimeException exception) { return Integer.MAX_VALUE; }
        }

        public synchronized Map<Aspect, Integer> get(ItemStack stack) {
            if (stack.isEmpty()) return Map.of();
            var key = Key.of(stack);
            var known = cache.get(key);
            if (known != null) return known;
            if (limited.get(key) != null) return Map.of();
            var work = new Work();
            var result = derive(stack, new HashSet<>(), work);
            // Never cache recursive/partially visited intermediate results.
            // A bounded traversal must not turn an incomplete search into a permanent aspect assignment.
            if (work.exhausted) {
                // Remember the unresolved traversal separately until recipe/tag reload, so
                // holding a tooltip cannot repeat an expensive failed search every frame.
                limited.put(key, Boolean.TRUE);
                return Map.of();
            }
            cache.put(key, result);
            return result;
        }

        private Map<Aspect, Integer> derive(ItemStack stack, Set<Key> path, Work work) {
            if (stack.isEmpty()) return Map.of();
            var key = Key.of(stack);
            if (path.contains(key)) { work.cycles++; return Map.of(); }
            if (!work.visit(path.size())) return Map.of();
            var completed = work.completed.get(key);
            if (completed != null) return completed;
            int cyclesBefore = work.cycles;
            path.add(key);
            try {
                Map<Aspect, Integer> base;
                if (PotionAspects.handles(stack)) base = PotionAspects.base(stack, brewing, reagent -> derive(reagent, path, work));
                else {
                    base = AspectTags.baseForItem(stack);
                    var rule = AspectRecipeRules.get(stack);
                    if (rule != null && !rule.complex()) base = AspectRecipeRules.apply(rule, Map.of(), input -> derive(input, path, work));
                    else if (base.isEmpty()) {
                        base = AspectMath.cap(fromRecipes(stack, path, work));
                        if (rule != null) base = AspectRecipeRules.apply(rule, base, input -> derive(input, path, work));
                    }
                }
                var result = ItemAspectBonuses.apply(stack, base);
                // Only cycle-free subtrees are independent of the current ancestor path.
                if (!work.exhausted && work.cycles == cyclesBefore) work.completed.put(key, result);
                return result;
            } finally { path.remove(key); }
        }

        private Map<Aspect, Integer> fromRecipes(ItemStack stack, Set<Key> path, Work work) {
            var candidates = recipes.getOrDefault(stack.getItem(), List.of());
            // TC6 gives crucible recipes priority, regardless of recipe-manager iteration order.
            for (var candidate : candidates) {
                if (!(candidate.recipe() instanceof CrucibleRecipe crucible) || !outputMatches(candidate.output(), stack)) continue;
                if (crucible.catalyst().hasNoItems()) continue;
                var input = select(crucible.catalyst(), path, work);
                if (input.blocked() || input.stack().isEmpty()) continue;
                var sum = new LinkedHashMap<>(input.aspects());
                int output = candidate.output().getCount();
                crucible.aspects().forEach((id, amount) -> sum.merge(Aspect.byId(id).orElseThrow(),
                        (int) (Math.sqrt(amount) / output), Integer::sum));
                sum.values().removeIf(n -> n <= 0);
                return sum;
            }
            var legacy = fromOrdinaryRecipes(stack, candidates, false, path, work);
            // New workstation routes fill gaps; they do not lower original crafting-derived values.
            return legacy.isEmpty() ? fromOrdinaryRecipes(stack, candidates, true, path, work) : legacy;
        }

        private Map<Aspect, Integer> fromOrdinaryRecipes(ItemStack stack, List<Candidate> candidates,
                                                        boolean modern, Set<Key> path, Work work) {
            Map<Aspect, Integer> best = Map.of();
            int bestTotal = Integer.MAX_VALUE;
            for (var candidate : candidates) {
                var recipe = candidate.recipe();
                if (candidate.modern() != modern || recipe instanceof CrucibleRecipe || invalidRecipes.contains(recipe)
                        || !outputMatches(candidate.output(), stack)) continue;
                int cyclesBefore = work.cycles;
                try {
                    var sum = new LinkedHashMap<Aspect, Integer>();
                    var ingredients = new ArrayList<ItemStack>();
                    boolean missing = false;
                    for (var ingredient : candidate.ingredients()) {
                        if (!ingredient.isEmpty() && ingredient.hasNoItems()) { missing = true; break; }
                        var input = select(ingredient, path, work);
                        if (input.blocked() || !ingredient.isEmpty() && input.stack().isEmpty()) { missing = true; break; }
                        ingredients.add(input.stack());
                        input.aspects().forEach((aspect, amount) -> sum.merge(aspect, amount, Integer::sum));
                    }
                    if (missing) continue; // An unresolved/empty required tag is not a craftable recipe.
                    // Ask the recipe itself about reusable catalysts and component-aware containers.
                    if (!modern) {
                        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth()
                                : recipe instanceof ArcaneRecipe arcane && !arcane.shapeless() ? arcane.pattern().width() : Math.min(3, ingredients.size());
                        int height = width == 0 ? 0 : (ingredients.size() + width - 1) / width;
                        while (ingredients.size() < width * height) ingredients.add(ItemStack.EMPTY);
                        var input = CraftingInput.of(width, height, ingredients);
                        var remainders = recipe instanceof CraftingRecipe crafting ? crafting.getRemainingItems(input)
                                : ((ArcaneRecipe) recipe).getRemainingItems(input);
                        for (var remainder : remainders) derive(remainder, path, work)
                                .forEach((aspect, amount) -> AspectMath.reduce(sum, aspect, amount));
                    }
                    // Furnaces, stonecutters and smithing consume their input slots; they do not
                    // execute crafting-grid container callbacks. Fuel is not an item ingredient.
                    int output = candidate.output().getCount();
                    boolean processing = recipe instanceof AbstractCookingRecipe || recipe instanceof StonecutterRecipe;
                    sum.replaceAll((aspect, amount) -> processing ? amount / output : scaled(amount, output));
                    if (recipe instanceof ArcaneRecipe arcane && arcane.vis() > 0)
                        sum.merge(Aspect.MAGIC, (int) (Math.sqrt(1 + arcane.vis() / 2) / output), Integer::sum);
                    sum.values().removeIf(n -> n <= 0);
                    int total = sum.values().stream().mapToInt(Integer::intValue).sum();
                    // Recoloring back through an ancestor is not a cheaper source of the
                    // material. Keep an already resolved construction instead of eroding
                    // its aspects through repeated 75% conversion loops.
                    if (!best.isEmpty() && work.cycles != cyclesBefore) continue;
                    if (total > 0 && total < bestTotal) { bestTotal = total; best = sum; }
                } catch (RuntimeException exception) {
                    // One third-party crafting callback must not break every tooltip or smelter.
                    invalidRecipes.add(recipe);
                    LOGGER.warn("Cannot calculate aspects for recipe class {}", recipe.getClass().getName(), exception);
                }
                if (work.exhausted) break;
            }
            return best;
        }

        private Selected select(Ingredient ingredient, Set<Key> path, Work work) {
            var options = ingredient.getItems();
            if (options.length == 0) return new Selected(ItemStack.EMPTY, Map.of(), false);
            // Prefer an already assigned material before descending into broad modern
            // recoloring tags. This still picks TC6's first representative when it is known.
            for (var option : options) if (!option.isEmpty() && ingredient.test(option)
                    && !AspectTags.baseForItem(option).isEmpty())
                return new Selected(option.copyWithCount(1), derive(option, path, work), false);
            ItemStack first = ItemStack.EMPTY;
            boolean blocked = false;
            for (var option : options) {
                if (option.isEmpty() || !ingredient.test(option)) continue;
                int cyclesBefore = work.cycles;
                var values = derive(option, path, work);
                if (!values.isEmpty()) return new Selected(option.copyWithCount(1), values, false);
                if (work.cycles != cyclesBefore) blocked = true;
                else if (first.isEmpty()) first = option.copyWithCount(1);
                if (work.exhausted) break;
            }
            return new Selected(first, Map.of(), first.isEmpty() && blocked);
        }

        private static boolean outputMatches(ItemStack output, ItemStack requested) {
            if (!output.is(requested.getItem())) return false;
            for (var entry : output.getComponentsPatch().entrySet()) {
                var type = entry.getKey();
                // Wear, names and enchantments are not subtype identities in TC6.
                if (type == DataComponents.DAMAGE || type == DataComponents.CUSTOM_NAME || type == DataComponents.REPAIR_COST
                        || type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS) continue;
                if (!Objects.equals(entry.getValue().orElse(null), requested.get(type))) return false;
            }
            return true;
        }
    }

    public static int scaled(int total, int output) { return AspectMath.scaled(total, output); }
    private RecipeAspects() {}
}
