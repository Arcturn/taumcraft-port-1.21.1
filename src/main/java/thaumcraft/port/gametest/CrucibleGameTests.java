package thaumcraft.port.gametest;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.machine.SpecialItemEntity;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.essentia.ThaumatoriumEntity;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

/** Real block, collision, recipe and capability paths; thermal steps stay in one server tick. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CrucibleGameTests {
    private static final BlockPos RELATIVE = new BlockPos(1, 3, 1);

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void originalAndModernHeatSourcesAllBoilOnThe151stTick(GameTestHelper helper) {
        var be = fixture(helper);
        List<BlockState> sources = List.of(Blocks.FIRE.defaultBlockState(),
                Blocks.SOUL_FIRE.defaultBlockState(), Blocks.LAVA.defaultBlockState(),
                Blocks.MAGMA_BLOCK.defaultBlockState(), MachineContent.NITOR.get("nitor_yellow").get().defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
                Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (var source : sources) {
            source(helper, be, source);
            be.state.restore(1000, 0, -100, Map.of());
            tick(helper, be, 150);
            helper.assertValueEqual(be.state.heat(), 150, "Unexpected warmup speed for " + source);
            helper.assertFalse(be.state.boiling(), "Water boiled before tick 151 for " + source);
            tick(helper, be, 1);
            helper.assertTrue(be.state.boiling(), "Water did not boil on tick 151 for " + source);
            helper.assertValueEqual(be.getUpdateTag(helper.getLevel().registryAccess()).getInt("heat"), 151,
                    "Client update state lost the exact boiling threshold");
            tick(helper, be, 80);
            helper.assertValueEqual(be.state.heat(), 200, "Source exceeded maximum heat for " + source);
            helper.assertValueEqual(be.state.water(), 1000, "Boiling incorrectly evaporated water for " + source);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void torchesAndUnlitOrWaterloggedCampfiresDoNotHeat(GameTestHelper helper) {
        var be = fixture(helper);
        List<BlockState> cold = List.of(Blocks.TORCH.defaultBlockState(), Blocks.SOUL_TORCH.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false),
                Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.WATERLOGGED, true),
                Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.WATERLOGGED, true),
                Blocks.STONE.defaultBlockState());
        for (var source : cold) {
            source(helper, be, source);
            be.state.restore(1000, 151, -100, Map.of());
            tick(helper, be, 1);
            helper.assertValueEqual(be.state.heat(), 150, "Non-source retained or increased heat: " + source);
            helper.assertFalse(be.state.boiling(), "Non-source failed to stop boiling: " + source);
            be.state.restore(1000, 0, -100, Map.of());
            tick(helper, be, 155);
            helper.assertValueEqual(be.state.heat(), 0, "Non-source heated cold water: " + source);
        }
        source(helper, be, Blocks.MAGMA_BLOCK.defaultBlockState());
        be.state.restore(0, 200, -100, Map.of());
        tick(helper, be, 1);
        helper.assertValueEqual(be.state.heat(), 199, "An empty crucible should cool even above a source");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void bucketRefillRetainsHeatAndSneakEmptyHandClearsContents(GameTestHelper helper) {
        var be = fixture(helper);
        var player = player(helper, be);
        be.state.restore(950, 180, -150, Map.of(Aspect.FIRE, 12, Aspect.FLUX, 4));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        click(player, be, Direction.UP);
        helper.assertValueEqual(be.state.water(), 950, "An indivisible vanilla bucket partly drained into only 50 mB of space");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "Failed partial bucket transfer lost its water");
        helper.assertValueEqual(be.state.heat(), 180, "Rejected partial fill changed heat");
        be.state.restore(0, 180, -150, Map.of(Aspect.FIRE, 12, Aspect.FLUX, 4));
        helper.assertTrue(click(player, be, Direction.UP).consumesAction(), "Water bucket click was not handled");
        helper.assertValueEqual(be.state.water(), 1000, "Bucket failed to refill a hot empty crucible");
        helper.assertValueEqual(be.state.heat(), 180, "Refill reset the existing heat");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Survival refill did not return the bucket");
        helper.assertValueEqual(be.state.total(), 16, "Refill changed stored aspects");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        var aura = AuraSystem.get(helper.getLevel(), be.getBlockPos());
        helper.assertTrue(aura != null, "Aura was not available for pollution assertion");
        float flux = aura.flux();
        helper.assertTrue(click(player, be, Direction.NORTH).consumesAction(), "Sneak empty-hand click was not handled");
        helper.assertValueEqual(be.state.water(), 0, "Sneak empty-hand did not remove water");
        helper.assertValueEqual(be.state.total(), 0, "Sneak empty-hand did not remove aspects");
        helper.assertValueEqual(be.state.heat(), 180, "Emptying incorrectly reset heat");
        helper.assertTrue(Math.abs(aura.flux() - flux - 7) < .001f,
                "Emptying did not apply the special Vitium pollution multiplier");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void topClickDissolvesOneItemAndColdOrSideClicksPreserveIt(GameTestHelper helper) {
        var be = fixture(helper);
        var player = player(helper, be);
        var input = new ItemStack(Items.IRON_INGOT, 4);
        var expected = RecipeAspects.get(input, helper.getLevel());
        helper.assertFalse(expected.isEmpty(), "Ingredient has no aspects in the actual recipe resolver");
        player.setItemInHand(InteractionHand.MAIN_HAND, input);
        be.state.restore(1000, 150, -100, Map.of());
        click(player, be, Direction.UP);
        helper.assertValueEqual(input.getCount(), 4, "Cold crucible consumed the input");
        be.state.restore(1000, 151, -100, Map.of());
        click(player, be, Direction.NORTH);
        helper.assertValueEqual(input.getCount(), 4, "Side click inserted an ingredient");
        helper.assertTrue(click(player, be, Direction.UP).consumesAction(), "Boiling top click was not accepted");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "Top click must insert exactly one ingredient");
        helper.assertValueEqual(be.state.aspects(), expected, "Top click did not dissolve the actual ingredient aspects");
        helper.assertValueEqual(be.state.water(), 1000, "Dissolving an ingredient spent crafting water");
        helper.assertValueEqual(be.state.timer(), -150, "Dissolution did not reset the original grace period");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void knownRecipeConsumesCatalystAndOutputDoesNotDissolveAgain(GameTestHelper helper) {
        var be = fixture(helper);
        var player = player(helper, be);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        be.state.restore(1000, 200, -100, Map.of(Aspect.ENERGY, 10, Aspect.FIRE, 10, Aspect.LIGHT, 10));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE_DUST, 2));
        helper.assertTrue(click(player, be, Direction.UP).consumesAction(), "Known catalyst click was not handled");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Recipe consumed more than one held catalyst");
        helper.assertValueEqual(be.state.water(), 950, "Recipe did not spend exactly 50 mB");
        helper.assertValueEqual(be.state.total(), 0, "Recipe did not spend its exact aspects");
        helper.assertValueEqual(be.state.timer(), -250, "Crafting did not apply the original grace period");
        var outputs = outputs(helper, be);
        helper.assertValueEqual(outputs.size(), 1, "Recipe did not create exactly one Nitor item entity");
        var output = outputs.getFirst();
        output.setPos(Vec3.atCenterOf(be.getBlockPos()));
        collide(helper, be, output);
        helper.assertTrue(output.isAlive(), "Crafted output dissolved again when it touched the crucible");
        helper.assertValueEqual(be.state.total(), 0, "Output re-entered the stored aspects");
        output.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void droppedCatalystUsesThrowersResearchAndUnownedItemsOnlyDissolve(GameTestHelper helper) {
        var be = fixture(helper);
        var player = player(helper, be);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        be.state.restore(1000, 200, -100, Map.of(Aspect.ENERGY, 20, Aspect.FIRE, 20, Aspect.LIGHT, 20));
        var dropped = new ItemEntity(helper.getLevel(), be.getBlockPos().getX() + .5,
                be.getBlockPos().getY() + .5, be.getBlockPos().getZ() + .5, new ItemStack(Items.GLOWSTONE_DUST, 2));
        dropped.setThrower(player);
        helper.getLevel().addFreshEntity(dropped);
        collide(helper, be, dropped);
        helper.assertTrue(dropped.isAlive(), "Original shrinking loop should leave half a two-item stack after one collision");
        helper.assertValueEqual(dropped.getItem().getCount(), 1, "First collision must process one of two catalysts");
        helper.assertValueEqual(be.state.water(), 950, "First collision spent more than one recipe's water");
        collide(helper, be, dropped);
        helper.assertFalse(dropped.isAlive(), "Owned catalyst stack was not fully consumed on collision");
        helper.assertValueEqual(be.state.water(), 900, "Two dropped catalysts did not make two recipes");
        helper.assertValueEqual(be.state.total(), 0, "Owned dropped catalyst ignored thrower research");
        var outputs = outputs(helper, be);
        helper.assertValueEqual(outputs.stream().mapToInt(e -> e.getItem().getCount()).sum(), 2,
                "Dropped stack created the wrong output count");
        outputs.forEach(ItemEntity::discard);

        be.state.restore(1000, 200, -100, Map.of(Aspect.ENERGY, 10, Aspect.FIRE, 10, Aspect.LIGHT, 10));
        var unowned = new ItemEntity(helper.getLevel(), be.getBlockPos().getX() + .5,
                be.getBlockPos().getY() + .5, be.getBlockPos().getZ() + .5, new ItemStack(Items.GLOWSTONE_DUST));
        helper.getLevel().addFreshEntity(unowned);
        collide(helper, be, unowned);
        helper.assertFalse(unowned.isAlive(), "Unowned ingredient should still dissolve");
        helper.assertValueEqual(be.state.water(), 1000, "Unowned catalyst crafted without a known player");
        helper.assertTrue(be.state.total() > 30, "Unowned catalyst did not contribute its ingredient aspects");
        helper.assertTrue(outputs(helper, be).isEmpty(), "Unowned catalyst bypassed research gating");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void craftedEntityRetainsItsTypeFireImmunityAndFloatingMotionAfterSave(GameTestHelper helper) {
        var be = fixture(helper);
        var item = new SpecialItemEntity(helper.getLevel(), be.getBlockPos().getX() + .5,
                be.getBlockPos().getY() + .71, be.getBlockPos().getZ() + .5, new ItemStack(Items.GOLD_INGOT, 3));
        item.setDeltaMovement(0, .075, 0);
        double initialY = item.getY();
        helper.assertTrue(helper.getLevel().noCollision(item, item.getBoundingBox().expandTowards(0, .2, 0)),
                "Floating-item fixture is obstructed at " + item.position() + " box=" + item.getBoundingBox()
                        + " block=" + helper.getLevel().getBlockState(item.blockPosition())
                        + " above=" + helper.getLevel().getBlockState(item.blockPosition().above()));
        item.tickCount = 1;
        item.tick();
        helper.assertTrue(item.getY() == initialY, "Crafted entity moved during its skipped first world tick");
        item.tickCount = 2;
        item.tick();
        helper.assertTrue(item.getY() > initialY && item.getDeltaMovement().y > 0 && item.getDeltaMovement().y < .075,
                "Crafted output did not rise gently: initialY=" + initialY + ", position=" + item.position()
                        + ", motion=" + item.getDeltaMovement() + ", tickCount=" + item.tickCount
                        + ", noGravity=" + item.isNoGravity() + ", removed=" + item.isRemoved()
                        + ", onGround=" + item.onGround() + ", fluid=" + item.getMaxHeightFluidType());
        helper.assertFalse(item.hurt(helper.getLevel().damageSources().lava(), 5), "Lava damaged a crafted special item");
        helper.assertFalse(item.hurt(helper.getLevel().damageSources().inFire(), 5), "Fire damaged a crafted special item");

        var saved = new CompoundTag();
        helper.assertTrue(item.save(saved), "Crafted entity could not be serialized with its registered type");
        var loaded = EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
        helper.assertTrue(loaded instanceof SpecialItemEntity, "Saved crafted entity reloaded as an ordinary dropped item");
        var restored = (SpecialItemEntity) loaded;
        helper.assertTrue(restored.getItem().is(Items.GOLD_INGOT) && restored.getItem().getCount() == 3,
                "Crafted stack was changed during entity save/load");
        helper.assertTrue(restored.getTags().contains(SpecialItemEntity.CRUCIBLE_OUTPUT_TAG),
                "Crafted output protection was lost on save/load");
        helper.assertFalse(restored.hurt(helper.getLevel().damageSources().lava(), 5),
                "Reloaded crafted entity lost its fire immunity");
        item.discard();
        restored.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void formedThaumatoriumUsesTheSameLitCampfireRules(GameTestHelper helper) {
        var crucible = fixture(helper);
        helper.setBlock(RELATIVE.above(), EssentiaContent.THAUMATORIUM.get());
        helper.setBlock(RELATIVE.above(2), EssentiaContent.THAUMATORIUM_TOP.get());
        var blockEntity = helper.getLevel().getBlockEntity(crucible.getBlockPos().above());
        helper.assertTrue(blockEntity instanceof ThaumatoriumEntity, "Fixture has no thaumatorium block entity");
        var machine = (ThaumatoriumEntity) blockEntity;
        helper.assertTrue(machine.formed(), "Thaumatorium fixture is incomplete");
        source(helper, crucible, Blocks.CAMPFIRE.defaultBlockState());
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(machine.heated, "Lit campfire heats the crucible but not the formed thaumatorium");
            source(helper, crucible, Blocks.SOUL_CAMPFIRE.defaultBlockState());
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(machine.heated, "Lit soul campfire failed to heat the formed thaumatorium");
            source(helper, crucible, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        });
        helper.runAfterDelay(18, () -> {
            helper.assertFalse(machine.heated, "Extinguished soul campfire still heated the thaumatorium");
            source(helper, crucible, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.WATERLOGGED, true));
        });
        helper.runAfterDelay(24, () -> {
            helper.assertFalse(machine.heated, "Waterlogged campfire still heated the thaumatorium");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void overflowAndPeriodicDecayBothRunOnTheSameTick(GameTestHelper helper) {
        var be = fixture(helper);
        source(helper, be, Blocks.MAGMA_BLOCK.defaultBlockState());
        be.state.restore(1000, 200, 99, Map.of(Aspect.FIRE, 502));
        var aura = AuraSystem.get(helper.getLevel(), be.getBlockPos());
        helper.assertTrue(aura != null, "Aura was not available for decay assertion");
        float flux = aura.flux();
        tick(helper, be, 1);
        helper.assertValueEqual(be.state.total(), 500, "Overflow suppressed the simultaneous timed aspect loss");
        helper.assertTrue(Math.abs(aura.flux() - flux - .5f) < .001f, "Two spills did not pollute the aura twice");
        helper.assertValueEqual(be.state.timer(), 0, "Periodic decay timer was not reset");
        helper.assertValueEqual(be.getBlockState().getAnalogOutputSignal(helper.getLevel(), be.getBlockPos()), 15,
                "Full aspect load did not expose the original comparator signal");
        be.state.restore(0, 0, 99, Map.of(Aspect.FLUX, 1));
        tick(helper, be, 1);
        helper.assertValueEqual(be.state.total(), 0, "Cold dry crucible incorrectly suspended aspect decay");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void waterCapabilitySupportsAutomationAndPreservesHeat(GameTestHelper helper) {
        var be = fixture(helper);
        be.state.restore(0, 175, -100, Map.of());
        for (var side : Direction.values()) {
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), side);
            helper.assertTrue(handler != null, "Crucible does not expose a fluid handler on " + side);
            helper.assertValueEqual(handler.getTankCapacity(0), 1000, "Fluid tank has the wrong capacity");
            helper.assertValueEqual(handler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), 0,
                    "Water tank accepted lava on " + side);
        }
        var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), Direction.UP);
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.SIMULATE), 1000,
                "Simulated fill did not respect tank capacity");
        helper.assertValueEqual(be.state.water(), 0, "Simulated fill changed the tank");
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.EXECUTE), 1000,
                "Automated water fill did not cap at 1000 mB");
        helper.assertValueEqual(be.state.heat(), 175, "Automated fill reset heat");
        helper.assertValueEqual(handler.drain(400, FluidAction.SIMULATE).getAmount(), 400, "Simulated drain returned the wrong amount");
        helper.assertValueEqual(be.state.water(), 1000, "Simulated drain changed the tank");
        helper.assertTrue(handler.drain(new FluidStack(Fluids.LAVA, 400), FluidAction.EXECUTE).isEmpty(),
                "Drain accepted the wrong requested fluid");
        helper.assertValueEqual(handler.drain(400, FluidAction.EXECUTE).getAmount(), 400, "Automated drain returned the wrong amount");
        helper.assertValueEqual(be.state.water(), 600, "Automated drain did not update stored water");
        helper.assertValueEqual(be.state.heat(), 175, "Automated drain reset heat");
        helper.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), 400,
                "A divisible fluid transfer did not top up the partly full tank");
        helper.assertValueEqual(be.state.water(), 1000, "Automated partial fill did not reach capacity");
        helper.assertValueEqual(be.state.heat(), 175, "Automated partial fill reset heat");
        helper.succeed();
    }

    private static CrucibleEntity fixture(GameTestHelper helper) {
        helper.setBlock(RELATIVE.below(2), Blocks.NETHERRACK);
        helper.setBlock(RELATIVE.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(RELATIVE, MachineContent.CRUCIBLE.get());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(RELATIVE));
        helper.assertTrue(be instanceof CrucibleEntity, "Fixture has no crucible block entity");
        return (CrucibleEntity) be;
    }

    private static ServerPlayer player(GameTestHelper helper, CrucibleEntity be) {
        var player = GameTestPlayers.create(helper);
        player.setPos(be.getBlockPos().getX() + .5, be.getBlockPos().getY(), be.getBlockPos().getZ() - 1.5);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        return player;
    }

    private static void source(GameTestHelper helper, CrucibleEntity be, BlockState state) {
        helper.getLevel().setBlock(be.getBlockPos().below(2),
                (state.is(Blocks.SOUL_FIRE) ? Blocks.SOUL_SAND : Blocks.NETHERRACK).defaultBlockState(), 2);
        helper.getLevel().setBlock(be.getBlockPos().below(), state, 2);
        helper.assertValueEqual(helper.getLevel().getBlockState(be.getBlockPos().below()), state,
                "Heat-source fixture did not retain the requested block state");
    }

    private static void tick(GameTestHelper helper, CrucibleEntity be, int count) {
        for (int i = 0; i < count; i++)
            CrucibleEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }

    private static InteractionResult click(ServerPlayer player, CrucibleEntity be, Direction face) {
        return player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(be.getBlockPos()), face, be.getBlockPos(), false));
    }

    private static void collide(GameTestHelper helper, CrucibleEntity be, ItemEntity item) {
        be.getBlockState().entityInside(helper.getLevel(), be.getBlockPos(), item);
    }

    private static List<ItemEntity> outputs(GameTestHelper helper, CrucibleEntity be) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(be.getBlockPos()).inflate(2),
                item -> item.isAlive() && item.getItem().is(MachineContent.NITOR.get("nitor_yellow").get().asItem()));
    }
}
