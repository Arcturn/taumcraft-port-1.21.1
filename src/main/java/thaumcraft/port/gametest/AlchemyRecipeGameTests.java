package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.alchemy.material.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.essentia.EssentiaItem;
import thaumcraft.port.machine.*;
import thaumcraft.port.research.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AlchemyRecipeGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void allNewCrucibleRoutesConsumeTheirOriginalCostsAndHonorResearch(GameTestHelper h) {
        var pos=new BlockPos(1,3,1);
        h.setBlock(pos.below(),Blocks.MAGMA_BLOCK);h.setBlock(pos,MachineContent.CRUCIBLE.get());
        var be=(CrucibleEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var candidates=new ArrayList<String>(List.of("liquid_death","bottle_taint","bath_salts","sanity_soap","everfull_urn"));
        for(String suffix:List.of("gunpowder","slime","dye","glowstone","clay","string","web","lava"))candidates.add("crucible_hedge_"+suffix);
        for(Aspect aspect:Aspect.values())candidates.add("vis_crystal_"+aspect.id());
        for(String id:candidates) {
            var holder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:"+id)).orElseThrow();
            h.assertTrue(holder.value() instanceof CrucibleRecipe,"Missing registered crucible route "+id);
            var recipe=(CrucibleRecipe)holder.value();
            var player=GameTestPlayers.create(h);
            var state=ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID());
            var aspects=new EnumMap<Aspect,Integer>(Aspect.class);
            recipe.aspects().forEach((a,n)->aspects.put(Aspect.byId(a).orElseThrow(),n));
            var catalyst=recipe.catalyst().getItems()[0].copyWithCount(1);
            var area=new AABB(be.getBlockPos()).inflate(3);
            h.getLevel().getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard);
            be.state.restore(1000,200,-100,aspects);
            be.acceptItem(catalyst,player);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"Unresearched player crafted "+id);
            h.assertValueEqual(be.state.water(),1000,"Locked recipe consumed crafting water "+id);
            state.stage(recipe.research().split("@")[0],256,false);
            catalyst=recipe.catalyst().getItems()[0].copyWithCount(1);
            be.state.restore(1000,200,-100,aspects);
            h.assertTrue(be.acceptItem(catalyst,player),"Researched catalyst rejected "+id);
            var outputs=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
            h.assertValueEqual(outputs.size(),1,"Crucible output count "+id);
            h.assertTrue(ItemStack.isSameItemSameComponents(outputs.getFirst().getItem(),recipe.result()),"Crucible output/components differ "+id);
            h.assertValueEqual(outputs.getFirst().getItem().getCount(),recipe.result().getCount(),"Crucible output amount "+id);
            h.assertTrue(catalyst.isEmpty(),"Catalyst not consumed "+id);
            h.assertValueEqual(be.state.total(),0,"Aspect costs were not consumed exactly "+id);
            h.assertValueEqual(be.state.water(),950,"Original fifty mB crafting cost "+id);
            outputs.forEach(ItemEntity::discard);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void taintBottleRejectsEmptyAndWrongAspectPhials(GameTestHelper h) {
        var recipe=(CrucibleRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:bottle_taint")).orElseThrow().value();
        h.assertFalse(recipe.matches(new SingleRecipeInput(EssentiaItem.phial(null)),h.getLevel()),"Empty phial became a taint bottle");
        h.assertFalse(recipe.matches(new SingleRecipeInput(EssentiaItem.phial(Aspect.WATER)),h.getLevel()),"Water phial became a taint bottle");
        h.assertTrue(recipe.matches(new SingleRecipeInput(EssentiaItem.phial(Aspect.FLUX)),h.getLevel()),"Full Vitium phial rejected");
        for(String metal:List.of("tin","silver","lead")) {
            var recipeHolder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:cluster_"+metal+"_smelting")).orElseThrow();
            h.assertTrue(recipeHolder.value() instanceof net.minecraft.world.item.crafting.SmeltingRecipe,"Custom cluster smelting must satisfy the vanilla recipe type");
            var cooking=(ClusterCookingRecipe)recipeHolder.value();
            var blast=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:cluster_"+metal+"_blasting")).orElseThrow().value();
            h.assertTrue(blast instanceof net.minecraft.world.item.crafting.BlastingRecipe,"Custom cluster blasting must satisfy the vanilla recipe type");
            var input=new SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:cluster_"+metal))));
            var output=cooking.getResultItem(h.getLevel().registryAccess());
            h.assertValueEqual(cooking.matches(input,h.getLevel()),!output.isEmpty(),"Absent compat ingot fabricated a smelting result "+metal);
            if(!output.isEmpty())h.assertValueEqual(output.getCount(),2,"Purified cluster must smelt into two ingots");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void thaumiumEquipmentAndTallowCandlesHaveOriginalPhysicalProperties(GameTestHelper h) {
        var ingot=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ingot_thaumium")));
        for(String suffix:List.of("pick","sword","axe","shovel","hoe")) {
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_"+suffix));
            var stack=new ItemStack(item);
            h.assertValueEqual(stack.getMaxDamage(),500,"Thaumium tool durability "+suffix);
            h.assertValueEqual(item.getEnchantmentValue(),22,"Thaumium tool enchantability "+suffix);
            h.assertTrue(item.isValidRepairItem(stack,ingot),"Thaumium tool repair material "+suffix);
        }
        for(var entry:Map.of("helm",275,"chest",400,"legs",375,"boots",325).entrySet()) {
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_"+entry.getKey()));
            h.assertValueEqual(new ItemStack(item).getMaxDamage(),entry.getValue(),"Thaumium armor durability "+entry.getKey());
            h.assertTrue(item.isValidRepairItem(new ItemStack(item),ingot),"Thaumium armor repair material");
        }
        var candle=AlchemyMaterials.CANDLES.get("candle_white").get();var p=new BlockPos(1,2,1);
        h.setBlock(p.below(),Blocks.STONE);h.setBlock(p,candle);
        h.assertValueEqual(h.getBlockState(p).getLightEmission(),14,"Original candle light level");
        h.assertTrue(h.getBlockState(p).getCollisionShape(h.getLevel(),h.absolutePos(p)).isEmpty(),"Candle must not obstruct movement");
        h.setBlock(p.below(),Blocks.AIR);
        h.assertTrue(h.getBlockState(p).isAir(),"Unsupported candle remained floating");
        h.assertValueEqual(AlchemyMaterials.rareEarthChance(Blocks.DIAMOND_ORE),.05f,"Original rare earth chance");
        h.assertValueEqual(AlchemyMaterials.rareEarthChance(Blocks.DEEPSLATE_DIAMOND_ORE),.05f,"Deepslate extension preserves chance");
        h.succeed();
    }
    private AlchemyRecipeGameTests() {}
}
