package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.alchemy.liquid.AlchemyLiquidEvents;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.research.ResearchCodec;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.WorldContent;
import thaumcraft.port.world.WorldEntities;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AlchemyLiquidGameTests {
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void droppedSaltsDissolveAfterTenSecondsIntoARealBathSource(GameTestHelper helper) {
        BlockPos p = helper.absolutePos(new BlockPos(2, 2, 2));
        basin(helper, p);
        helper.getLevel().setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
        var salts = new ItemEntity(helper.getLevel(), p.getX()+.5, p.getY()+.2, p.getZ()+.5,
                new ItemStack(AlchemyLiquids.BATH_SALTS.get()));
        salts.setNoPickUpDelay(); helper.getLevel().addFreshEntity(salts);
        helper.runAtTickTime(190, () -> helper.assertTrue(helper.getLevel().getBlockState(p).is(Blocks.WATER),
                "Salts converted water before their original 200-tick lifespan"));
        helper.runAtTickTime(205, () -> {
            helper.assertTrue(salts.isRemoved(), "Expired salts remained in the world");
            helper.assertTrue(helper.getLevel().getBlockState(p).is(AlchemyLiquids.PURIFYING_BATH_BLOCK.get()), "Salts did not create purifying fluid");
            helper.assertTrue(helper.getLevel().getFluidState(p).isSource(), "Salts created flowing fluid instead of a source");
            helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void saltsCannotConvertFlowingWaterOrWaterloggedBlocks(GameTestHelper helper) {
        BlockPos p = helper.absolutePos(new BlockPos(2, 2, 2));
        var salts = new ItemEntity(helper.getLevel(), p.getX()+.5, p.getY()+.2, p.getZ()+.5,
                new ItemStack(AlchemyLiquids.BATH_SALTS.get()));
        var flow = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
        helper.getLevel().setBlockAndUpdate(p, flow);
        AlchemyLiquidEvents.expire(new ItemExpireEvent(salts));
        helper.assertTrue(helper.getLevel().getBlockState(p).equals(flow), "Salts converted flowing water");
        var logged = Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        helper.getLevel().setBlockAndUpdate(p, logged);
        AlchemyLiquidEvents.expire(new ItemExpireEvent(salts));
        helper.assertTrue(helper.getLevel().getBlockState(p).equals(logged), "Salts destroyed a waterlogged block");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void bathRequiresSourceConsumesItAndNeverRefreshesExistingWard(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        var state = ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        state.warp(400, 10, 20);
        var p = player.blockPosition();
        var source = AlchemyLiquids.PURIFYING_BATH_BLOCK.get().defaultBlockState();
        var flow = source.setValue(LiquidBlock.LEVEL, 1);
        helper.getLevel().setBlockAndUpdate(p, flow);
        flow.entityInside(helper.getLevel(), p, player);
        helper.assertTrue(!player.hasEffect(AlchemyLiquids.WARP_WARD), "Flowing bath granted ward");
        helper.getLevel().setBlockAndUpdate(p, source);
        source.entityInside(helper.getLevel(), p, player);
        helper.assertTrue(player.hasEffect(AlchemyLiquids.WARP_WARD), "Bath source did not grant ward");
        helper.assertValueEqual(player.getEffect(AlchemyLiquids.WARP_WARD).getDuration(), 10000, "Ward ignored permanent warp");
        helper.assertTrue(helper.getLevel().isEmptyBlock(p), "Used bath source remained");
        helper.getLevel().setBlockAndUpdate(p, source);
        source.entityInside(helper.getLevel(), p, player);
        helper.assertTrue(helper.getLevel().getBlockState(p).is(AlchemyLiquids.PURIFYING_BATH_BLOCK.get()), "Existing ward consumed another bath");
        helper.assertValueEqual(state.normalWarp(), 10, "Bath directly reduced normal warp");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void soapOnlyFinishesAfterNinetySixTicksAndStacksBothBathBonuses(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        var state = ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        state.warp(20, 10, 30);
        var soap = new ItemStack(AlchemyLiquids.SANITY_SOAP.get(), 2);
        AlchemyLiquids.SANITY_SOAP.get().releaseUsing(soap, helper.getLevel(), player, 5);
        helper.assertValueEqual(soap.getCount(), 2, "An interrupted wash consumed soap");
        helper.assertValueEqual(state.temporaryWarp(), 30, "An interrupted wash cleansed warp");
        player.addEffect(new MobEffectInstance(AlchemyLiquids.WARP_WARD, 200));
        helper.getLevel().setBlockAndUpdate(player.blockPosition(), AlchemyLiquids.PURIFYING_BATH_BLOCK.get().defaultBlockState());
        AlchemyLiquids.SANITY_SOAP.get().releaseUsing(soap, helper.getLevel(), player, 4);
        helper.assertValueEqual(soap.getCount(), 1, "Completed wash did not consume one soap");
        helper.assertValueEqual(state.permanentWarp(), 20, "Soap removed permanent warp");
        helper.assertValueEqual(state.normalWarp(), 7, "Soap did not combine base, ward and bath reductions");
        helper.assertValueEqual(state.temporaryWarp(), 0, "Soap did not remove all temporary warp");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void holdingSoapRunsTheRealUseLifecycleAndConsumesItExactlyOnce(GameTestHelper helper) {
        var player = new WashingPlayer(helper);
        var state = ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        state.warp(4, 8, 13);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlchemyLiquids.SANITY_SOAP.get(), 2));
        AlchemyLiquids.SANITY_SOAP.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 96; tick++) player.useTick();
        helper.assertTrue(player.isUsingItem(), "Soap stopped before 96 elapsed use ticks");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "Soap was consumed before the wash finished");
        player.useTick();
        helper.assertTrue(!player.isUsingItem(), "Finished soap did not release the active hand");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "Use completion consumed soap more than once");
        helper.assertValueEqual(state.normalWarp(), 7, "Dry washing did not remove one normal warp");
        helper.assertValueEqual(state.temporaryWarp(), 0, "Use lifecycle failed to remove temporary warp");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void deathBypassesArmorDropsCrystalsAndPreservesOrdinaryLoot(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2)); cow.setNoAi(true);
        cow.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var p = cow.blockPosition();
        var source = AlchemyLiquids.DEATH_BLOCK.get().defaultBlockState();
        float health = cow.getHealth();
        source.entityInside(helper.getLevel(), p, cow);
        helper.assertValueEqual(cow.getHealth(), health - 5, "Liquid Death did not bypass armor for five source damage");
        cow.invulnerableTime = 0;
        cow.hurt(new DamageSource(helper.getLevel().registryAccess().holderOrThrow(AlchemyLiquids.DISSOLVE)), 100);
        var loot = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2));
        helper.assertTrue(loot.stream().anyMatch(e -> e.getItem().is(PortContent.CRYSTAL_ESSENCE.get())), "Dissolved creature yielded no aspect crystals");
        helper.assertTrue(loot.stream().anyMatch(e -> e.getItem().is(Items.BEEF)), "Dissolve replaced ordinary creature loot");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void bothOriginalFluidsCanBeCollectedInBuckets(GameTestHelper helper) {
        var p = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var block : java.util.List.of(AlchemyLiquids.DEATH_BLOCK, AlchemyLiquids.PURIFYING_BATH_BLOCK)) {
            var source = block.get().defaultBlockState(); helper.getLevel().setBlockAndUpdate(p, source);
            var bucket = block.get().pickupBlock(null, helper.getLevel(), p, source);
            helper.assertTrue(bucket.is(source.getFluidState().getType().getBucket()), "Source returned the wrong bucket");
            helper.assertTrue(helper.getLevel().isEmptyBlock(p), "Collecting a bucket duplicated the fluid");
        }
        helper.succeed();
    }
    @GameTest(template = "alchemy_liquid_empty")
    public static void taintBottleSplashAffectsLivingTargetsButNotUndeadOrTaintedCreatures(GameTestHelper helper) {
        var p = helper.absolutePos(new BlockPos(5, 2, 5));
        for (int x=-4;x<=4;x++) for (int z=-4;z<=4;z++) helper.getLevel().setBlockAndUpdate(p.offset(x,-1,z), Blocks.STONE.defaultBlockState());
        var cow = helper.spawn(EntityType.COW, new BlockPos(9, 2, 9)); cow.setNoAi(true);
        var skeleton = helper.spawn(EntityType.SKELETON, new BlockPos(6, 2, 5)); skeleton.setNoAi(true);
        helper.assertTrue(helper.getLevel().getEntity(cow.getUUID()) == cow, "Diagonal cow was not registered in the loaded fixture");
        var taint = WorldEntities.SLIME.get().create(helper.getLevel());
        taint.setPos(p.getX()+1, p.getY(), p.getZ()+1); helper.getLevel().addFreshEntity(taint);
        var bottle = AlchemyLiquids.BOTTLE_PROJECTILE.get().create(helper.getLevel());
        bottle.setPos(p.getX()+.5,p.getY(),p.getZ()+.5); helper.getLevel().addFreshEntity(bottle);
        helper.getLevel().random.setSeed(0); bottle.getRandom().setSeed(0); bottle.splash();
        helper.assertTrue(bottle.isRemoved(), "Thrown bottle survived its impact");
        helper.assertTrue(cow.hasEffect(WorldEntities.TAINT), "Original cube splash missed a living diagonal target");
        helper.assertValueEqual(cow.getEffect(WorldEntities.TAINT).getDuration(), 100, "Splash used a different effect duration");
        helper.assertTrue(!skeleton.hasEffect(WorldEntities.TAINT), "Bottled taint infected an undead target");
        helper.assertTrue(!taint.hasEffect(WorldEntities.TAINT), "Bottled taint infected a tainted creature");
        boolean goo = false;
        for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) goo |= helper.getLevel().getBlockState(p.offset(x,0,z)).is(WorldContent.block("flux_goo"));
        helper.assertTrue(goo, "Impact did not seed any flux goo on solid ground");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void wardSuppressesWarpChecksAndBathSaltsDiscoverySurvivesSavedCounter(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper);
        var state = ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        state.warp(6,5,3);
        player.addEffect(new MobEffectInstance(AlchemyLiquids.WARP_WARD, 100));
        helper.assertTrue(!AlchemyLiquidEvents.checkWarp(player, 0), "Ward allowed a warp check");
        helper.assertValueEqual(state.temporaryWarp(), 3, "Ward did not suppress temporary-warp decay");
        helper.assertValueEqual(state.warpCounter(), 14, "Ward spent event activity");
        player.removeEffect(AlchemyLiquids.WARP_WARD);
        helper.assertTrue(AlchemyLiquidEvents.checkWarp(player, 0), "Eligible warp event was rejected");
        helper.assertTrue(state.knows("!BATHSALTS"), "Actual warp above ten did not reveal bath salts");
        helper.assertValueEqual(state.temporaryWarp(), 2, "Warp check did not decay temporary warp");
        var restored = ResearchCodec.read(ResearchCodec.write(state));
        helper.assertValueEqual(restored.warpCounter(), state.warpCounter(), "Reload reset the reduced event counter");
        helper.assertTrue(restored.knows("!BATHSALTS"), "Reload lost bath salts discovery");
        helper.succeed();
    }
    private static void basin(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL)
            helper.getLevel().setBlockAndUpdate(pos.relative(direction), Blocks.STONE.defaultBlockState());
    }
    private static final class WashingPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        WashingPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "soap-test"));
            BlockPos p = helper.absolutePos(new BlockPos(2, 2, 2)); setPos(p.getX()+.5, p.getY(), p.getZ()+.5);
        }
        // FakePlayer deliberately disables tick; expose the actual LivingEntity use pipeline.
        void useTick() { updateUsingItem(getUseItem()); }
    }
    private AlchemyLiquidGameTests() {}
}
