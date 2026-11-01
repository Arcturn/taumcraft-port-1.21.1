package thaumcraft.port.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.research.LegacyRequirement;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchCodec;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.content.PortContent;

/** Loaded original requirements, including valid chest tags and ignored stale item IDs. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class LegacyRequirementGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hungryChestAcceptsEveryOriginalChestVariant(GameTestHelper helper) {
        String source = ResearchCatalog.strings(ResearchCatalog.get("HUNGRYCHEST").stages().getFirst(),
                "required_item").stream().filter(value -> value.startsWith("oredict:")).findFirst().orElseThrow();
        var requirement = LegacyRequirement.parse(source);
        for (var item : java.util.List.of(Items.CHEST, Items.TRAPPED_CHEST, Items.ENDER_CHEST)) {
            helper.assertTrue(requirement.matches().test(new ItemStack(item)),
                    "HUNGRYCHEST must accept the original ore dictionary chest variant: " + item);
        }
        helper.assertFalse(requirement.matches().test(new ItemStack(Items.BARREL)),
                "An unrelated inventory block must not fulfill the chest requirement");
        helper.assertValueEqual(requirement.count(), 1, "Research consumes exactly one chest");
        helper.assertValueEqual(requirement.craftKey(), "minecraft:chest", "Keep the existing representative icon key");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void smelterResearchMatchesOriginalMissingCraftAndConsumesOnlyPlateAndTheory(GameTestHelper helper) {
        for (String material : java.util.List.of("thaumium", "void")) {
            String key = material.equals("thaumium") ? "ESSENTIASMELTERTHAUMIUM" : "ESSENTIASMELTERVOID";
            var entry = ResearchCatalog.get(key);
            // Unmodified BETA26 resolves the obsolete thaumcraft:metal ID to EMPTY,
            // so both loaded stages have no craft/craft-reference requirement.
            helper.assertTrue(ResearchCatalog.strings(entry.stages().getFirst(), "required_craft").isEmpty(),
                    "Loaded smelter stage retained a phantom legacy metal craft requirement");
            var player = GameTestPlayers.create(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.THAUMONOMICON.get()));
            var state = ResearchStorage.get(player.getServer()).player(player.getUUID());
            state.flag("TUBES");
            state.stage("METALLURGY", 3);
            if (material.equals("void")) {
                state.flag("ESSENTIASMELTERTHAUMIUM");
                state.flag("BASEELDRITCH");
                state.addKnowledge("THEORY", "ELDRITCH", 41);
            }
            state.stage(key, 1);
            state.addKnowledge("THEORY", "ALCHEMY", 39);
            state.craft("minecraft:stick");
            var craftedBefore = java.util.Set.copyOf(state.crafted());
            for (int i = 0; i < 3; i++) {
                String metal = java.util.List.of("brass", "thaumium", "void").get(i);
                player.getInventory().setItem(10 + i, new ItemStack(BuiltInRegistries.ITEM.get(
                        ResourceLocation.parse("thaumcraft:metal_" + metal)), 3));
            }
            var before = ResearchCodec.write(state);
            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(before.equals(ResearchCodec.write(state)), "Missing plate changed research or knowledge");
            helper.assertValueEqual(player.totalExperience, 0, "Missing plate awarded experience");

            var plate = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:plate_" + material));
            player.getInventory().setItem(9, new ItemStack(plate, 2));
            state.addKnowledge("THEORY", "ALCHEMY", -8);
            before = ResearchCodec.write(state);
            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(before.equals(ResearchCodec.write(state)), "Partial alchemy theory paid a whole-point cost");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Missing alchemy theory consumed its plate");
            state.addKnowledge("THEORY", "ALCHEMY", 8);
            if (material.equals("void")) {
                state.addKnowledge("THEORY", "ELDRITCH", -10);
                before = ResearchCodec.write(state);
                ResearchProgress.advance(player, key, 1);
                helper.assertTrue(before.equals(ResearchCodec.write(state)), "Missing eldritch theory changed research or knowledge");
                helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Missing eldritch theory consumed its plate");
                state.addKnowledge("THEORY", "ELDRITCH", 10);
            }

            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(state.knows(key), "Valid original requirements failed without a metal-block craft history");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Research must consume exactly one matching plate");
            helper.assertValueEqual(state.knowledge("THEORY", "ALCHEMY"), 7, "Research must spend one alchemy theory and preserve its remainder");
            if (material.equals("void")) helper.assertValueEqual(state.knowledge("THEORY", "ELDRITCH"), 9,
                    "Void smelter research must spend one eldritch theory and preserve its remainder");
            helper.assertTrue(state.crafted().equals(craftedBefore), "Research changed existing craft history or invented a metal craft");
            for (int i = 10; i < 13; i++) helper.assertValueEqual(player.getInventory().getItem(i).getCount(), 3,
                    "Research consumed an unrelated metal block");
            helper.assertValueEqual(player.totalExperience, 5, "Successful research must reward five XP once");
            before = ResearchCodec.write(state);
            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(before.equals(ResearchCodec.write(state)), "Replayed request changed completed research or knowledge");
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Replayed request consumed another plate");
            helper.assertValueEqual(player.totalExperience, 5, "Replayed request rewarded XP again");
        }
        helper.succeed();
    }

    private LegacyRequirementGameTests() {}
}
