package thaumcraft.port.data;

import java.util.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.essentia.EssentiaItem;
import thaumcraft.port.research.*;

/** Numeric regressions against TC6 rules using real 1.21 stacks, components and recipes. */
public final class AspectFixtures {
    public static int validate(ResourceManager resources, RegistryAccess registries, RegistryOps<JsonElement> ops) throws Exception {
        int checks = 0;
        require(AspectTags.forItem(new ItemStack(Items.IRON_INGOT)).equals(Map.of(Aspect.METAL, 15)), "Actual common iron tag is loaded"); checks++;
        for (var rule : AspectRecipeRules.all()) {
            require(BuiltInRegistries.ITEM.containsKey(rule.target()), "Recipe rule resolves to a real item: " + rule.target()); checks++;
            if (rule.source() != null) { require(BuiltInRegistries.ITEM.containsKey(rule.source()), "Rule source exists: " + rule.source()); checks++; }
        }
        Item[] swords = {Items.WOODEN_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};
        int[] aversion = {16, 16, 20, 24, 28};
        for (int i = 0; i < swords.length; i++) {
            require(AspectTags.forItem(new ItemStack(swords[i])).get(Aspect.AVERSION) == aversion[i], "TC6 sword bonus " + swords[i]); checks++;
        }
        Item[] tools = {Items.WOODEN_PICKAXE, Items.GOLDEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.DIAMOND_PICKAXE, Items.GOLDEN_HOE, Items.IRON_HOE, Items.SHEARS};
        int[] toolAmounts = {4, 4, 8, 12, 16, 4, 12, 12};
        for (int i = 0; i < tools.length; i++) {
            require(AspectTags.forItem(new ItemStack(tools[i])).get(Aspect.TOOL) == toolAmounts[i], "TC6 tool/hoe/shears bonus " + tools[i]); checks++;
        }
        require(AspectTags.forItem(new ItemStack(Items.DIAMOND_CHESTPLATE)).get(Aspect.PROTECT) == 32, "Armor defense bonus"); checks++;
        require(AspectTags.forItem(new ItemStack(Items.BOW)).equals(Map.of(Aspect.AVERSION, 10, Aspect.FLIGHT, 5)), "Bow bonuses"); checks++;
        for (var item : List.of(Items.RED_DYE, Items.BLUE_DYE, Items.INK_SAC, Items.COCOA_BEANS, Items.LAPIS_LAZULI, Items.BONE_MEAL)) {
            require(AspectTags.forItem(new ItemStack(item)).get(Aspect.SENSES) >= 5, "Dye migration " + item); checks++;
        }

        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
        var swordAspects = AspectTags.forItem(sword);
        require(swordAspects.get(Aspect.AVERSION) == 28 && swordAspects.get(Aspect.MAGIC) == 15, "Sharpness uses max merge, not addition"); checks++;
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantments.getOrThrow(Enchantments.INFINITY), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        require(AspectTags.forItem(book).equals(Map.of(Aspect.CRAFT, 3, Aspect.MAGIC, 9)), "Stored enchantment + very rare bonus"); checks++;
        var frost = new ItemStack(Items.ENCHANTED_BOOK);
        var frostStored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        frostStored.set(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), 1);
        frost.set(DataComponents.STORED_ENCHANTMENTS, frostStored.toImmutable());
        require(AspectTags.forItem(frost).get(Aspect.FIRE) == 1 && AspectTags.forItem(frost).get(Aspect.AVERSION) == 1, "Split enchantment levels truncate separately"); checks++;

