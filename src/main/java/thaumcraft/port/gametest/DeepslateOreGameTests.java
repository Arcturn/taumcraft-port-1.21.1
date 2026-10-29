package thaumcraft.port.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.research.ScanDiscoveries;

/** Registered deep ore blocks must participate in the same gameplay as their stone variants. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class DeepslateOreGameTests {
    private static final List<String> MATERIALS=List.of("amber","cinnabar","quartz");

    @GameTest(template="empty",timeoutTicks=100)
    public static void deepslateLootPreservesOrdinaryDropsFortuneAndSilkTouch(GameTestHelper helper){
        var level=helper.getLevel();
        var plain=new ItemStack(Items.DIAMOND_PICKAXE);
        var fortune=plain.copy();
        fortune.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
        var silk=plain.copy();
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        int pairedChecks=0,curios=0;
        for(String material:MATERIALS){
            var ordinary=block("ore_"+material);
            var deep=block("deepslate_ore_"+material);
            for(var tool:List.of(plain,fortune,silk))for(long index=1;index<=512;index++){
                long seed=index*0x9e3779b97f4a7c15L;
                var originalDrops=drops(level,ordinary,tool,seed);
                var deepDrops=drops(level,deep,tool,seed);
                helper.assertValueEqual(normalized(deepDrops,deep),normalized(originalDrops,ordinary),
                        "Deep ore loot differs from stone variant for "+material+" at seed "+seed);
                helper.assertValueEqual(deepDrops.size(),1,"Ore must yield exactly one kind of drop");
                var drop=deepDrops.getFirst();
                if(tool==silk || material.equals("cinnabar")){
                    helper.assertTrue(drop.is(deep.asItem())&&drop.getCount()==1,"Silk Touch/self drop lost the deepslate variant");
                }else if(material.equals("quartz")){
                    helper.assertTrue(drop.is(Items.QUARTZ),"Quartz ore dropped the wrong item");
                    helper.assertTrue(drop.getCount()>=1&&drop.getCount()<=(tool==fortune?4:1),"Quartz Fortune count is invalid");
                }else if(drop.is(BuiltInRegistries.ITEM.get(id("curio_preserved")))){
                    helper.assertValueEqual(drop.getCount(),1,"Amber ore duplicated a preserved curiosity");
                    if(tool==plain)curios++;
                }else{
                    helper.assertTrue(drop.is(BuiltInRegistries.ITEM.get(id("amber"))),"Amber ore dropped the wrong item");
                    helper.assertTrue(drop.getCount()>=1&&drop.getCount()<=(tool==fortune?8:2),"Amber Fortune count is invalid");
                }
                pairedChecks++;
            }
        }
        // Both the ordinary and deep variants retain the rare curiosity alternative.
        helper.assertTrue(curios>10&&curios<60,"Amber curiosity alternative disappeared or became common: "+curios);
        System.out.println("DEEPSLATE_ORE_LOOT paired_seeded_checks="+pairedChecks+" amber_curios_in_512="+curios);
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void deepslateOresKeepMiningAspectsAndResearchIntegration(GameTestHelper helper){
        var level=helper.getLevel();
        for(String material:MATERIALS){
            var ordinary=block("ore_"+material);var deep=block("deepslate_ore_"+material);
            var stoneState=ordinary.defaultBlockState();var deepState=deep.defaultBlockState();
            var stoneItem=new ItemStack(ordinary);var deepItem=new ItemStack(deep);
            helper.assertTrue(deepState.is(BlockTags.MINEABLE_WITH_PICKAXE),"Deep ore is absent from the pickaxe mining tag");
            helper.assertTrue(deepState.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:ores/"+material))),"Deep ore block material tag is missing");
            helper.assertTrue(deepItem.is(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ores/"+material))),"Deep ore item material tag is missing");
            float stoneHardness=switch(material){case "amber"->1.5f;case "cinnabar"->2f;default->3f;};
            helper.assertValueEqual(stoneState.getDestroySpeed(level,BlockPos.ZERO),stoneHardness,"Ordinary ore hardness differs from TC6");
            helper.assertValueEqual(deepState.getDestroySpeed(level,BlockPos.ZERO),stoneHardness*1.5f,"Deep ore hardness must scale the material's original hardness");
            helper.assertTrue(deepState.getSoundType(level,BlockPos.ZERO,null)==SoundType.DEEPSLATE,"Deep ore lacks deepslate mining sounds");
            for(var item:List.of(Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.DIAMOND_AXE)){
                var tool=new ItemStack(item);
                helper.assertValueEqual(tool.isCorrectToolForDrops(deepState),tool.isCorrectToolForDrops(stoneState),"Deep ore changed tool requirements");
                boolean expected=item==Items.IRON_PICKAXE||item==Items.DIAMOND_PICKAXE
                        ||item==Items.STONE_PICKAXE&&!material.equals("cinnabar")
                        ||item==Items.WOODEN_PICKAXE&&material.equals("quartz");
                helper.assertValueEqual(tool.isCorrectToolForDrops(deepState),expected,"Ore harvest tier differs from original "+material);
            }
            helper.assertFalse(AspectTags.forItem(stoneItem).isEmpty(),"Ordinary ore has no reference aspects");
            helper.assertValueEqual(AspectTags.forItem(deepItem),AspectTags.forItem(stoneItem),"Deep ore lost its item aspects");
            helper.assertValueEqual(AspectTags.forBlock(deepState),AspectTags.forBlock(stoneState),"Deep ore lost its block aspects");
            helper.assertValueEqual(ScanDiscoveries.itemFlags(deepItem),ScanDiscoveries.itemFlags(stoneItem),"Deep ore does not unlock the ordinary research flags");
            helper.assertValueEqual(ScanDiscoveries.blockFlags(deepState,deepItem),ScanDiscoveries.blockFlags(stoneState,stoneItem),"Deep ore block scan differs from ordinary ore");
            for(int i=0;i<32;i++){
                int experience=deep.getExpDrop(deepState,level,BlockPos.ZERO,null,null,new ItemStack(Items.DIAMOND_PICKAXE));
                helper.assertTrue(material.equals("cinnabar")?experience==0:experience>=1&&experience<=4,"Deep ore XP differs from ordinary ore");
            }
        }
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void deepslateCinnabarWorksInFurnacesAndMetalPurification(GameTestHelper helper){
        var level=helper.getLevel();
        var recipes=level.getRecipeManager();
        var catalyst=recipes.byKey(id("crucible_metal_purification_cinnabar")).orElseThrow().value();
        helper.assertTrue(catalyst instanceof CrucibleRecipe,"Cinnabar purification recipe is not a crucible recipe");
        var crucible=(CrucibleRecipe)catalyst;
        for(String variant:List.of("ore_cinnabar","deepslate_ore_cinnabar")){
            var input=new SingleRecipeInput(new ItemStack(block(variant)));
            var smelting=recipes.getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value();
            var blasting=recipes.getRecipeFor(RecipeType.BLASTING,input,level).orElseThrow().value();
            var mercury=BuiltInRegistries.ITEM.get(id("quicksilver"));
            helper.assertTrue(smelting.assemble(input,level.registryAccess()).is(mercury),"Cinnabar smelting did not yield quicksilver");
            helper.assertTrue(blasting.assemble(input,level.registryAccess()).is(mercury),"Cinnabar blasting did not yield quicksilver");
            helper.assertValueEqual(smelting.getExperience(),1.0f,"Cinnabar smelting XP changed");
            helper.assertValueEqual(blasting.getExperience(),1.0f,"Cinnabar blasting XP changed");
            helper.assertTrue(crucible.matches(input,level),"Cinnabar variant is rejected by metal purification");
            helper.assertTrue(crucible.assemble(input,level.registryAccess()).is(BuiltInRegistries.ITEM.get(id("cluster_cinnabar"))),"Cinnabar purification yielded the wrong cluster");
        }
        helper.assertFalse(crucible.matches(new SingleRecipeInput(new ItemStack(block("deepslate_ore_quartz"))),level),"Cinnabar purification accepts unrelated deep ore");
        helper.succeed();
    }

    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static Block block(String path){
        var key=id(path);
        if(!BuiltInRegistries.BLOCK.containsKey(key))throw new AssertionError("Missing ore registration: "+key);
        return BuiltInRegistries.BLOCK.get(key);
    }
    private static List<ItemStack> drops(ServerLevel level,Block block,ItemStack tool,long seed){
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.ZERO)
                .withParameter(LootContextParams.BLOCK_STATE,block.defaultBlockState())
                .withParameter(LootContextParams.TOOL,tool).create(LootContextParamSets.BLOCK);
        return level.getServer().reloadableRegistries().getLootTable(block.getLootTable()).getRandomItems(params,seed);
    }
    private static Map<String,Integer> normalized(List<ItemStack> drops,Block source){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(var stack:drops){
            String key=stack.is(source.asItem())?"self":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            result.merge(key,stack.getCount(),Integer::sum);
        }
        return result;
    }
}
