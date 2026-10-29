package thaumcraft.port.aspect;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Original complex/copy definitions, followed by separate documented modern extensions. */
public final class AspectRecipeRules {
    public record Operation(String mode, Aspect aspect, int amount) {}
    public record Rule(ResourceLocation target, boolean complex, ResourceLocation source, List<Operation> operations) {}
    private static final Map<ResourceLocation, Rule> RULES = load();

    private static Map<ResourceLocation, Rule> load() {
        var rules = new LinkedHashMap<ResourceLocation, Rule>();
        for (String file : List.of("aspect-recipe-rules.json", "modern-aspect-recipe-rules.json")) {
        try (var reader = new InputStreamReader(Objects.requireNonNull(AspectRecipeRules.class.getResourceAsStream(
                "/thaumcraft-port/" + file)), StandardCharsets.UTF_8)) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var json = element.getAsJsonObject();
                var operations = new ArrayList<Operation>();
                for (var operation : json.getAsJsonArray("operations")) {
                    var op = operation.getAsJsonObject();
                    operations.add(new Operation(op.get("op").getAsString(),
                            Aspect.byId(op.get("aspect").getAsString()).orElseThrow(), op.get("amount").getAsInt()));
                }
                var rule = new Rule(ResourceLocation.parse(json.get("target").getAsString()), json.get("kind").getAsString().equals("complex"),
                        json.get("source").isJsonNull() ? null : ResourceLocation.parse(json.get("source").getAsString()), List.copyOf(operations));
                if (file.equals("modern-aspect-recipe-rules.json") && rules.containsKey(rule.target()))
                    throw new IllegalArgumentException("Modern rule overrides original: " + rule.target());
                // Repeated complex registrations do not add a second bonus in the original proxy.
                if (rule.complex()) rules.putIfAbsent(rule.target(), rule);
                else rules.put(rule.target(), rule);
            }
        } catch (Exception exception) { throw new IllegalStateException("Cannot read original aspect recipe rules", exception); }
        }
        return Collections.unmodifiableMap(rules);
    }

    public static Collection<Rule> all() { return RULES.values(); }
    public static Rule get(ItemStack stack) { return RULES.get(BuiltInRegistries.ITEM.getKey(stack.getItem())); }

    public static Map<Aspect, Integer> apply(Rule rule, Map<Aspect, Integer> recipe,
                                            Function<ItemStack, Map<Aspect, Integer>> resolver) {
        var values = new LinkedHashMap<Aspect, Integer>();
        if (rule.source() != null) values.putAll(resolver.apply(new ItemStack(BuiltInRegistries.ITEM.get(rule.source()))));
        for (var operation : rule.operations()) {
            switch (operation.mode()) {
                case "merge" -> AspectMath.merge(values, operation.aspect(), operation.amount());
                case "add" -> values.merge(operation.aspect(), operation.amount(), Integer::sum);
                case "remove" -> { int rest = values.getOrDefault(operation.aspect(), 0) - operation.amount();
                    if (rest > 0) values.put(operation.aspect(), rest); else values.remove(operation.aspect()); }
                default -> throw new IllegalStateException("Unknown aspect operation " + operation.mode());
            }
        }
        // registerComplexObjectTag adds generated tags into the supplied AspectList, in that order.
        if (rule.complex()) recipe.forEach((aspect, amount) -> values.merge(aspect, amount, Integer::sum));
        return values;
    }

    private AspectRecipeRules() {}
}
