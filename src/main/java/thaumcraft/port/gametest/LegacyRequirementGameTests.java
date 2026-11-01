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

/** Actual loaded common tags, matching Forge 1.12.2's OreDictionary chest family. */
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
    public static void smelterResearchRequiresItsOwnMetalAndConsumesPlateAndTheoryOnce(GameTestHelper helper) {
        for (String material : java.util.List.of("thaumium", "void")) {
            String key = material.equals("thaumium") ? "ESSENTIASMELTERTHAUMIUM" : "ESSENTIASMELTERVOID";
            var player = GameTestPlayers.create(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.THAUMONOMICON.get()));
            var state = ResearchStorage.get(player.getServer()).player(player.getUUID());
            state.flag("TUBES");
            state.stage("METALLURGY", 3);
            if (material.equals("void")) {
                state.flag("ESSENTIASMELTERTHAUMIUM");
                state.flag("BASEELDRITCH");
                state.addKnowledge("THEORY", "ELDRITCH", 32);
            }
            state.stage(key, 1);
            state.addKnowledge("THEORY", "ALCHEMY", 32);
            var plate = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:plate_" + material));
            player.getInventory().setItem(9, new ItemStack(plate, 2));
            String wrongMetal = material.equals("thaumium") ? "brass" : "thaumium";
            ResearchProgress.craft(player, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("thaumcraft:metal_" + wrongMetal))));
            var before = ResearchCodec.write(state);
            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(before.equals(ResearchCodec.write(state)), "Crafting the previous metal unlocked " + key);
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 2, "Failed research consumed its plate");
            ResearchProgress.craft(player, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("thaumcraft:metal_" + material))));
            ResearchProgress.advance(player, key, 1);
            helper.assertTrue(state.knows(key), "The correct metal block did not satisfy " + key);
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Research must consume exactly one matching plate");
            helper.assertValueEqual(state.knowledge("THEORY", "ALCHEMY"), 0, "Research must spend one alchemy theory");
            if (material.equals("void")) helper.assertValueEqual(state.knowledge("THEORY", "ELDRITCH"), 0,
                    "Void smelter research must also spend one eldritch theory");
            helper.assertValueEqual(player.totalExperience, 5, "Successful research must reward five XP once");
            ResearchProgress.advance(player, key, 1);
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, "Replayed request consumed another plate");
            helper.assertValueEqual(player.totalExperience, 5, "Replayed request rewarded XP again");
        }
        helper.succeed();
    }

    private LegacyRequirementGameTests() {}
}
