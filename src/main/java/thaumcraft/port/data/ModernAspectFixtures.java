package thaumcraft.port.data;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import thaumcraft.port.aspect.*;

/** Semantic modern-content checks plus an honest inventory audit using loaded game data. */
public final class ModernAspectFixtures {
    public static int validate(RegistryAccess registries, RecipeAspects.Calculator calculator) throws Exception {
        int checks = 0;
        var modern = json("modern-aspect-tags.json");
        for (var entry : modern.entrySet()) {
            String key = entry.getKey();
            var id = ResourceLocation.parse(key.substring(key.indexOf(':') + 1));
            if (key.startsWith("item:")) {
                require(BuiltInRegistries.ITEM.containsKey(id), "Modern item exists: " + id);
                require(!calculator.get(new ItemStack(BuiltInRegistries.ITEM.get(id))).isEmpty(), "Modern item has aspects: " + id);
            } else if (key.startsWith("entity:")) {
                require(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "Modern entity exists: " + id);
            } else if (key.startsWith("block:")) {
                require(BuiltInRegistries.BLOCK.containsKey(id), "Modern block exists: " + id);
                require(!AspectTags.forBlock(BuiltInRegistries.BLOCK.get(id).defaultBlockState()).isEmpty(), "Modern world-only block has aspects: " + id);
            } else if (key.startsWith("tag:")) {
                require(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).isPresent(), "Modern common tag exists: " + id);
            } else throw new IllegalStateException("Unknown modern aspect key: " + key);
            for (var amount : entry.getValue().getAsJsonObject().entrySet())
                require(Aspect.byId(amount.getKey()).isPresent() && amount.getValue().getAsInt() > 0,
                        "Modern seed uses a positive original Latin aspect: " + key);
            checks++;
        }
        for (String oldFile : List.of("aspect-tags.json", "world-aspect-tags.json", "vanilla-aspect-tags.json",
                "additional-aspect-tags.json", "deepslate-aspect-tags.json")) {
            for (var entry : json(oldFile).entrySet()) require(!modern.has(entry.getKey()), "Modern map must not replace migrated value: " + entry.getKey());
            checks++;
        }
        require(calculator.get(new ItemStack(Items.RAW_IRON)).get(Aspect.METAL) == 15, "Raw iron keeps original metal scale"); checks++;
        require(calculator.get(new ItemStack(Items.COPPER_INGOT)).equals(Map.of(Aspect.METAL, 10, Aspect.EXCHANGE, 5)), "Original mod-copper definition is reused by vanilla copper"); checks++;
        require(calculator.get(new ItemStack(Items.DEEPSLATE_COAL_ORE)).equals(calculator.get(new ItemStack(Items.COAL_ORE))), "Coal host rock does not change its original composition"); checks++;
        require(calculator.get(new ItemStack(Items.AMETHYST_SHARD)).equals(Map.of(Aspect.CRYSTAL, 10, Aspect.SENSES, 5)), "Amethyst reflects crystallinity and resonance"); checks++;
        require(calculator.get(new ItemStack(Items.SCULK_SENSOR)).containsKey(Aspect.SENSES)
                && !calculator.get(new ItemStack(Items.SCULK_SENSOR)).containsKey(Aspect.FLUX), "Sculk is not invented Thaumcraft taint"); checks++;
        require(calculator.get(new ItemStack(Items.BREEZE_ROD)).equals(Map.of(Aspect.AIR, 15, Aspect.MOTION, 5)), "Breeze rod is a wind/motion source"); checks++;
        require(calculator.get(new ItemStack(Items.OMINOUS_TRIAL_KEY)).get(Aspect.MAGIC) == 5, "Ominous key differs from ordinary key"); checks++;
        require(!calculator.get(new ItemStack(Items.TRIAL_KEY)).containsKey(Aspect.MAGIC), "Ordinary trial key does not inherit ominous magic"); checks++;
        require(calculator.get(new ItemStack(Items.CHERRY_DOOR)).get(Aspect.TRAP) == 5
                && calculator.get(new ItemStack(Items.CHERRY_DOOR)).get(Aspect.MECHANISM) == 5, "New door inherits original functional bonus"); checks++;
        require(calculator.get(new ItemStack(Items.CHERRY_BUTTON)).get(Aspect.MECHANISM) == 5, "New button inherits original functional bonus"); checks++;
        var copper = calculator.get(new ItemStack(Items.COPPER_BLOCK));
        require(calculator.get(new ItemStack(Items.OXIDIZED_COPPER)).equals(copper), "Reversible oxidation does not create or destroy metal aspects"); checks++;
        require(calculator.get(new ItemStack(Items.OXIDIZED_COPPER_DOOR)).equals(calculator.get(new ItemStack(Items.COPPER_DOOR))),
                "Oxidized copper door preserves metal and door functions"); checks++;
        require(calculator.get(new ItemStack(Items.WHITE_CANDLE)).get(Aspect.LIGHT) == 5,
                "Colored candle keeps the light function of the original candle"); checks++;
        require(calculator.get(new ItemStack(Items.STRIPPED_BAMBOO_BLOCK)).equals(calculator.get(new ItemStack(Items.BAMBOO_BLOCK))),
                "Stripping bamboo preserves its material"); checks++;
        require(calculator.get(new ItemStack(Items.CARVED_PUMPKIN)).equals(calculator.get(new ItemStack(Items.PUMPKIN))),
                "Separated carved-pumpkin state retains the original pumpkin material"); checks++;
        require(!calculator.get(new ItemStack(Items.DRIED_KELP)).containsKey(Aspect.WATER), "Drying actually removes the water aspect"); checks++;
        var bucket = calculator.get(new ItemStack(Items.WATER_BUCKET));
        var axolotl = calculator.get(new ItemStack(Items.AXOLOTL_BUCKET));
        require(axolotl.get(Aspect.METAL).equals(bucket.get(Aspect.METAL)) && axolotl.get(Aspect.WATER).equals(bucket.get(Aspect.WATER))
                && axolotl.get(Aspect.BEAST) == 5, "Animal bucket preserves the complete metal/water container"); checks++;
        for (var item : List.of(Items.NETHERITE_INGOT, Items.NETHERITE_PICKAXE, Items.NETHERITE_CHESTPLATE,
                Items.COPPER_BULB, Items.OXIDIZED_COPPER_BULB, Items.CRAFTER, Items.BAMBOO_MOSAIC,
                Items.CHERRY_HANGING_SIGN, Items.WIND_CHARGE, Items.MACE, Items.TINTED_GLASS, Items.DECORATED_POT)) {
            require(!calculator.get(new ItemStack(item)).isEmpty(), "Modern processed item derives aspects: " + BuiltInRegistries.ITEM.getKey(item)); checks++;
        }
        var mace = calculator.get(new ItemStack(Items.MACE));
        require(mace.getOrDefault(Aspect.AVERSION, 0) >= 24 && mace.containsKey(Aspect.METAL) && mace.containsKey(Aspect.AIR),
                "Mace keeps both crafting materials plus its weapon function"); checks++;
        require(calculator.get(new ItemStack(Items.CROSSBOW)).get(Aspect.MECHANISM) == 5, "Crossbow mechanical bonus"); checks++;
        require(calculator.get(new ItemStack(Items.SPYGLASS)).get(Aspect.SENSES) >= 10, "Spyglass sensing bonus"); checks++;
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        // A fully derived book already carries Aer from sugar cane, and the original
        // seven-aspect cap can cull a weak added Motus. Isolate the bonus while still
        // using the real loaded enchantment holder and data component.
        var bonusCalculator = new RecipeAspects.Calculator(registries,
                new net.minecraft.world.item.alchemy.PotionBrewing.Builder(FeatureFlags.VANILLA_SET).build(), List.of());
        var enchanted = bonusCalculator.get(book);
        require(enchanted.get(Aspect.AIR) == 1 && enchanted.get(Aspect.MOTION) == 1 && enchanted.get(Aspect.MAGIC) > 0,
                "Modern enchantment uses actual registry component and original split rounding: " + enchanted); checks++;
        var fullBook = calculator.get(book);
        require(fullBook.getOrDefault(Aspect.MAGIC, 0) > 0 && fullBook.size() <= 7,
                "Derived modern enchanted book retains the original seven-aspect cap: " + fullBook); checks++;
        System.out.println("MODERN_ENCHANTMENT isolated=" + enchanted + " fully_derived_book=" + fullBook);
        writeCoverage(calculator);
        return checks;
    }

    private static void writeCoverage(RecipeAspects.Calculator calculator) throws Exception {
        var totals = new TreeMap<String, Integer>();
        var entries = new JsonArray();
        var unresolved = new JsonArray();
        var technical = Set.of("air", "barrier", "light", "structure_void", "structure_block", "jigsaw",
                "command_block", "chain_command_block", "repeating_command_block", "command_block_minecart",
                "debug_stick", "knowledge_book", "petrified_oak_slab");
        var legacyDynamicOutputs = Set.of("firework_star", "filled_map", "written_book");
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("minecraft")) continue;
            var stack = new ItemStack(item);
            var result = calculator.get(stack);
            String classification;
            if (!result.isEmpty()) {
                if (!AspectTags.baseForItem(stack).isEmpty()) classification = "explicit_or_common_tag";
                else if (AspectRecipeRules.get(stack) != null) classification = "named_recipe_or_function_rule";
                else if (!AspectTags.forItem(stack).isEmpty()) classification = "equipment_or_component_bonus_with_optional_recipe_materials";
                else classification = "recipe_derived";
            } else if (item instanceof SpawnEggItem || technical.contains(id.getPath())) classification = "intentional_creative_or_technical";
            else if (!item.isEnabled(FeatureFlags.VANILLA_SET)) classification = "feature_disabled";
            else if (legacyDynamicOutputs.contains(id.getPath())) classification = "legacy_dynamic_recipe_output_not_derived";
            else { classification = "unresolved"; unresolved.add(id.toString()); }
            totals.merge(classification, 1, Integer::sum);
            var row = new JsonObject(); row.addProperty("item", id.toString()); row.addProperty("classification", classification);
            var amounts = new JsonObject(); result.forEach((aspect, amount) -> amounts.addProperty(aspect.id(), amount));
            row.add("aspects", amounts); entries.add(row);
        }
        var root = new JsonObject();
        root.addProperty("scope", "Every registered vanilla item default stack, using loaded tags and the real calculator. This is coverage, not a claim every special component or third-party machine recipe is supported.");
        root.addProperty("legacy_dynamic_recipe_limit", "filled_map, written_book and firework_star have no base mapping in this port and cannot be derived from their special recipes' empty static outputs. Their nonempty components do not currently provide a material resolver either. They are not counted as supported or technical, and this does not assert that original TC6 intentionally left them blank.");
        root.add("counts", new Gson().toJsonTree(totals)); root.add("unresolved", unresolved); root.add("items", entries);
        var project = Path.of("").toAbsolutePath();
        if (!Files.exists(project.resolve("build.gradle")) && Files.exists(project.resolve("../build.gradle"))) project = project.getParent();
        var output = project.resolve("build/modern-aspect-coverage.json"); Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n");
        System.out.println("MODERN_ASPECT_COVERAGE " + totals + " report=" + output);
    }

    private static JsonObject json(String name) throws Exception {
        try (var reader = new InputStreamReader(Objects.requireNonNull(ModernAspectFixtures.class.getResourceAsStream(
                "/thaumcraft-port/" + name)), StandardCharsets.UTF_8)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }
    private static void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    private ModernAspectFixtures() {}
}
