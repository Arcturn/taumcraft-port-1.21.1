package thaumcraft.port.gametest;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.auromancy.effect.FocusEffectContent;
import thaumcraft.port.auromancy.effect.SapBlock;
import thaumcraft.port.boss.BossContent;
import thaumcraft.port.mob.TaintedMob;
import thaumcraft.port.research.ScanDiscoveries;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class BossIntegrationGameTests {
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void bossScansPreserveOriginalClassDiscoveriesAndAspects(GameTestHelper h) {
        verify(h, BossContent.ELDRITCH_WARDEN.get(), Map.of(Aspect.ELDRITCH,40,Aspect.DEATH,40,Aspect.UNDEAD,40), Set.of("!ORBOSS","!ORMOB"));
        verify(h, BossContent.ELDRITCH_GOLEM.get(), Map.of(Aspect.ELDRITCH,40,Aspect.ENERGY,40,Aspect.MECHANISM,40), Set.of("!ORBOSS","!ORMOB"));
        verify(h, BossContent.CULTIST_LEADER.get(), Map.of(Aspect.ELDRITCH,40,Aspect.AVERSION,40,Aspect.MAN,40), Set.of("!ORBOSS"));
        verify(h, BossContent.GIANT_TAINTACLE.get(), Map.of(Aspect.ELDRITCH,40,Aspect.BEAST,40,Aspect.FLUX,40), Set.of("!Taintacle","!ORMOB"));
        verify(h, BossContent.CULTIST_PORTAL_GREATER.get(), Map.of(), Set.of());
        h.assertTrue(TaintedMob.isTaintedType(BossContent.GIANT_TAINTACLE.get()), "Giant taintacle must share the taint faction");
        h.assertTrue(BossContent.ELDRITCH_GOLEM.get().is(SapBlock.ELDRITCH), "Construct must ignore eldritch sap");
        h.assertTrue(BossContent.ELDRITCH_WARDEN.get().is(SapBlock.ELDRITCH), "Warden must ignore eldritch sap");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void allFiveBossSpawnEggsResolveTheirEncounters(GameTestHelper h) {
        Map<SpawnEggItem,EntityType<?>> eggs=Map.of(
                BossContent.ELDRITCH_GOLEM_EGG.get(),BossContent.ELDRITCH_GOLEM.get(),
                BossContent.ELDRITCH_WARDEN_EGG.get(),BossContent.ELDRITCH_WARDEN.get(),
                BossContent.CULTIST_LEADER_EGG.get(),BossContent.CULTIST_LEADER.get(),
                BossContent.CULTIST_PORTAL_GREATER_EGG.get(),BossContent.CULTIST_PORTAL_GREATER.get(),
                BossContent.GIANT_TAINTACLE_EGG.get(),BossContent.GIANT_TAINTACLE.get());
        eggs.forEach((egg,type)->h.assertTrue(egg.getType(egg.getDefaultInstance())==type,"Spawn egg resolves the wrong encounter: "+type));
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void cultistEquipmentCopiesIronAspectsBeforeOriginalBonuses(GameTestHelper h) {
        var ironChest=RecipeAspects.get(new ItemStack(Items.IRON_CHESTPLATE),h.getLevel());
        var praetor=RecipeAspects.get(stack("crimson_praetor_chest"),h.getLevel());
        h.assertValueEqual(praetor.get(Aspect.ELDRITCH),ironChest.getOrDefault(Aspect.ELDRITCH,0)+10,"Praetor eldritch essence");
        h.assertValueEqual(praetor.get(Aspect.METAL),ironChest.get(Aspect.METAL),"Praetor iron essence");
        var blade=RecipeAspects.get(stack("crimson_blade"),h.getLevel());
        var ironSword=RecipeAspects.get(new ItemStack(Items.IRON_SWORD),h.getLevel());
        h.assertValueEqual(blade.get(Aspect.ELDRITCH),ironSword.getOrDefault(Aspect.ELDRITCH,0)+10,"Crimson blade eldritch essence");
        h.assertValueEqual(blade.get(Aspect.DEATH),ironSword.getOrDefault(Aspect.DEATH,0)+10,"Crimson blade death essence");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void wardenSapDisappearsOnItsScheduledTick(GameTestHelper h) {
        var pos=new BlockPos(2,2,2);
        h.setBlock(pos,FocusEffectContent.SAP.get());
        h.getLevel().scheduleTick(h.absolutePos(pos),FocusEffectContent.SAP.get(),3);
        h.runAfterDelay(5,()->{
            h.assertFalse(h.getBlockState(pos).is(FocusEffectContent.SAP.get()),"Scheduled warden sap remained after expiry");
            h.succeed();
        });
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void fluxTaintHealsConvertedChampionsAndDoesNotDamageUndead(GameTestHelper h) {
        var cow=EntityType.COW.create(h.getLevel());
        h.assertTrue(thaumcraft.port.champion.ChampionSystem.makeTainted(cow),"Passive creature did not become tainted");
        cow.setHealth(cow.getMaxHealth()-4);
        float before=cow.getHealth();
        thaumcraft.port.world.WorldEntities.TAINT.get().applyEffectTick(cow,0);
        h.assertValueEqual(cow.getHealth(),before+1,"Tainted champion should heal from flux taint");
        var zombie=EntityType.ZOMBIE.create(h.getLevel());
        before=zombie.getHealth();
        thaumcraft.port.world.WorldEntities.TAINT.get().applyEffectTick(zombie,0);
        h.assertValueEqual(zombie.getHealth(),before,"Undead should not take flux-taint potion damage");
        cow.discard();zombie.discard();h.succeed();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)));
    }
    private static void verify(GameTestHelper h,EntityType<?> type,Map<Aspect,Integer> aspects,Set<String> discoveries) {
        var entity=type.create(h.getLevel());
        h.assertTrue(AspectTags.forEntity(entity).equals(aspects),"Incorrect boss aspects for "+type);
        h.assertTrue(ScanDiscoveries.entityFlags(entity).equals(discoveries),"Incorrect boss discoveries for "+type+": "+ScanDiscoveries.entityFlags(entity));
        entity.discard();
    }
    private BossIntegrationGameTests() {}
}
