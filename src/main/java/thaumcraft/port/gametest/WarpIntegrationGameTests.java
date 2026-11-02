package thaumcraft.port.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;
import thaumcraft.port.research.ResearchCodec;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.warp.WarpConfig;
import thaumcraft.port.warp.WarpEvents;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WarpIntegrationGameTests {
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void protectiveWardPreservesBothTemporaryWarpAndEventActivity(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());
        state.warp(30,20,10);state.warpCounter(70);
        player.addEffect(new MobEffectInstance(AlchemyLiquids.WARP_WARD,1000));
        h.assertFalse(WarpEvents.check(player,0),"Ward allowed a periodic event");
        h.assertValueEqual(state.temporaryWarp(),10,"Ward allowed temporary decay");
        h.assertValueEqual(state.warpCounter(),70,"Ward spent event activity");
        h.assertFalse(state.knows("ELDRITCHMINOR"),"Protected check granted an event discovery");
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void disabledEventsPreserveSavedWarpWithoutDestroyingActiveEffects(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(10,20,30);
        player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,100));
        boolean previous=WarpConfig.WUSS_MODE.get();
        try {
            WarpConfig.WUSS_MODE.set(true);
            h.assertFalse(WarpEvents.check(player,0),"Disabled events still triggered");
            h.assertValueEqual(state.temporaryWarp(),30,"Disabled events consumed temporary warp");
            h.assertValueEqual(state.warpCounter(),60,"Disabled events consumed activity");
            h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),"Option erased an active effect");
        } finally {WarpConfig.WUSS_MODE.set(previous);}
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void failedRollDecaysOnlyTemporaryWarpAndDoesNotGrantDiscoveries(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(51,3,4);state.warpCounter(16);
        h.assertFalse(WarpEvents.check(player,99),"Failed probability roll fired an event");
        h.assertValueEqual(state.permanentWarp(),51,"A failed roll changed permanent warp");
        h.assertValueEqual(state.normalWarp(),3,"A failed roll changed normal warp");
        h.assertValueEqual(state.temporaryWarp(),3,"Failed roll must still decay temporary warp once");
        h.assertValueEqual(state.warpCounter(),16,"Failed roll spent activity");
        h.assertFalse(state.knows("ELDRITCHMINOR"),"A failed roll unlocked eldritch research");
        var restored=ResearchCodec.read(ResearchCodec.write(state));
        h.assertValueEqual(restored.warpCounter(),16,"Reload restarted activity");
        h.assertValueEqual(restored.temporaryWarp(),3,"Reload lost temporary decay");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void depletedActivityStaysDormantUntilNewWarpIsGained(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(5,0,0);state.warpCounter(1);
        h.assertTrue(WarpEvents.check(player,0),"First eligible event failed");
        h.assertValueEqual(state.warpCounter(),0,"Minimum decay did not exhaust activity");
        h.assertFalse(WarpEvents.check(player,0),"Zero activity kept causing events");
        state.warp(5,0,1);
        h.assertValueEqual(state.warpCounter(),6,"A new warp gain did not reactivate events");
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void permanentAndNormalWarpUseStrictDiscoveryThresholds(GameTestHelper h) {
        for(int amount:new int[]{10,11,25,26,50,51}) {
            var player=GameTestPlayers.create(h);player.getRandom().setSeed(0);
            var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(amount/2,amount-amount/2,0);state.warpCounter(1);
            h.assertTrue(WarpEvents.check(player,0),"Eligible discovery event rejected");
            h.assertTrue(state.knows("!BATHSALTS")== (amount>10),"Bath salts threshold differs");
            h.assertTrue(state.knows("ELDRITCHMINOR")== (amount>25),"Minor eldritch threshold differs");
            h.assertTrue(state.knows("ELDRITCHMAJOR")== (amount>50),"Major eldritch threshold differs");
        }
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void temporaryWarpCannotUnlockEldritchDiscoveries(GameTestHelper h) {
        var player=GameTestPlayers.create(h);player.getRandom().setSeed(0);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(0,0,60);state.warpCounter(1);
        h.assertTrue(WarpEvents.check(player,0),"Temporary warp should cause events");
        h.assertFalse(state.knows("!BATHSALTS")||state.knows("ELDRITCHMINOR")||state.knows("ELDRITCHMAJOR"),"Temporary warp granted permanent/normal discoveries");
        h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void clarityEventRemovesOnlyNormalWarpAndPersistsIt(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(8,3,4);state.warpCounter(7);
        WarpEvents.dispatch(player,76,100);
        h.assertValueEqual(state.permanentWarp(),8,"Clarity reduced permanent warp");
        h.assertValueEqual(state.normalWarp(),2,"Clarity did not remove exactly one normal warp");
        h.assertValueEqual(state.temporaryWarp(),4,"Clarity altered temporary warp");
        h.assertValueEqual(state.warpCounter(),7,"Clarity reactivated event activity");
        var restored=ResearchCodec.read(ResearchCodec.write(state));
        h.assertValueEqual(restored.normalWarp(),2,"Clarity was lost on reload");h.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void operatorWarpCommandsValidatePermissionsAndPreserveOtherTypes(GameTestHelper h) throws Exception {
        var player=GameTestPlayers.create(h);var source=player.createCommandSourceStack();
        var dispatcher=player.getServer().getCommands().getDispatcher();
        boolean rejected=false;
        try {dispatcher.execute("thaumcraft warp set @s permanent 40",source.withPermission(0));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
        h.assertTrue(rejected,"Non-operator changed warp");
        h.assertValueEqual(dispatcher.execute("thaumcraft warp set @s permanent 40",source.withPermission(2)),1,"Operator could not set warp");
        dispatcher.execute("thaumcraft warp add @s temporary 7",source.withPermission(2));
        dispatcher.execute("thaumcraft warp add @s temporary -20",source.withPermission(2));
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());
        h.assertValueEqual(state.permanentWarp(),40,"Temporary command changed permanent warp");
        h.assertValueEqual(state.temporaryWarp(),0,"Command underflowed temporary warp");
        h.succeed();
    }
    private WarpIntegrationGameTests() {}
}
