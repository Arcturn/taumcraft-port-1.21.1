package thaumcraft.port.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.SalisMundusRecipe;
import thaumcraft.port.integration.CuriosEvents;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.research.ResearchState;
import thaumcraft.port.research.Scanning;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    private static CraftingInput dustInput(Aspect third) {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(Items.BOWL), new ItemStack(Items.FLINT),
                new ItemStack(Items.REDSTONE), CrystalItem.stack(Aspect.AIR), CrystalItem.stack(Aspect.EARTH),
                CrystalItem.stack(third), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        return CraftingInput.of(3, 3, items);
    }

    @GameTest(template = "empty")
    public static void salisAcceptsDistinctCrystalsAndReturnsTools(GameTestHelper helper) {
        var recipe = new SalisMundusRecipe(CraftingBookCategory.MISC);
        var input = dustInput(Aspect.FIRE);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Valid legacy dust recipe rejected");
        helper.assertTrue(recipe.assemble(input, helper.getLevel().registryAccess()).is(PortContent.SALIS_MUNDUS.get()), "Wrong output");
        var remainder = recipe.getRemainingItems(input);
        helper.assertTrue(remainder.get(0).is(Items.BOWL), "Bowl not returned");
        helper.assertTrue(remainder.get(1).is(Items.FLINT), "Flint not returned");
        helper.assertTrue(remainder.get(2).isEmpty() && remainder.get(3).isEmpty(), "Ingredients incorrectly duplicated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void salisRejectsDuplicateAspects(GameTestHelper helper) {
        var recipe = new SalisMundusRecipe(CraftingBookCategory.MISC);
        helper.assertFalse(recipe.matches(dustInput(Aspect.AIR), helper.getLevel()), "Duplicate aspect accepted");
        helper.assertFalse(recipe.canCraftInDimensions(2, 2), "Recipe fitted into a player crafting grid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crystalComponentSeparatesStacks(GameTestHelper helper) {
        var air = CrystalItem.stack(Aspect.AIR);
        var fire = CrystalItem.stack(Aspect.FIRE);
        helper.assertFalse(ItemStack.isSameItemSameComponents(air, fire), "Different crystals can stack together");
        var encoded = fire.save(helper.getLevel().registryAccess());
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) encoded);
        helper.assertValueEqual(CrystalItem.aspect(loaded), Aspect.FIRE, "Crystal aspect lost after saving");
        var research = new ResearchState();
        research.recordScan(Scanning.itemSubject(air), AspectTags.forItem(air).keySet());
        research.recordScan(Scanning.itemSubject(fire), AspectTags.forItem(fire).keySet());
        helper.assertTrue(research.discovered().containsAll(List.of("aer", "ignis")),
                "Scanning one crystal prevented discovery of another aspect");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void researchPersistsAndCannotBeAwardedTwice(GameTestHelper helper) {
        var storage = new ResearchStorage();
        UUID id = UUID.randomUUID();
        storage.player(id).recordScan("item:minecraft:stone", List.of(Aspect.EARTH));
        var encoded = storage.save(new CompoundTag(), helper.getLevel().registryAccess());
        var loaded = ResearchStorage.load(encoded, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.player(id).discovered().contains("terra"), "Aspect lost during save/load");
        helper.assertFalse(loaded.player(id).recordScan("item:minecraft:stone", List.of(Aspect.EARTH)), "Duplicate scan rewarded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyAspectValuesAndCuriosTagsLoad(GameTestHelper helper) {
        helper.assertValueEqual(AspectTags.forItem(new ItemStack(Items.STONE)).get(Aspect.EARTH), 5, "Stone aspect changed");
        helper.assertValueEqual(AspectTags.forItem(new ItemStack(Items.IRON_INGOT)).get(Aspect.METAL), 15, "Iron tag aspect changed");
        helper.assertTrue(new ItemStack(PortContent.CHARM_UNDYING.get()).is(
                TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "charm"))), "Curios charm tag missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void salisTransformsBookshelf(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.BOOKSHELF);
        BlockPos pos = helper.absolutePos(relative);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.SALIS_MUNDUS.get(), 2));
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        PortContent.SALIS_MUNDUS.get().useOn(context);
        helper.assertBlockPresent(Blocks.BOOKSHELF, relative);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Incorrect dust consumption");
        Runnable assertCenteredBook = () -> {
            var books = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                    item -> item.isAlive() && item.getItem().is(PortContent.THAUMONOMICON.get()));
            helper.assertValueEqual(books.size(), 1, "Ritual must create exactly one Thaumonomicon");
            var book = books.getFirst();
            helper.assertTrue(book.position().distanceToSqr(Vec3.atCenterOf(pos)) < 1.0e-10,
                    "Thaumonomicon moved away from the center of the former bookshelf");
            helper.assertTrue(book.getDeltaMovement().lengthSqr() < 1.0e-10,
                    "Thaumonomicon received a drop/throw impulse");
            helper.assertTrue(book.isNoGravity(), "Thaumonomicon should hover at the ritual center");
        };
        helper.runAfterDelay(52, () -> {
            helper.assertBlockPresent(Blocks.AIR, relative);
            assertCenteredBook.run();
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Dust consumed twice");
        });
        helper.runAfterDelay(72, () -> {
            assertCenteredBook.run();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void undyingCharmConsumesOnlyOneAndPreventsDeath(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        var charms = inventory.getStacksHandler("charm").orElseThrow().getStacks();
        charms.setStackInSlot(0, new ItemStack(PortContent.CHARM_UNDYING.get()));
        player.setHealth(0);
        var event = new LivingDeathEvent(player, player.damageSources().generic());
        CuriosEvents.onDeath(event);
        helper.assertTrue(event.isCanceled(), "Equipped charm did not prevent death");
        helper.assertValueEqual(player.getHealth(), 1.0f, "Wrong restored health");
        helper.assertTrue(charms.getStackInSlot(0).isEmpty(), "Charm was not consumed");
        var second = new LivingDeathEvent(player, player.damageSources().generic());
        CuriosEvents.onDeath(second);
        helper.assertFalse(second.isCanceled(), "Consumed charm prevented a second death");
        helper.succeed();
    }
}
