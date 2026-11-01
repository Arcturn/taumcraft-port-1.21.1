package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.research.LegacyRequirement;

/** Cross-module checks using the loaded item registry and recipe manager. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AlchemyIntegrationGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void newMaterialsSatisfyOriginalBookRequirements(GameTestHelper helper) {
        var rare=stack("rareearth");
        var nugget=LegacyRequirement.parse("thaumcraft:nugget;1;10");
        helper.assertTrue(nugget.matches().test(rare),"Rare Earths must satisfy original nugget metadata 10");
        helper.assertFalse(nugget.matches().test(new ItemStack(Items.QUARTZ)),"Quartz must not replace Rare Earths");
        var pick=stack("thaumium_pick");
        var requirement=LegacyRequirement.parse("thaumcraft:thaumium_pick");
        helper.assertTrue(requirement.matches().test(pick),"The crafted thaumium pickaxe must fulfill original research");
        helper.assertValueEqual(requirement.craftKey(),LegacyRequirement.craftKey(pick),"Craft events and research use the same item key");
        String[] metals={"iron","gold","copper","tin","silver","lead","cinnabar","quartz"};
        for(int i=0;i<metals.length;i++) helper.assertTrue(LegacyRequirement.parse("thaumcraft:cluster;1;"+i)
                .matches().test(stack("cluster_"+metals[i])),"Original cluster metadata "+i+" must resolve to its real item");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=40)
    public static void alchemicalBucketsAndMineralsKeepOriginalExplicitAspects(GameTestHelper helper) {
        var bucket=RecipeAspects.get(new ItemStack(Items.BUCKET),helper.getLevel());
        for(String name:new String[]{"bucket_death","bucket_purifying"}) {
            var expected=new java.util.LinkedHashMap<>(bucket);
            var additions=name.equals("bucket_death")?Map.of(Aspect.DEATH,15,Aspect.ENTROPY,15)
                    :Map.of(Aspect.MIND,15,Aspect.ORDER,15);
            additions.forEach((aspect,amount)->expected.merge(aspect,amount,Integer::sum));
            helper.assertTrue(RecipeAspects.get(stack(name),helper.getLevel()).equals(expected),
                    "ConfigAspects copies an empty bucket, not water or the crucible catalyst: "+name);
        }
        helper.assertTrue(RecipeAspects.get(stack("rareearth"),helper.getLevel()).equals(
                Map.of(Aspect.EARTH,5,Aspect.ORDER,5,Aspect.METAL,5)),"Original Rare Earths aspect definition");
        helper.assertValueEqual(RecipeAspects.get(stack("cluster_lead"),helper.getLevel()).get(Aspect.ORDER),15,
                "Lead cluster combines both original Ordo additions");
        helper.assertTrue(RecipeAspects.get(stack("candle_white"),helper.getLevel()).getOrDefault(Aspect.LIGHT,0)>=5,
                "Tallow candles retain their five-Lux complex object bonus");
        helper.succeed();
    }

    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id)));}
    private AlchemyIntegrationGameTests(){}
}
