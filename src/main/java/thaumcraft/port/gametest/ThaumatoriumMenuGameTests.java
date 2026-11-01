package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ThaumatoriumMenuGameTests {
    private static final ResourceLocation NITOR = ResourceLocation.parse("thaumcraft:crucible_nitor");
    private static final BlockPos POS = new BlockPos(1, 3, 1);

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void originalInventoryHasNoGhostSlotsAndShiftClickKeepsAllCatalysts(GameTestHelper helper) {
        var be = fixture(helper);
        var player = viewer(helper, be);
        var menu = new ThaumatoriumMenu(17, player.getInventory(), be);
        player.containerMenu = menu;
        helper.assertValueEqual(menu.slots.size(), 37, "Recipe icons became real inventory slots");
        helper.assertValueEqual(menu.getSlot(0).x, 55, "Wrong original catalyst X");
        helper.assertValueEqual(menu.getSlot(0).y, 24, "Wrong original catalyst Y");
        helper.assertValueEqual(menu.getSlot(1).y, 135, "Wrong original inventory Y");
        helper.assertValueEqual(menu.getSlot(28).y, 193, "Wrong original hotbar Y");
        player.getInventory().setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 9));
        helper.assertValueEqual(menu.quickMoveStack(player, 28).getCount(), 9, "Shift click ignored the real hotbar slot");
        helper.assertValueEqual(be.getItem(0).getCount(), 9, "Shift click lost catalysts");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "Catalyst duplicated in player inventory");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(be.isEmpty(), "Shift click failed to empty catalyst slot");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 9, "Original forward inventory transfer order changed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void selectionRejectsReplayForeignMenuAndStaleCatalystButAllowsProgressUpdates(GameTestHelper helper) {
        var be = fixture(helper);
        var player = viewer(helper, be);
        be.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 2));
        var menu = new ThaumatoriumMenu(18, player.getInventory(), be);
        player.containerMenu = menu;
        menu.snapshot();
        var request = new ThaumatoriumSelectPayload(menu.containerId, menu.token, menu.revision, NITOR);
        menu.select(player, new ThaumatoriumSelectPayload(19, menu.token, menu.revision, NITOR));
        menu.select(player, new ThaumatoriumSelectPayload(18, "wrong-token", menu.revision, NITOR));
        menu.select(player, new ThaumatoriumSelectPayload(18, menu.token, menu.revision, ResourceLocation.parse("thaumcraft:missing")));
        helper.assertTrue(be.selected.isEmpty(), "Spoofed selection changed the machine");
        be.contents.insert(Aspect.FIRE, 2, false);
        menu.snapshot();
        helper.assertValueEqual(menu.revision, request.revision(), "Essentia progress invalidated an in-flight recipe click");
        menu.select(player, request);
        helper.assertTrue(be.selected.containsKey(NITOR), "Valid selection was rejected");
        menu.select(player, request);
        helper.assertTrue(be.selected.containsKey(NITOR), "Replayed packet toggled the recipe a second time");
        menu.select(player, new ThaumatoriumSelectPayload(18, menu.token, menu.revision, NITOR));
        helper.assertTrue(be.selected.isEmpty(), "Fresh selected-recipe click did not remove it");
        var stale = new ThaumatoriumSelectPayload(18, menu.token, menu.revision, NITOR);
        be.setItem(0, new ItemStack(Items.DIRT));
        menu.select(player, stale);
        helper.assertTrue(be.selected.isEmpty(), "Stale catalyst click selected an unavailable recipe");
        helper.assertValueEqual(be.contents.amount(Aspect.FIRE), 2, "Selection changes discarded accumulated essentia");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void serverSnapshotRestoresOutputsSelectedOrderAndPerAspectProgress(GameTestHelper helper) {
        var be = fixture(helper);
        var player = viewer(helper, be);
        be.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
        be.choose(player, NITOR);
        be.contents.insert(Aspect.FIRE, 7, false);
        be.contents.insert(Aspect.LIGHT, 3, false);
        var server = new ThaumatoriumMenu(19, player.getInventory(), be);
        var client = new ThaumatoriumMenu(19, player.getInventory(), new SimpleContainer(1));
        client.receive(server.token, server.snapshot());
        helper.assertValueEqual(client.selected, java.util.List.of(NITOR), "Client lost selected recipe order");
        helper.assertTrue(client.catalystPresent, "Client cannot select localized recipe sorting without catalyst state");
        helper.assertValueEqual(client.capacity, be.capacity, "Client lost upgrade capacity");
        helper.assertValueEqual(client.contents.get(Aspect.FIRE), Integer.valueOf(7), "Client lost stored fire");
        helper.assertValueEqual(client.contents.get(Aspect.LIGHT), Integer.valueOf(3), "Client lost stored light");
        var recipe = client.recipes.stream().filter(r -> r.id().equals(NITOR)).findFirst().orElseThrow();
        helper.assertTrue(recipe.output().is(MachineContent.NITOR.get("nitor_yellow").get().asItem()), "Wrong recipe output preview");
        helper.assertValueEqual(recipe.aspects().get(Aspect.FIRE), Integer.valueOf(10), "Client lost required essentia quantity");
        be.setItem(0, ItemStack.EMPTY);
        client.receive(server.token, server.snapshot());
        helper.assertValueEqual(client.recipes.size(), 1, "Selected recipe disappeared when catalyst ran out");
        helper.assertFalse(client.catalystPresent, "Client must preserve saved selection order when catalyst is empty");
        helper.succeed();
    }

    private static ThaumatoriumEntity fixture(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(POS);
        level.setBlock(pos.below(), MachineContent.CRUCIBLE.get().defaultBlockState(), 2);
        level.setBlock(pos, EssentiaContent.THAUMATORIUM.get().defaultBlockState(), 2);
        level.setBlock(pos.above(), EssentiaContent.THAUMATORIUM_TOP.get().defaultBlockState(), 2);
        return (ThaumatoriumEntity)level.getBlockEntity(pos);
    }

    private static ServerPlayer viewer(GameTestHelper helper, ThaumatoriumEntity be) {
        var player = GameTestPlayers.create(helper);
        player.setPos(be.getBlockPos().getX() + .5, be.getBlockPos().getY(), be.getBlockPos().getZ() + .5);
        ResearchStorage.get(player.getServer()).player(player.getUUID()).stage("UNLOCKALCHEMY", 3, false);
        return player;
    }
    private ThaumatoriumMenuGameTests() {}
}
