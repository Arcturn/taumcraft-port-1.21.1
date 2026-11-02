package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.warp.WarpEffects;
import thaumcraft.port.warp.ZombieBrainItem;
import thaumcraft.port.world.WorldContent;
import thaumcraft.port.world.WorldEntities;

/** BETA26 status effects exercised through real entities, consumables, effect persistence and events. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WarpEffectGameTests {
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void milkProtectionIsPerInstancePersistsAndStillAllowsTotems(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        for(var effect:List.of(WarpEffects.THAUMARHIA,WarpEffects.UNNATURAL_HUNGER,WarpEffects.SUN_SCORNED,
                WarpEffects.DEATH_GAZE,WorldEntities.VIS_EXHAUST,WorldEntities.INFECTIOUS)) {
            var original=WarpEffects.uncurable(new MobEffectInstance(effect,1000,1,true,true));
            var loaded=MobEffectInstance.load((CompoundTag)original.save());
            h.assertTrue(loaded!=null&&!loaded.getCures().contains(EffectCures.MILK),"Milk immunity lost through save/load: "+effect.getId());
            h.assertTrue(loaded.getCures().contains(EffectCures.PROTECTED_BY_TOTEM),"Totem must retain vanilla 1.12 cure behavior");
            player.addEffect(loaded);
        }
        player.addEffect(new MobEffectInstance(WarpEffects.BLURRED_VISION,1000));
        player.addEffect(new MobEffectInstance(MobEffects.POISON,1000));
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(h.getLevel(),player);
        h.assertFalse(player.hasEffect(WarpEffects.BLURRED_VISION)||player.hasEffect(MobEffects.POISON),"Milk did not cure ordinary effects");
        h.assertValueEqual(player.getActiveEffects().size(),6,"Milk removed a protected warp condition");
        player.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        h.assertTrue(player.getActiveEffects().isEmpty(),"Totem failed to clear protected warp conditions");
        player.addEffect(new MobEffectInstance(WorldEntities.VIS_EXHAUST,1000));
        player.addEffect(new MobEffectInstance(WorldEntities.INFECTIOUS,1000));
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(h.getLevel(),player);
        h.assertTrue(player.getActiveEffects().isEmpty(),"Warp immunity leaked into ordinary exposure or infection");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void hungerAddsSourceExhaustionAndMealsOnlyTreatTheSpecificCondition(GameTestHelper h) {
        var player=GameTestPlayers.create(h);player.getFoodData().setExhaustion(0);
        player.addEffect(WarpEffects.uncurable(new MobEffectInstance(WarpEffects.UNNATURAL_HUNGER,5000,2,true,true)));
        for(int i=0;i<10;i++)WarpEffects.UNNATURAL_HUNGER.get().applyEffectTick(player,2);
        h.assertTrue(Math.abs(player.getFoodData().getExhaustionLevel()-.75f)<.00001,"Unnatural hunger exhaustion is not .025*(amplifier+1) per tick");
        consume(player,new ItemStack(Items.APPLE));
        var hunger=player.getEffect(WarpEffects.UNNATURAL_HUNGER);
        h.assertTrue(hunger!=null&&hunger.getAmplifier()==2&&hunger.getDuration()==5000,"Ordinary food treated unnatural hunger");
        consume(player,new ItemStack(Items.ROTTEN_FLESH));
        hunger=player.getEffect(WarpEffects.UNNATURAL_HUNGER);
        h.assertTrue(hunger!=null&&hunger.getAmplifier()==1&&hunger.getDuration()==4400,"Rotten flesh did not reduce one level and 600 ticks");
        h.assertTrue(hunger.isAmbient()&&!hunger.isVisible()&&!hunger.getCures().contains(EffectCures.MILK),"Treatment changed original hidden residual effect flags or milk immunity");
        consume(player,brain());
        hunger=player.getEffect(WarpEffects.UNNATURAL_HUNGER);
        h.assertTrue(hunger!=null&&hunger.getAmplifier()==0&&hunger.getDuration()==3800,"Brain did not use the same treatment after rotten flesh");
        consume(player,new ItemStack(Items.ROTTEN_FLESH));
        h.assertFalse(player.hasEffect(WarpEffects.UNNATURAL_HUNGER),"Final hunger level was not cured");
        player.addEffect(WarpEffects.uncurable(new MobEffectInstance(WarpEffects.UNNATURAL_HUNGER,600,3)));
        consume(player,brain());
        h.assertFalse(player.hasEffect(WarpEffects.UNNATURAL_HUNGER),"Expired residual hunger was reapplied");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void zombieBrainRestoresFoodConsumesOneAndAddsOnlyNormalOrTemporaryWarp(GameTestHelper h) {
        var player=GameTestPlayers.create(h);var food=brain();food.setCount(2);
        h.assertTrue(food.getItem() instanceof ZombieBrainItem,"Brain is still an inert ingredient");
        h.assertTrue(food.is(ItemTags.WOLF_FOOD),"Original wolf-food behavior was omitted");
        player.getFoodData().setFoodLevel(10);player.getFoodData().setSaturation(0);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(7,0,0);
        consume(player,food);
        h.assertValueEqual(food.getCount(),1,"Brain consumption must spend one item");
        h.assertValueEqual(player.getFoodData().getFoodLevel(),14,"Brain must restore four food points");
        h.assertTrue(Math.abs(player.getFoodData().getSaturationLevel()-1.6f)<.0001,"Brain saturation modifier is not .2");
        h.assertValueEqual(state.permanentWarp(),7,"Brain added permanent warp");
        h.assertTrue(state.normalWarp()==1&&state.temporaryWarp()==0||state.normalWarp()==0&&state.temporaryWarp()>=1&&state.temporaryWarp()<=3,"Brain warp differs from the source's alternative branches");
        h.assertFalse(player.hasEffect(MobEffects.HUNGER),"Brain activated BETA26's unreachable base ItemFood potion setting");
        player.setGameMode(GameType.CREATIVE);var creative=brain();creative.setCount(2);consume(player,creative);
        h.assertValueEqual(creative.getCount(),1,"Original brain consumes one even in creative");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void thaumarrhiaPlacesFullGooOnlyIntoAirAtThePlayersFeet(GameTestHelper h) {
        var level=h.getLevel();var player=GameTestPlayers.create(h);var pos=h.absolutePos(new BlockPos(3,3,3));player.setPos(Vec3.atBottomCenterOf(pos));
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());long seed=seedWithInt(15,0);
        level.random.setSeed(seed);WarpEffects.THAUMARHIA.get().applyEffectTick(player,0);
        h.assertTrue(level.getBlockState(pos).is(WorldContent.block("flux_goo")),"Thaumarhia failed its source 1/15 goo placement roll");
        h.assertTrue(level.getBlockState(pos).equals(WorldContent.block("flux_goo").defaultBlockState()),"Thaumarhia did not place the original full source block");
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.random.setSeed(seed);
        WarpEffects.THAUMARHIA.get().applyEffectTick(player,0);
        h.assertTrue(level.getBlockState(pos).is(Blocks.STONE),"Thaumarhia replaced a non-air block");
        h.assertTrue(WarpEffects.THAUMARHIA.get().shouldApplyEffectTickThisTick(120,0)&&!WarpEffects.THAUMARHIA.get().shouldApplyEffectTickThisTick(119,0),"Thaumarhia source twenty-tick cadence changed");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void sunScornedBurnsUnderOpenBrightSkyAndHealsInDarkness(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,4,4));var target=new BrightnessPig(level);target.setPos(Vec3.atBottomCenterOf(pos));
        h.assertTrue(level.canSeeSky(pos),"Sun fixture is not exposed to sky");
        target.brightness=1;level.random.setSeed(seedWithFloatBelow(.04f));WarpEffects.SUN_SCORNED.get().applyEffectTick(target,3);
        h.assertValueEqual(target.getRemainingFireTicks(),80,"Sun-scorned ignition is not four seconds");
        target.clearFire();var roof=pos.above(2);var previous=level.getBlockState(roof);level.setBlockAndUpdate(roof,Blocks.STONE.defaultBlockState());
        // Skylight propagation is asynchronous in 1.21; wait for the placed roof.
        h.runAfterDelay(5, () -> {
        try {
            h.assertFalse(level.canSeeSky(pos),"Roof did not block exposure");
            level.random.setSeed(seedWithFloatBelow(.04f));WarpEffects.SUN_SCORNED.get().applyEffectTick(target,3);
            h.assertTrue(target.getRemainingFireTicks()<=0,"Sun-scorned effect ignored the roof");
        } finally {level.setBlockAndUpdate(roof,previous);}
        target.brightness=0;target.setHealth(5);level.random.setSeed(0);WarpEffects.SUN_SCORNED.get().applyEffectTick(target,0);
        h.assertValueEqual(target.getHealth(),6f,"Darkness did not heal exactly one point");
        target.brightness=.3f;WarpEffects.SUN_SCORNED.get().applyEffectTick(target,0);
        h.assertValueEqual(target.getHealth(),6f,"Mid-light incorrectly healed the player");
        h.assertTrue(WarpEffects.SUN_SCORNED.get().shouldApplyEffectTickThisTick(80,3)&&!WarpEffects.SUN_SCORNED.get().shouldApplyEffectTickThisTick(79,3),"Sun-scorned forty-tick cadence changed");
        h.succeed();
        });
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void deathGazeHonoursViewOcclusionRangeAndExistingWither(GameTestHelper h) {
        var level=h.getLevel();var player=GameTestPlayers.create(h);var base=h.absolutePos(new BlockPos(8,2,6));player.setPos(Vec3.atBottomCenterOf(base));player.setYRot(0);player.setXRot(0);
        player.addEffect(WarpEffects.uncurable(new MobEffectInstance(WarpEffects.DEATH_GAZE,6000,1)));
        var ahead=h.spawn(EntityType.ZOMBIE,new BlockPos(8,2,11));ahead.setNoAi(true);
        var side=h.spawn(EntityType.ZOMBIE,new BlockPos(13,2,9));side.setNoAi(true);
        var behind=h.spawn(EntityType.ZOMBIE,new BlockPos(8,2,2));behind.setNoAi(true);
        var distant=h.spawn(EntityType.ZOMBIE,new BlockPos(8,2,19));distant.setNoAi(true);
        try {
            WarpEffects.checkDeathGaze(player);
            h.assertTrue(ahead.hasEffect(MobEffects.WITHER),"Death gaze missed a visible target in its cone");
            h.assertValueEqual(ahead.getEffect(MobEffects.WITHER).getDuration(),80,"Death gaze wither duration differs from BETA26");
            h.assertTrue(ahead.getTarget()==player&&ahead.getLastHurtByMob()==player&&ahead.getLastHurtMob()==player,"Death gaze did not set retaliation and combat references");
            h.assertFalse(side.hasEffect(MobEffects.WITHER)||behind.hasEffect(MobEffects.WITHER)||distant.hasEffect(MobEffects.WITHER),"Gaze affected a target outside cone or range");
            ahead.removeEffect(MobEffects.WITHER);ahead.addEffect(new MobEffectInstance(MobEffects.WITHER,10));WarpEffects.checkDeathGaze(player);
            h.assertValueEqual(ahead.getEffect(MobEffects.WITHER).getDuration(),10,"Existing wither was refreshed by gaze");
            ahead.removeEffect(MobEffects.WITHER);var wall=base.offset(0,1,2);level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            WarpEffects.checkDeathGaze(player);h.assertFalse(ahead.hasEffect(MobEffects.WITHER),"Death gaze penetrated an opaque wall");
            level.removeBlock(wall,false);
        } finally {ahead.discard();side.discard();behind.discard();distant.discard();}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void deathGazeRespectsDisabledServerPvp(GameTestHelper h) {
        var level=h.getLevel();var player=GameTestPlayers.create(h);var victim=GameTestPlayers.create(h);var base=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8,2,6)));
        player.setPos(base);player.setYRot(0);player.setXRot(0);victim.setPos(base.add(0,0,5));level.addNewPlayer(victim);
        player.addEffect(WarpEffects.uncurable(new MobEffectInstance(WarpEffects.DEATH_GAZE,6000,0)));
        var server=level.getServer();boolean original=server.isPvpAllowed();
        try {
            server.setPvpAllowed(false);WarpEffects.checkDeathGaze(player);h.assertFalse(victim.hasEffect(MobEffects.WITHER),"Gaze ignored server PvP protection");
            server.setPvpAllowed(true);WarpEffects.checkDeathGaze(player);h.assertTrue(victim.hasEffect(MobEffects.WITHER),"Enabled PvP unexpectedly blocked original gaze behavior");
        } finally {server.setPvpAllowed(original);victim.discard();}
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void infectiousExhaustionLowersEachGenerationAndKeepsSecondaryCasesCurable(GameTestHelper h) {
        var level=h.getLevel();var source=h.spawn(EntityType.COW,new BlockPos(3,2,3));var next=h.spawn(EntityType.COW,new BlockPos(5,2,3));var finalCase=h.spawn(EntityType.COW,new BlockPos(14,2,3));
        source.setNoAi(true);next.setNoAi(true);finalCase.setNoAi(true);
        try {
            source.addEffect(WarpEffects.uncurable(new MobEffectInstance(WorldEntities.INFECTIOUS,1000,1)));
            WorldEntities.INFECTIOUS.get().applyEffectTick(source,1);
            var infection=next.getEffect(WorldEntities.INFECTIOUS);
            h.assertTrue(infection!=null&&infection.getAmplifier()==0&&infection.getDuration()==6000,"Source infection generation/duration changed");
            h.assertTrue(infection.getCures().contains(EffectCures.MILK),"Secondary infection inherited warp-only immunity");
            next.setPos(finalCase.position().add(-2,0,0));WorldEntities.INFECTIOUS.get().applyEffectTick(next,0);
            h.assertTrue(finalCase.hasEffect(WorldEntities.VIS_EXHAUST)&&!finalCase.hasEffect(WorldEntities.INFECTIOUS),"Last infection generation did not become ordinary vis exhaustion");
            finalCase.removeEffectsCuredBy(EffectCures.MILK);h.assertFalse(finalCase.hasEffect(WorldEntities.VIS_EXHAUST),"Final exposure cannot be cured normally");
            h.assertTrue(WorldEntities.INFECTIOUS.get().shouldApplyEffectTickThisTick(80,3)&&!WorldEntities.INFECTIOUS.get().shouldApplyEffectTickThisTick(79,3),"Contagion cadence must stay forty ticks independent of amplifier");
        } finally {source.discard();next.discard();finalCase.discard();}
        h.succeed();
    }

    private static ItemStack brain(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:brain")));}
    private static void consume(ServerPlayer player,ItemStack food){
        player.getFoodData().setFoodLevel(Math.min(10,player.getFoodData().getFoodLevel()));
        var original=food.copy();EventHooks.onItemUseFinish(player,original,0,food.finishUsingItem(player.level(),player));
    }
    private static long seedWithInt(int bound,int value){for(long seed=0;seed<10000;seed++)if(RandomSource.create(seed).nextInt(bound)==value)return seed;throw new AssertionError("No deterministic seed");}
    private static long seedWithFloatBelow(float bound){for(long seed=0;seed<10000;seed++)if(RandomSource.create(seed).nextFloat()<bound)return seed;throw new AssertionError("No deterministic seed");}
    private static final class BrightnessPig extends Pig {
        float brightness;
        BrightnessPig(Level level){super(EntityType.PIG,level);}
        @Override public float getLightLevelDependentMagicValue(){return brightness;}
    }
    private WarpEffectGameTests() { }
}