        var brewingBuilder = new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);
        PotionBrewing.addVanillaMixes(brewingBuilder);
        var brewing = brewingBuilder.build();
        var calculator = new RecipeAspects.Calculator(registries, brewing, List.of());
        require(calculator.get(PotionContents.createItemStack(Items.POTION, Potions.WATER)).equals(Map.of(Aspect.WATER, 10)), "Water bottle has ten Aqua"); checks++;
        require(calculator.get(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)).equals(Map.of(Aspect.WATER, 5, Aspect.AVERSION, 5)), "Splash water"); checks++;
        require(calculator.get(PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WATER)).equals(Map.of(Aspect.WATER, 5, Aspect.ENERGY, 5)), "Lingering water"); checks++;
        require(calculator.get(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.WATER)).equals(Map.of(Aspect.WATER, 5, Aspect.TRAP, 5)), "Tipped arrow container supplement"); checks++;
        var awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        require(calculator.get(awkward).equals(Map.of(Aspect.PLANT, 1, Aspect.FLUX, 1, Aspect.ALCHEMY, 3, Aspect.WATER, 5)), "Nether wart reagent plus alchemy, then 66% reduction"); checks++;
        awkward.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        require(calculator.get(awkward).equals(Map.of(Aspect.WATER, 10)), "Mutating components does not reuse another potion's cache"); checks++;
        require(calculator.get(book).get(Aspect.MAGIC) == 9 && calculator.get(new ItemStack(Items.ENCHANTED_BOOK)).isEmpty(), "Stored-enchantment cache isolation"); checks++;
        for (var aspect : Aspect.values()) {
            require(calculator.get(CrystalItem.stack(aspect)).equals(Map.of(aspect, 1)), "Crystal contents " + aspect); checks++;
            require(calculator.get(EssentiaItem.phial(aspect)).equals(Map.of(aspect, 10)), "Phial contents " + aspect); checks++;
        }

        var ordinary = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.BARRIER),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE)));
        var recipeCalc = new RecipeAspects.Calculator(registries, brewing, List.of(ordinary));
        require(recipeCalc.get(new ItemStack(Items.BARRIER)).equals(Map.of(Aspect.ENERGY, 7)), "Crafting 75% aspect yield"); checks++;
        var reusable = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.BARRIER),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE))) {
            @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
                return NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.REDSTONE));
            }
        };
        require(new RecipeAspects.Calculator(registries, brewing, List.of(reusable)).get(new ItemStack(Items.BARRIER)).isEmpty(), "Recipe-specific reusable ingredient is subtracted"); checks++;
        var crucible = new CrucibleRecipe("", Ingredient.of(Items.REDSTONE), Map.of("ignis", 16), new ItemStack(Items.BARRIER, 2));
        require(new RecipeAspects.Calculator(registries, brewing, List.of(ordinary, crucible)).get(new ItemStack(Items.BARRIER))
                .equals(Map.of(Aspect.ENERGY, 10, Aspect.FIRE, 2)), "Crucible priority and undivided catalyst aspects"); checks++;
        var a = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.BARRIER), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.STRUCTURE_VOID)));
        var b = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.STRUCTURE_VOID), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.BARRIER)));
        require(new RecipeAspects.Calculator(registries, brewing, List.of(a, b)).get(new ItemStack(Items.BARRIER)).isEmpty(), "Recipe cycles terminate"); checks++;
        var cycleBuilder = new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);
        cycleBuilder.addMix(Potions.AWKWARD, Items.REDSTONE, Potions.MUNDANE);
        cycleBuilder.addMix(Potions.MUNDANE, Items.REDSTONE, Potions.AWKWARD);
        require(!new RecipeAspects.Calculator(registries, cycleBuilder.build(), List.of())
                .get(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)).isEmpty(), "Custom brewing cycle terminates"); checks++;

        var state = new ResearchState();
        var airKey = Scanning.itemSubject(CrystalItem.stack(Aspect.AIR));
        var fireKey = Scanning.itemSubject(CrystalItem.stack(Aspect.FIRE));
        require(airKey.equals(fireKey), "Crystal NBT is not a separate generic observation"); checks++;
        require(state.recordScan(airKey, List.of(Aspect.AIR)) && !state.recordScan(fireKey, List.of(Aspect.FIRE)), "Generic reward once, aspect discoveries independent"); checks++;
        require(state.discovered().containsAll(List.of("aer", "ignis")) && state.knowledge("OBSERVATION", "AUROMANCY") == 2, "Both aspects reveal original ScanAspect rewards"); checks++;
        state.stage("FIRSTSTEPS", 2); state.addKnowledge("THEORY", "BASICS", 17); state.warp(2, 3, 4);
        var copy = ResearchCodec.read(ResearchCodec.write(state));
        require(copy.discovered().equals(state.discovered()) && copy.knowledge().equals(state.knowledge()) && copy.stage("FIRSTSTEPS") == 2
                && copy.permanentWarp() == 2 && copy.normalWarp() == 3 && copy.temporaryWarp() == 4, "Journal round-trip preserves discovery, stages, currency and warp"); checks++;
        require(!copy.recordScan(fireKey, List.of(Aspect.FIRE)) && copy.knowledge().equals(state.knowledge()), "Reload cannot repeat aspect discovery rewards"); checks++;
        var storage = new ResearchStorage();
        var firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        storage.player(firstPlayer).recordScan(airKey, List.of(Aspect.AIR));
        storage.player(firstPlayer).stage("FIRSTSTEPS", 2);
        storage.player(firstPlayer).warp(1, 2, 3);
        storage.player(secondPlayer).recordScan("item:minecraft:stone", List.of(Aspect.EARTH));
        storage.player(secondPlayer).addKnowledge("THEORY", "ALCHEMY", 31);
        var storageCopy = ResearchStorage.load(storage.save(new net.minecraft.nbt.CompoundTag(), registries), registries);
        require(storageCopy.player(firstPlayer).discovered().equals(Set.of("aer"))
                && storageCopy.player(secondPlayer).discovered().equals(Set.of("terra")), "SavedData keeps UUID journals separate"); checks++;
        require(storageCopy.player(firstPlayer).stage("FIRSTSTEPS") == 2 && storageCopy.player(firstPlayer).temporaryWarp() == 3
                && storageCopy.player(secondPlayer).knowledge("THEORY", "ALCHEMY") == 31, "SavedData round-trip preserves player progress"); checks++;
        var noteSubjects = new HashSet<String>();
        for (int kind = 0; kind <= 12; kind++) noteSubjects.add(Scanning.itemSubject(CelestialNotesItem.stack(kind)));
        require(noteSubjects.size() == 13, "Celestial metadata maps to thirteen scan identities"); checks++;
        require(Scanning.itemSubject(EssentiaItem.phial(Aspect.AIR)).equals(Scanning.itemSubject(EssentiaItem.phial(Aspect.FIRE))), "Filled phials share original metadata identity"); checks++;
        for (int kind = 0; kind <= 12; kind++) {
            require(calculator.get(CelestialNotesItem.stack(kind)).equals(Map.of(Aspect.MIND, 5, Aspect.DARKNESS, 5, Aspect.LIGHT, 5)), "Original celestial-note aspects " + kind); checks++;
        }
        var loadedRecipes = new ArrayList<Recipe<?>>();
        for (var entry : resources.listResources("recipe", id -> id.getPath().endsWith(".json")
                && (id.getNamespace().equals("minecraft") || id.getNamespace().equals("thaumcraft"))).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) { loadedRecipes.add(Recipe.CODEC.parse(ops, JsonParser.parseReader(reader)).getOrThrow()); }
        }
        require(loadedRecipes.size() > 1000, "Load vanilla recipes as well as mod recipes for derivation"); checks++;
        var full = new RecipeAspects.Calculator(registries, brewing, loadedRecipes);
        var cauldron = full.get(new ItemStack(Items.CAULDRON));
        require(cauldron.equals(Map.of(Aspect.METAL, 78, Aspect.ALCHEMY, 15)), "Seven iron ingots + original cauldron bonus"); checks++;
        require(full.get(new ItemStack(thaumcraft.port.machine.MachineContent.CRUCIBLE.get()))
                .equals(Map.of(Aspect.METAL, 78, Aspect.ALCHEMY, 35, Aspect.CRAFT, 20)), "Crucible copies the complete cauldron aspects before additions"); checks++;
        var waterBucket = full.get(new ItemStack(Items.WATER_BUCKET));
        require(waterBucket.equals(Map.of(Aspect.METAL, 33, Aspect.VOID, 5, Aspect.WATER, 20)), "Filled bucket retains metal and container aspects"); checks++;
        var thaumometer = full.get(new ItemStack(PortContent.THAUMOMETER.get()));
        require(thaumometer.get(Aspect.SENSES) == 10 && thaumometer.get(Aspect.AURA) == 10
                && thaumometer.get(Aspect.MAGIC) == 3 && thaumometer.get(Aspect.METAL) == 30
                && thaumometer.get(Aspect.DESIRE) == 30, "Thaumometer derives four gold ingots and its original extra aspects"); checks++;
        var crafting = full.get(new ItemStack(Items.CRAFTING_TABLE));
        var arcaneExpected = new LinkedHashMap<>(crafting); arcaneExpected.merge(Aspect.MAGIC, 5, Integer::sum); arcaneExpected.merge(Aspect.AURA, 5, Integer::sum);
        require(crafting.get(Aspect.CRAFT) == 20 && full.get(new ItemStack(thaumcraft.port.machine.MachineContent.WORKBENCH.get()))
                .equals(arcaneExpected), "Arcane workbench copies crafting table with Fabrico bonus"); checks++;
        var bookshelf = new LinkedHashMap<>(full.get(new ItemStack(Items.BOOKSHELF)));
        AspectMath.merge(bookshelf, Aspect.MAGIC, 10);
        require(full.get(new ItemStack(PortContent.THAUMONOMICON.get())).equals(AspectMath.finish(bookshelf)), "Thaumonomicon copies bookshelf with Praecantatio max-merge"); checks++;
        for (var color : DyeColor.values()) {
            var terracotta = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_terracotta"));
            require(full.get(new ItemStack(terracotta)).equals(full.get(new ItemStack(Items.WHITE_TERRACOTTA))), "Stained terracotta wildcard: " + color); checks++;
        }
        checks += ModernAspectFixtures.validate(registries, full);
        return checks;
    }

    private static void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    private AspectFixtures() {}
}
