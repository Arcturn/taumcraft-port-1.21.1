package thaumcraft.port.gametest;

import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.PortContent;

/** Synthetic recipes use an existing unassigned mod display item, without adding fake gameplay items. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class RecipeAspectGameTests {
    private static ItemStack modOutput() { return new ItemStack(PortContent.RESEARCH_ICON.get()); }
    private static ShapelessRecipe craft(ItemStack output, Ingredient... inputs) {
        return new ShapelessRecipe("", CraftingBookCategory.MISC, output, NonNullList.of(Ingredient.EMPTY, inputs));
    }
    private static RecipeAspects.Calculator calculator(GameTestHelper helper, Recipe<?>... recipes) {
        var level = helper.getLevel();
        return new RecipeAspects.Calculator(level.registryAccess(), level.potionBrewing(), List.of(recipes));
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void modCraftingRecipesInheritKnownIngredientsAndOutputCounts(GameTestHelper helper) {
        helper.assertTrue(!BuiltInRegistries.ITEM.getKey(modOutput().getItem()).getNamespace().equals("minecraft"),
                "The synthetic output must be a real non-vanilla registered item");
        helper.assertTrue(AspectTags.baseForItem(modOutput()).isEmpty(), "Fixture item must not have assigned aspects");
        var output = modOutput(); output.setCount(2);
        var calc = calculator(helper, craft(output, Ingredient.of(Items.REDSTONE)));
        helper.assertValueEqual(calc.get(modOutput()), Map.of(Aspect.ENERGY, 3), "Crafting divides 75% yield by output count");
        helper.assertValueEqual(calc.get(modOutput().copyWithCount(64)), Map.of(Aspect.ENERGY, 3), "Stack count must not multiply per-item aspects");

        var alternatives = Ingredient.of(Items.BARRIER, Items.REDSTONE);
        helper.assertTrue(alternatives.getItems()[0].is(Items.BARRIER), "Fixture must put an unknown alternative first");
        helper.assertValueEqual(calculator(helper, craft(modOutput(), alternatives)).get(modOutput()),
                Map.of(Aspect.ENERGY, 7), "Unknown first alternative must not hide a known valid material");
        helper.assertTrue(calculator(helper, craft(modOutput(), Ingredient.of(Items.BARRIER))).get(modOutput()).isEmpty(),
                "An unknown raw material must not acquire arbitrary aspects");

        var missingTag = Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("aspect_fixture:absent")));
        helper.assertTrue(calculator(helper, craft(modOutput(), missingTag, Ingredient.of(Items.REDSTONE))).get(modOutput()).isEmpty(),
                "An uncraftable recipe with a missing required tag must not fabricate a partial assignment");
        System.out.println("ASPECT_MOD_COMPAT namespace=thaumcraft synthetic_recipes=true output_count=pass alternatives=pass missing_tag=pass unknown_raw=empty");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void recipeRemaindersCyclesAndBrokenCallbacksStayIsolated(GameTestHelper helper) {
        var reusable = new ShapelessRecipe("", CraftingBookCategory.MISC, modOutput(),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE))) {
            @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
                return NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.REDSTONE));
            }
        };
        helper.assertTrue(calculator(helper, reusable).get(modOutput()).isEmpty(), "Reusable catalyst must be subtracted");
        var broken = new ShapelessRecipe("", CraftingBookCategory.MISC, modOutput(),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE))) {
            @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
                throw new IllegalArgumentException("Intentional foreign-recipe failure fixture");
            }
        };
        helper.assertValueEqual(calculator(helper, broken, craft(modOutput(), Ingredient.of(Items.REDSTONE))).get(modOutput()),
                Map.of(Aspect.ENERGY, 7), "One broken custom recipe must not break a valid alternate recipe");

        var a = craft(new ItemStack(Items.BARRIER), Ingredient.of(Items.STRUCTURE_VOID));
        var b = craft(new ItemStack(Items.STRUCTURE_VOID), Ingredient.of(Items.BARRIER));
        helper.assertTrue(calculator(helper, a, b).get(new ItemStack(Items.BARRIER)).isEmpty(), "Closed recipe cycle must terminate without invented aspects");
        var seed = craft(new ItemStack(Items.STRUCTURE_VOID), Ingredient.of(Items.REDSTONE));
        var seeded = calculator(helper, a, b, seed);
        helper.assertValueEqual(seeded.get(new ItemStack(Items.BARRIER)), Map.of(Aspect.ENERGY, 5), "A cycle must not hide a separate material route");
        helper.assertValueEqual(seeded.get(new ItemStack(Items.STRUCTURE_VOID)), Map.of(Aspect.ENERGY, 7), "Recursive partial results must not poison the cache");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void modernWorkstationsDeriveMissingOutputsWithoutChangingLegacyPriority(GameTestHelper helper) {
        var ingredient = Ingredient.of(Items.REDSTONE);
        var output = modOutput().copyWithCount(2);
        Recipe<?>[] single = {
                new SmeltingRecipe("", CookingBookCategory.MISC, ingredient, output, 0, 200),
                new BlastingRecipe("", CookingBookCategory.MISC, ingredient, output, 0, 100),
                new SmokingRecipe("", CookingBookCategory.MISC, ingredient, output, 0, 100),
                new CampfireCookingRecipe("", CookingBookCategory.MISC, ingredient, output, 0, 600),
                new StonecutterRecipe("", ingredient, output)
        };
        for (var recipe : single) helper.assertValueEqual(calculator(helper, recipe).get(modOutput()), Map.of(Aspect.ENERGY, 5),
                "Modern single-input recipe must account for yield: " + recipe.getType());
        var smithing = new SmithingTransformRecipe(Ingredient.of(Items.BARRIER), Ingredient.of(Items.REDSTONE),
                Ingredient.of(Items.IRON_INGOT), modOutput());
        helper.assertValueEqual(calculator(helper, smithing).get(modOutput()), Map.of(Aspect.ENERGY, 7, Aspect.METAL, 11),
                "Smithing must read all three ingredients through its codec");
        var legacy = craft(modOutput(), Ingredient.of(Items.GOLD_INGOT));
        var cheapModern = new StonecutterRecipe("", ingredient, modOutput());
        helper.assertValueEqual(calculator(helper, cheapModern, legacy).get(modOutput()),
                calculator(helper, legacy).get(modOutput()), "New workstation fallback must not reduce a legacy crafting assignment");
        var direct = craft(new ItemStack(Items.IRON_INGOT), Ingredient.of(Items.REDSTONE));
        helper.assertValueEqual(calculator(helper, direct).get(new ItemStack(Items.IRON_INGOT)), Map.of(Aspect.METAL, 15),
                "Explicit/common-tag material values must retain priority");
        System.out.println("ASPECT_WORKSTATIONS crafting=pass cooking_types=4 stonecutting=pass smithing=pass legacy_priority=pass");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void componentSubtypesAndRecipeSnapshotsDoNotShareCachedValues(GameTestHelper helper) {
        var first = modOutput(); var firstData = new CompoundTag(); firstData.putInt("variant", 1);
        first.set(DataComponents.CUSTOM_DATA, CustomData.of(firstData));
        var second = modOutput(); var secondData = new CompoundTag(); secondData.putInt("variant", 2);
        second.set(DataComponents.CUSTOM_DATA, CustomData.of(secondData));
        var calc = calculator(helper, craft(first, Ingredient.of(Items.REDSTONE)));
        helper.assertValueEqual(calc.get(first), Map.of(Aspect.ENERGY, 7), "Matching component subtype must derive aspects");
        helper.assertTrue(calc.get(second).isEmpty(), "Another component subtype must not inherit an incompatible recipe");
        helper.assertTrue(calc.get(modOutput()).isEmpty(), "Plain item must not inherit a component-only recipe");
        var afterReload = calculator(helper, craft(first, Ingredient.of(Items.IRON_INGOT)));
        helper.assertValueEqual(afterReload.get(first), Map.of(Aspect.METAL, 11), "A new recipe snapshot must not reuse old assignments");
        helper.assertValueEqual(calc.get(first), Map.of(Aspect.ENERGY, 7), "Independent recipe managers must retain independent caches");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void loadedVanillaRecoloringGraphsKeepTheirConstructiveMaterialRoutes(GameTestHelper helper) {
        var level = helper.getLevel();
        var calc = new RecipeAspects.Calculator(level.registryAccess(), level.potionBrewing(),
                level.getRecipeManager().getRecipes().stream().map(RecipeHolder::value).toList());
        helper.assertValueEqual(calc.get(new ItemStack(Items.WHITE_CARPET)), Map.of(Aspect.BEAST, 7, Aspect.CRAFT, 2),
                "Recoloring cycles must not replace the original two-wool carpet calculation");
        var whiteBed = calc.get(new ItemStack(Items.WHITE_BED));
        helper.assertTrue(whiteBed.getOrDefault(Aspect.BEAST, 0) == 33,
                "White bed must retain the original three-wool material contribution: " + whiteBed);
        int checked = 0;
        for (var color : net.minecraft.world.item.DyeColor.values()) for (String suffix : List.of("wool", "carpet", "bed", "shulker_box")) {
            var id = ResourceLocation.withDefaultNamespace(color.getName() + "_" + suffix);
            var values = calc.get(new ItemStack(BuiltInRegistries.ITEM.get(id)));
            helper.assertTrue(!values.isEmpty(), "Loaded vanilla recipe family lost its aspects: " + id);
            helper.assertTrue(values.getOrDefault(Aspect.BEAST, 0) > 0, "Repeated recoloring erased the material of " + id);
            checked++;
        }
        for (var item : List.of(Items.MUD_BRICK_SLAB, Items.CRACKED_DEEPSLATE_TILES))
            helper.assertTrue(calc.get(new ItemStack(item)).getOrDefault(Aspect.EARTH, 0) > 0,
                    "Single-input processing must not erase the remaining material: " + item);
        System.out.println("ASPECT_VANILLA_GRAPH colored_family_items=" + checked + " material_routes=pass modern_processing=pass");
        helper.succeed();
    }
}
