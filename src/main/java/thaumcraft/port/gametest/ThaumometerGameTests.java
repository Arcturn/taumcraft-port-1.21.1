package thaumcraft.port.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.ResearchCodec;
import thaumcraft.port.research.ResearchState;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.research.Scanner;

/** Actual item-use paths, server ray casts and player inventory/research, without a client login. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ThaumometerGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void blockUseAcceptsAnotherTargetImmediatelyAndRepeatHasNoReward(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = fixture.player();
            var target = fixture.origin.offset(0, 1, 3);
            var state = state(player);
            fixture.set(target, Blocks.DIRT.defaultBlockState());
            var firstUse = useOn(player, target);
            helper.assertTrue(firstUse.consumesAction(), "Scanner fallback did not consume item use");
            helper.assertFalse(firstUse.shouldSwing(), "Scanner requested a modern arm swing absent from original ordinary item use");
            helper.assertTrue(state.scanned().contains("item:minecraft:dirt"),
                    "Vanilla block-use fallback did not scan the real ray-cast block");
            assertFeedback(helper, player, 0, "tc.knownobject", ChatFormatting.GREEN);
            helper.assertFalse(player.getCooldowns().isOnCooldown(PortContent.THAUMOMETER.get()),
                    "Scanning added a cooldown absent from the original item");

            // Both uses occur in one server tick. A hard-coded cooldown used to discard this scan.
            fixture.set(target, Blocks.DIAMOND_BLOCK.defaultBlockState());
            useOn(player, target);
            helper.assertTrue(state.scanned().contains("item:minecraft:diamond_block"),
                    "A second new target in the same tick was silently rejected");
            assertFeedback(helper, player, 1, "tc.knownobject", ChatFormatting.GREEN);
            var beforeRepeat = ResearchCodec.write(state);
            useOn(player, target);
            assertFeedback(helper, player, 2, "tc.unknownobject", ChatFormatting.DARK_PURPLE);
            helper.assertValueEqual(ResearchCodec.write(state), beforeRepeat,
                    "Repeated scan changed research or awarded observation again");
            helper.assertValueEqual(player.messages.size(), 3, "An item use produced duplicate scan feedback");
            System.out.println("THAUMOMETER_USE same_tick_new_targets=2 repeat_reward=false styled_feedback=3");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void containerScanVisitsOccupiedSlotsAndNeverExtractsItems(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = fixture.player();
            var target = fixture.origin.offset(0, 1, 3);
            fixture.set(target, Blocks.BARREL.defaultBlockState());
            var barrel = (BarrelBlockEntity) fixture.level.getBlockEntity(target);
            helper.assertTrue(barrel != null, "Container fixture has no block entity");
            barrel.setItem(0, new ItemStack(Items.DIAMOND, 64));
            barrel.setItem(13, new ItemStack(Items.IRON_INGOT, 7));
            barrel.setItem(26, new ItemStack(Items.DIAMOND, 2));
            var before = new ArrayList<ItemStack>();
            for (int i = 0; i < barrel.getContainerSize(); i++) before.add(barrel.getItem(i).copy());
            helper.assertTrue(fixture.level.getCapability(Capabilities.ItemHandler.BLOCK, target, Direction.UP) != null,
                    "Container does not expose the original scanner's UP-facing item capability");

            player.setShiftKeyDown(true);
            useOn(player, target);
            helper.assertValueEqual(player.messages.size(), 4,
                    "Scanner must emit one block message and one message per occupied slot, not per item");
            assertFeedback(helper, player, 1, "tc.knownobject", ChatFormatting.GREEN);
            assertFeedback(helper, player, 2, "tc.knownobject", ChatFormatting.GREEN);
            assertFeedback(helper, player, 3, "tc.unknownobject", ChatFormatting.DARK_PURPLE);
            helper.assertTrue(state(player).scanned().containsAll(List.of("item:minecraft:diamond", "item:minecraft:iron_ingot")),
                    "Container contents did not reach normal item scanning");
            helper.assertTrue(state(player).knows("f_MATIRON"), "Container scan skipped material discovery triggers");
            for (int i = 0; i < before.size(); i++) {
                var after = barrel.getItem(i);
                helper.assertTrue(ItemStack.isSameItemSameComponents(before.get(i), after)
                                && before.get(i).getCount() == after.getCount(),
                        "Scanner extracted or changed container slot " + i);
            }
            System.out.println("THAUMOMETER_CONTAINER occupied_slots=3 messages=4 unchanged_slots=" + before.size());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void ordinaryInteractionsTakePriorityAndSneakingContainerFallsBackToScan(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var target = fixture.origin.offset(0, 1, 3);
            fixture.set(target, Blocks.BARREL.defaultBlockState());
            var barrel = (BarrelBlockEntity) fixture.level.getBlockEntity(target);
            helper.assertTrue(barrel != null, "Interaction fixture has no barrel block entity");
            barrel.setItem(0, new ItemStack(Items.DIAMOND, 3));

            var ordinary = fixture.player();
            var ordinaryResearch = ResearchCodec.write(state(ordinary));
            helper.assertTrue(useOn(ordinary, target).consumesAction(), "Normal barrel click was not handled");
            helper.assertValueEqual(ordinary.menuOpenAttempts, 1, "Thaumometer prevented the normal barrel menu");
            helper.assertTrue(ordinary.containerMenu instanceof ChestMenu, "Barrel did not create its actual inventory menu");
            helper.assertValueEqual(ordinary.messages.size(), 0, "Opening a barrel also scanned it");
            helper.assertValueEqual(ResearchCodec.write(state(ordinary)), ordinaryResearch,
                    "Normal barrel interaction granted scan research");
            ordinary.closeContainer();

            var sneaking = fixture.player();
            sneaking.setShiftKeyDown(true);
            helper.assertTrue(useOn(sneaking, target).consumesAction(), "Sneaking barrel fallback did not use the item");
            helper.assertValueEqual(sneaking.menuOpenAttempts, 0, "Sneaking scan opened the barrel menu");
            helper.assertTrue(state(sneaking).scanned().contains("item:minecraft:diamond"),
                    "Sneaking past the barrel interaction did not scan its contents");
            helper.assertValueEqual(sneaking.messages.size(), 2, "Sneaking fallback did not scan block and occupied slot once");
            helper.assertValueEqual(barrel.getItem(0).getCount(), 3, "Sneaking scan changed barrel contents");

            // Player.interactOn includes NeoForge's EntityInteract event. A blanket scanner
            // cancellation used to consume this click before ItemFrame.interact could insert it.
            var framePos = fixture.origin.offset(2, 1, 1);
            fixture.set(framePos.south(), Blocks.STONE.defaultBlockState());
            var frame = new ItemFrame(fixture.level, framePos, Direction.NORTH);
            fixture.spawn(frame);
            var framePlayer = fixture.player();
            var frameResearch = ResearchCodec.write(state(framePlayer));
            var result = framePlayer.interactOn(frame, InteractionHand.MAIN_HAND);
            if (result == InteractionResult.PASS) use(framePlayer);
            helper.assertTrue(result.consumesAction(), "Normal item-frame interaction was not handled");
            helper.assertTrue(frame.getItem().is(PortContent.THAUMOMETER.get()),
                    "Scanner intercepted item-frame insertion instead of preserving the normal interaction");
            helper.assertTrue(framePlayer.getMainHandItem().isEmpty(), "Item frame did not consume the inserted tool");
            helper.assertValueEqual(framePlayer.messages.size(), 0, "Item-frame interaction also scanned a target");
            helper.assertValueEqual(ResearchCodec.write(state(framePlayer)), frameResearch,
                    "Item-frame interaction granted scan research");
            System.out.println("THAUMOMETER_DISPATCH barrel_opens=true sneak_scans=true item_frame_inserts=true forced_entity_scan=false");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void entityRayIncludesDroppedItemsAndProjectilesButRejectsWallsAndRange(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = fixture.player();
            var item = new ItemEntity(fixture.level, player.getX(), player.getY() + 1.4,
                    player.getZ() + 3, new ItemStack(Items.DIAMOND));
            item.setNoGravity(true);
            fixture.spawn(item);
            var arrow = EntityType.ARROW.create(fixture.level);
            helper.assertTrue(arrow != null, "Projectile fixture could not be created");
            arrow.setPos(player.getX(), player.getY() + 1.4, player.getZ() + 6);
            arrow.setNoGravity(true);
            fixture.spawn(arrow);
            var target = Scanner.entityTarget(player, 9, 0);
            helper.assertTrue(target != null && target.getEntity() == item,
                    "Entity ray did not select the nearest non-colliding dropped item");
            use(player);
            helper.assertTrue(state(player).scanned().contains("item:minecraft:diamond"),
                    "Dropped item was scanned as a generic entity instead of its stack");

            item.discard();
            target = Scanner.entityTarget(player, 9, 0);
            helper.assertTrue(target != null && target.getEntity() == arrow,
                    "Scanner ignored a nonliving projectile beyond normal block reach");
            use(player);
            helper.assertTrue(state(player).knows("f_arrow"), "Projectile scan failed its independent discovery trigger");

            var wall = fixture.origin.offset(0, 1, 3);
            fixture.set(wall, Blocks.STONE.defaultBlockState());
            var blockedPlayer = fixture.player();
            helper.assertTrue(Scanner.entityTarget(blockedPlayer, 9, 0) == null,
                    "Scanner selected an entity through an opaque wall");
            use(blockedPlayer);
            helper.assertFalse(state(blockedPlayer).knows("f_arrow"), "A wall-blocked projectile granted research");
            fixture.set(wall, Blocks.AIR.defaultBlockState());
            arrow.setPos(player.getX(), player.getY() + 1.4, player.getZ() + 11);
            helper.assertTrue(Scanner.entityTarget(player, 9, 0) == null,
                    "Scanner reached an entity outside its nine-block ray");
            System.out.println("THAUMOMETER_ENTITY dropped_item=true projectile_trigger=true wall_rejected=true range_rejected=true");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void ordinaryMissAndIncompleteOrObstructedSkyUseUnknownFeedback(GameTestHelper helper) {
        var level = helper.getLevel();
        long previousDayTime = level.getDayTime();
        var fixture = new Fixture(helper);
        var player = fixture.player();
        try {
            level.setDayTime(18000);
            player.setXRot(-45);
            assertMiss(helper, player);
            use(player);
            assertFeedback(helper, player, 0, "tc.unknownobject", ChatFormatting.DARK_PURPLE);

            state(player).stage("CELESTIALSCANNING", 1, false);
            player.messages.clear();
            use(player);
            assertFeedback(helper, player, 0, "tc.unknownobject", ChatFormatting.DARK_PURPLE);
            helper.assertValueEqual(player.messages.size(), 1, "Incomplete celestial research handled an ordinary miss");

            state(player).flag("CELESTIALSCANNING");
            helper.assertTrue(level.canSeeSky(player.blockPosition().above()), "Downward-aim fixture lacks open sky");
            player.setXRot(45);
            assertMiss(helper, player);
            player.messages.clear();
            use(player);
            assertFeedback(helper, player, 0, "tc.unknownobject", ChatFormatting.DARK_PURPLE);
            // A broad roof shades the player. The shallow ray stays below it throughout block reach.
            player.setXRot(-10);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                fixture.set(fixture.origin.offset(x, 3, z), Blocks.STONE.defaultBlockState());
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        } finally {
            level.setDayTime(previousDayTime);
        }
        // canSeeSky reads the SKY light layer in 1.21.1. Block placement queues its propagation;
        // evaluating it in the placement tick incorrectly sees the previous open-sky light values.
        helper.runAfterDelay(10, () -> {
            long callbackDayTime = level.getDayTime();
            try {
                level.setDayTime(18000);
                helper.assertFalse(level.canSeeSky(player.blockPosition().above()), "Roof fixture did not block the sky after lighting updates");
                assertMiss(helper, player);
                player.messages.clear();
                use(player);
                assertFeedback(helper, player, 0, "tc.unknownobject", ChatFormatting.DARK_PURPLE);
                helper.assertValueEqual(player.messages.size(), 1, "Obstructed sky emitted celestial feedback");
                helper.succeed();
            } finally {
                fixture.close();
                level.setDayTime(callbackDayTime);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void celestialScanConsumesOnePaperSuppressesGenericStatusAndRejectsDailyRepeat(GameTestHelper helper) {
        var level = helper.getLevel();
        long previousDayTime = level.getDayTime();
        try (var fixture = new Fixture(helper)) {
            level.setDayTime(18000);
            var player = fixture.player();
            player.setXRot(-45);
            var state = state(player);
            state.flag("CELESTIALSCANNING");
            long day = level.getGameTime() / 24000;
            String oldKey = "CEL_" + (day - 1) + "_Sun";
            state.flag(oldKey);
            helper.assertTrue(level.canSeeSky(player.blockPosition().above()), "Celestial fixture lacks open sky");
            assertMiss(helper, player);
            use(player);
            assertMessageKey(helper, player, 0, "tc.celestial.fail.2");
            helper.assertValueEqual(player.messages.size(), 1, "Missing materials also emitted generic scan feedback");
            helper.assertFalse(state.knows(oldKey), "Missing-material sky scan failed to prune older celestial keys");
            helper.assertValueEqual(count(player, PortContent.CELESTIAL_NOTES.get()), 0, "Missing materials created a note");

            player.getInventory().setItem(9, new ItemStack(Items.PAPER, 3));
            var tools = new ItemStack(PortContent.SCRIBING_TOOLS.get());
            tools.setDamageValue(37);
            player.getInventory().setItem(10, tools);
            player.messages.clear();
            use(player);
            helper.assertValueEqual(count(player, Items.PAPER), 2, "Successful celestial scan did not consume exactly one paper");
            helper.assertValueEqual(count(player, PortContent.CELESTIAL_NOTES.get()), 1, "Successful celestial scan did not create exactly one note");
            helper.assertValueEqual(tools.getDamageValue(), 37, "Celestial scan consumed ink durability absent from original");
            helper.assertTrue(state.knows("CEL_" + day + "_Star1"), "Midnight south-facing scan did not record the southern stars");
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.is(PortContent.CELESTIAL_NOTES.get()))
                    helper.assertValueEqual(stack.getOrDefault(PortContent.CELESTIAL_KIND.get(), -1), 2,
                            "Southern-star celestial note has incorrect metadata");
            }
            helper.assertValueEqual(player.messages.size(), 0, "Successful celestial scan emitted a generic item-scan status");

            use(player);
            assertMessageKey(helper, player, 0, "tc.celestial.fail.1");
            helper.assertValueEqual(player.messages.size(), 1, "Repeated celestial scan duplicated feedback");
            helper.assertValueEqual(count(player, Items.PAPER), 2, "Daily repeat consumed another paper");
            helper.assertValueEqual(count(player, PortContent.CELESTIAL_NOTES.get()), 1, "Daily repeat duplicated a celestial note");
            System.out.println("THAUMOMETER_SKY missing_materials=handled old_keys_pruned=true note_count=1 paper_consumed=1 duplicate_rejected=true");
        } finally {
            level.setDayTime(previousDayTime);
        }
        helper.succeed();
    }

    private static void use(CapturePlayer player) {
        PortContent.THAUMOMETER.get().use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static InteractionResult useOn(CapturePlayer player, BlockPos pos) {
        var result = player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        // Minecraft.startUseItem falls back to ordinary item use only when the block path passes.
        if (result == InteractionResult.PASS)
            return player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND).getResult();
        return result;
    }

    private static void assertMiss(GameTestHelper helper, CapturePlayer player) {
        helper.assertValueEqual(Scanner.target(player, 9, player.blockInteractionRange()).getType(), HitResult.Type.MISS,
                "Sky/ordinary-miss fixture intersects a block or entity");
    }

    private static ResearchState state(CapturePlayer player) {
        return ResearchStorage.get(player.getServer()).player(player.getUUID());
    }

    private static int count(CapturePlayer player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void assertMessageKey(GameTestHelper helper, CapturePlayer player, int index, String key) {
        helper.assertTrue(player.messages.size() > index, "Missing feedback " + key);
        var message = player.messages.get(index);
        helper.assertTrue(message.actionBar, "Scanner message was sent to chat instead of the action bar");
        helper.assertTrue(message.component.getContents() instanceof TranslatableContents translated
                        && translated.getKey().equals(key), "Wrong scan feedback; expected " + key + ", got " + message.component);
    }

    private static void assertFeedback(GameTestHelper helper, CapturePlayer player, int index, String key, ChatFormatting color) {
        assertMessageKey(helper, player, index, key);
        var style = player.messages.get(index).component.getStyle();
        helper.assertValueEqual(style.getColor(), TextColor.fromLegacyFormat(color), "Original scan feedback color changed");
        helper.assertTrue(style.isItalic(), "Original scan feedback must be italic");
    }

    private record Message(Component component, boolean actionBar) {}

    private static final class CapturePlayer extends FakePlayer {
        private final List<Message> messages = new ArrayList<>();
        private int menuOpenAttempts;

        CapturePlayer(ServerLevel level, BlockPos origin) {
            super(level, new GameProfile(UUID.randomUUID(), "tc-scanner-test"));
            setGameMode(GameType.SURVIVAL);
            setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            setYRot(0);
            setXRot(0);
            setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PortContent.THAUMOMETER.get()));
        }

        @Override public void displayClientMessage(Component message, boolean actionBar) {
            messages.add(new Message(message.copy(), actionBar));
        }

        @Override public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
            menuOpenAttempts++;
            if (provider == null) return OptionalInt.empty();
            var menu = provider.createMenu(menuOpenAttempts, getInventory(), this);
            if (menu == null) return OptionalInt.empty();
            containerMenu = menu;
            return OptionalInt.of(menu.containerId);
        }
    }

    /** Synchronous elevated fixtures cannot overlap the ordinary ground-level GameTests. */
    private static final class Fixture implements AutoCloseable {
        private final ServerLevel level;
        private final BlockPos origin;
        private final Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
        private final List<Entity> entities = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            level = helper.getLevel();
            var base = helper.absolutePos(new BlockPos(1, 0, 1));
            origin = new BlockPos(base.getX(), level.getMaxBuildHeight() - 20, base.getZ());
        }

        CapturePlayer player() { return new CapturePlayer(level, origin); }

        void set(BlockPos pos, BlockState state) {
            originalBlocks.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            level.setBlock(pos, state, 3);
        }

        void spawn(Entity entity) {
            entities.add(entity);
            if (!level.addFreshEntity(entity)) throw new AssertionError("Cannot add scanner target entity");
        }

        @Override public void close() {
            entities.forEach(Entity::discard);
            originalBlocks.forEach((pos, state) -> level.setBlock(pos, state, 3));
        }
    }

    private ThaumometerGameTests() {}
}
