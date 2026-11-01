package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.data.InfusionRecipeFixtures;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionRecipeGameTests {
    @GameTest(template="empty",timeoutTicks=20)
    public static void everyRegisteredInfusionRequiresResearchAndExactUnorderedComponents(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var state=ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        var recipes=helper.getLevel().getRecipeManager().getAllRecipesFor(InfusionRecipes.TYPE.get());
        helper.assertTrue(recipes.size()>=28,"Expected all 28 original Infusion-category matrix recipes");
        for(var holder:recipes){
            var recipe=holder.value();var central=InfusionRecipeFixtures.sampleCentral(recipe);var components=InfusionRecipeFixtures.sampleComponents(recipe,central);
            helper.assertTrue(!recipe.matches(player,central,components),"Fresh player bypassed research: "+holder.id());
        }
        state.completeCatalog();
        for(var holder:recipes){
            var recipe=holder.value();var central=InfusionRecipeFixtures.sampleCentral(recipe);var components=new ArrayList<>(InfusionRecipeFixtures.sampleComponents(recipe,central));Collections.reverse(components);
            helper.assertTrue(recipe.matches(player,central,components),"Unlocked recipe rejected reversed pedestals: "+holder.id());
            helper.assertTrue(!recipe.result(player,central,components).isEmpty(),"Empty actual infusion result: "+holder.id());
            components.add(new ItemStack(Items.DIRT));helper.assertTrue(!recipe.matches(player,central,components),"Extra pedestal accepted: "+holder.id());
            components.removeLast();components.removeLast();helper.assertTrue(!recipe.matches(player,central,components),"Missing pedestal accepted: "+holder.id());
        }
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void discoveredResearchUnlocksAllInfusionKindsBeforePageCompletion(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var state=ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());
        for(var holder:helper.getLevel().getRecipeManager().getAllRecipesFor(InfusionRecipes.TYPE.get())){
            var recipe=holder.value();String[] gate=recipe.research().split("@");int stage=gate.length>1?Integer.parseInt(gate[1]):1;
            state.stage(gate[0],stage);
            helper.assertFalse(state.knows(gate[0]),"Test must leave the research page incomplete: "+gate[0]);
            var central=InfusionRecipeFixtures.sampleCentral(recipe);var components=InfusionRecipeFixtures.sampleComponents(recipe,central);
            helper.assertTrue(recipe.matches(player,central,components),"Discovered recipe still required page completion: "+holder.id());
        }
        var gated=new InfusionRecipe("FOCUSADVANCED@2",Ingredient.of(Items.STONE),List.of(Ingredient.of(Items.DIRT)),Map.of("aer",1),0,new ItemStack(Items.DIAMOND),"item","");
        state.stage("FOCUSADVANCED",1);
        helper.assertFalse(gated.matches(player,new ItemStack(Items.STONE),List.of(new ItemStack(Items.DIRT))),"Discovery bypassed an explicit stage requirement");
        state.stage("FOCUSADVANCED",2);
        helper.assertTrue(gated.matches(player,new ItemStack(Items.STONE),List.of(new ItemStack(Items.DIRT))),"Explicit infusion stage did not unlock");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void emptyTagDisplayBarriersCannotSupplyCentralOrPedestalIngredients(GameTestHelper helper){
        var missing=Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse("thaumcraft:test_missing_infusion_tag")));
        var alternative=CompoundIngredient.of(missing,Ingredient.of(Items.GOLD_INGOT));var barrier=new ItemStack(Items.BARRIER);var stone=new ItemStack(Items.STONE);
        var missingCentral=new InfusionRecipe("",alternative,List.of(Ingredient.of(Items.STONE)),Map.of("aer",1),0,new ItemStack(Items.DIAMOND),"item","");
        helper.assertFalse(missingCentral.matches(new InfusionInput(barrier,List.of(stone)),helper.getLevel()),"Central ingredient accepted an empty tag's display barrier");
        helper.assertTrue(missingCentral.matches(new InfusionInput(new ItemStack(Items.GOLD_INGOT),List.of(stone)),helper.getLevel()),"Valid central alternative stopped matching");
        var missingPedestal=new InfusionRecipe("",Ingredient.of(Items.STONE),List.of(alternative),Map.of("aer",1),0,new ItemStack(Items.DIAMOND),"item","");
        helper.assertFalse(missingPedestal.matches(new InfusionInput(stone,List.of(barrier)),helper.getLevel()),"Pedestal ingredient accepted an empty tag's display barrier");
        helper.assertTrue(missingPedestal.matches(new InfusionInput(stone,List.of(new ItemStack(Items.GOLD_INGOT))),helper.getLevel()),"Valid pedestal alternative stopped matching");
        var explicit=new InfusionRecipe("",Ingredient.of(Items.BARRIER),List.of(Ingredient.of(Items.BARRIER)),Map.of("aer",1),0,new ItemStack(Items.DIAMOND),"item","");
        helper.assertTrue(explicit.matches(new InfusionInput(barrier,List.of(barrier.copy())),helper.getLevel()),"Explicit barrier items must remain valid ingredients");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void runicShieldingPreservesAugmentsAndScalesCosts(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        var recipe=recipe(helper,"runic_armor");var central=new ItemStack(Items.IRON_HELMET);central.setDamageValue(34);central.set(DataComponents.CUSTOM_NAME,Component.literal("Named armor"));InfusionAugments.setWarp(central,2);
        for(int level=0;level<6;level++){
            InfusionAugments.setRunic(central,level);var components=InfusionRecipeFixtures.sampleComponents(recipe,central);int vis=20+(20<<level);
            helper.assertTrue(recipe.matches(player,central,components),"Runic level rejected "+level);
            helper.assertTrue(recipe.aspects(player,central,components).equals(Map.of(Aspect.PROTECT,vis,Aspect.CRYSTAL,vis/2,Aspect.ENERGY,vis/2)),"Runic aspect scaling "+level);
            helper.assertValueEqual(recipe.instability(player,central,components),5+level/2,"Runic instability scaling");
            var result=recipe.result(player,central,components);helper.assertValueEqual(InfusionAugments.getRunic(result),level+1,"Runic level output");
            helper.assertTrue(result.getDamageValue()==34&&result.getHoverName().equals(central.getHoverName())&&InfusionAugments.getWarp(result)==2,"Runic upgrade lost central data");
            helper.assertValueEqual(InfusionAugments.getRunic(central),level,"Recipe mutated input");
        }
        var discovered=GameTestPlayers.create(helper);ResearchStorage.get(helper.getLevel().getServer()).player(discovered.getUUID()).stage("RUNICSHIELDING",1);
        var freshArmor=new ItemStack(Items.LEATHER_BOOTS);
        helper.assertTrue(recipe.matches(discovered,freshArmor,InfusionRecipeFixtures.sampleComponents(recipe,freshArmor)),"Original runic recipe unlocks on research discovery");
        helper.assertTrue(!recipe.matches(player,new ItemStack(Items.DIRT),InfusionRecipeFixtures.sampleComponents(recipe,new ItemStack(Items.DIRT))),"Runic augmentation accepted a non-equipment block");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void enchantmentsRespectToolClassesCapsAndSurcharge(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        for(String id:List.of("arcing","burrowing","collector","destructive","essence","lamplight","refining","sounding")){
            var recipe=recipe(helper,"ie_"+id);var central=InfusionRecipeFixtures.sampleCentral(recipe);var components=InfusionRecipeFixtures.sampleComponents(recipe,central);
            InfusionAugments.setRunic(central,2);central.setDamageValue(17);
            for(int level=0;level<InfusionAugments.maxLevel(id);level++){
                helper.assertTrue(recipe.matches(player,central,components),"Eligible tool rejected: "+id+" "+level);
                var result=recipe.result(player,central,components);helper.assertValueEqual(InfusionAugments.getEnchantment(result,id),level+1,"Infusion enchant upgrade");
                helper.assertTrue(result.getDamageValue()==17&&InfusionAugments.getRunic(result)==2,"Enchant lost central damage/runic data");central=result;
            }
            helper.assertTrue(!recipe.matches(player,central,components),"Enchantment exceeded level cap: "+id);
            helper.assertTrue(recipe.aspects(player,central,components).isEmpty(),"Capped enchantment still reported another level's cost: "+id);
            helper.assertTrue(recipe.aspects(player,ItemStack.EMPTY,components).isEmpty(),"Missing central item still reported an enchantment cost: "+id);
            helper.assertTrue(!recipe.matches(player,new ItemStack(Items.DIRT),components),"Enchantment accepted ineligible item: "+id);
        }
        var pick=new ItemStack(Items.IRON_PICKAXE);InfusionAugments.setEnchantment(pick,"refining",1);InfusionAugments.setEnchantment(pick,"sounding",1);
        helper.assertTrue(recipe(helper,"ie_refining").aspects(player,pick,List.of()).equals(Map.of(Aspect.ORDER,186,Aspect.EXCHANGE,139)),"Other infusion enchants must add the original .33 surcharge");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void primalCrusherExposesBothOriginalToolClassesToInfusionEnchanting(GameTestHelper helper){
        var central=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primal_crusher")));
        helper.assertFalse(central.isEmpty(),"Primal crusher is missing");
        for(String id:List.of("arcing","burrowing","collector","destructive","essence","lamplight","refining","sounding"))
            helper.assertTrue(InfusionRecipe.canEnchant(central,id),"Crusher lost its original pickaxe/shovel/weapon eligibility: "+id);
        helper.assertFalse(InfusionRecipe.canEnchant(new ItemStack(Items.IRON_SHOVEL),"refining"),"A shovel gained the crusher's pickaxe eligibility");
        var recipe=recipe(helper,"ie_sounding");var components=InfusionRecipeFixtures.sampleComponents(recipe,central);
        helper.assertTrue(recipe.matches(new InfusionInput(central,components),helper.getLevel()),"Crusher could not receive a pickaxe infusion enchantment");
        var output=recipe.assemble(new InfusionInput(central,components),helper.getLevel().registryAccess());
        helper.assertValueEqual(InfusionAugments.getEnchantment(output,"sounding"),1,"Crusher sounding result");
        helper.assertTrue(InfusionAugments.getEnchantment(output,"destructive")==1&&InfusionAugments.getEnchantment(output,"refining")==1,"Crusher lost its inherent infusions");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void fortressGogglesAndMasksKeepEachOtherAndRunicData(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        var central=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:fortress_helm")));central.setDamageValue(28);InfusionAugments.setRunic(central,3);
        for(String id:List.of("helm_goggles","mask_grinning_devil","mask_angry_ghost","mask_sipping_fiend")){
            var recipe=recipe(helper,id);var components=InfusionRecipeFixtures.sampleComponents(recipe,central);helper.assertTrue(recipe.matches(player,central,components),"Fortress augment cannot match: "+id);central=recipe.result(player,central,components);
            helper.assertTrue(InfusionAugments.goggles(central)&&InfusionAugments.getRunic(central)==3&&central.getDamageValue()==28,"Fortress augmentation lost goggles/shield/damage");
        }
        helper.assertValueEqual(InfusionAugments.mask(central),2,"Final fortress mask");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void infusionRequiresExactAspectAndPotionComponents(GameTestHelper helper){
        var axe=recipe(helper,"elemental_axe");var central=InfusionRecipeFixtures.sampleCentral(axe);var components=new ArrayList<>(InfusionRecipeFixtures.sampleComponents(axe,central));
        components.set(0,CrystalItem.stack(Aspect.FIRE));helper.assertTrue(!axe.matches(new InfusionInput(central,components),helper.getLevel()),"Wrong primal crystal accepted");
        var life=recipe(helper,"verdant_heart_life");central=InfusionRecipeFixtures.sampleCentral(life);components=new ArrayList<>(InfusionRecipeFixtures.sampleComponents(life,central));
        var potion=new ItemStack(Items.POTION);potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.HEALING));components.set(2,potion);
        helper.assertTrue(!life.matches(new InfusionInput(central,components),helper.getLevel()),"Weak healing potion accepted in place of strong healing");
        potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.STRONG_HEALING));helper.assertTrue(life.matches(new InfusionInput(central,components),helper.getLevel()),"Strong healing potion rejected");
        potion.set(DataComponents.CUSTOM_NAME,Component.literal("Decorated strong healing"));
        potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.of(Potions.STRONG_HEALING),Optional.of(0x8855AA),List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,100,1))));
        helper.assertTrue(life.matches(new InfusionInput(central,components),helper.getLevel()),"Relaxed original Potion NBT rejected extra custom effects or color");
        var splash=PotionContents.createItemStack(Items.SPLASH_POTION,Potions.STRONG_HEALING);components.set(2,splash);
        helper.assertFalse(life.matches(new InfusionInput(central,components),helper.getLevel()),"Splash potion substituted for the original drinkable potion");
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void overlappingPedestalIngredientsUseMultisetMatching(GameTestHelper helper){
        var broad=Ingredient.of(Items.IRON_INGOT,Items.GOLD_INGOT);var narrow=Ingredient.of(Items.IRON_INGOT);
        helper.assertTrue(InfusionRecipe.matchComponents(List.of(broad,narrow),List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.GOLD_INGOT))),"Greedy matching consumed the only narrow ingredient");
        helper.assertTrue(!InfusionRecipe.matchComponents(List.of(narrow,narrow),List.of(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.GOLD_INGOT))),"One pedestal satisfied duplicate ingredients");helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void tripleTreatUsesThreeDistinctMeatsAndOriginalFood(GameTestHelper helper){
        var holder=helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:triple_meat_treat")).orElseThrow();
        var recipe=(TripleMeatTreatRecipe)holder.value();
        var kinds=InfusionFood.KINDS;
        for(int a=0;a<kinds.size();a++)for(int b=a+1;b<kinds.size();b++)for(int c=b+1;c<kinds.size();c++){
            var grid=net.minecraft.world.item.crafting.CraftingInput.of(2,2,List.of(new ItemStack(InfusionFood.NUGGETS.get(kinds.get(a)).get()),new ItemStack(Items.SUGAR),new ItemStack(InfusionFood.NUGGETS.get(kinds.get(b)).get()),new ItemStack(InfusionFood.NUGGETS.get(kinds.get(c)).get())));
            helper.assertTrue(recipe.matches(grid,helper.getLevel()),"Distinct meat triple rejected");
            helper.assertTrue(recipe.assemble(grid,helper.getLevel().registryAccess()).is(InfusionFood.TRIPLE_MEAT_TREAT.get()),"Treat recipe output");
        }
        var beef=new ItemStack(InfusionFood.NUGGETS.get("beef").get());
        helper.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(2,2,List.of(beef,beef.copy(),new ItemStack(InfusionFood.NUGGETS.get("fish").get()),new ItemStack(Items.SUGAR))),helper.getLevel()),"Duplicate meat substituted for three distinct kinds");
        var treat=new ItemStack(InfusionFood.TRIPLE_MEAT_TREAT.get());var food=treat.get(DataComponents.FOOD);
        helper.assertTrue(food!=null&&food.nutrition()==6&&Math.abs(food.saturation()-9.6f)<.001&&food.canAlwaysEat()&&food.effects().size()==1&&food.effects().getFirst().probability()==.66f&&food.effects().getFirst().effect().getDuration()==100,"Original treat nutrition, saturation or regeneration chance");
        var player=GameTestPlayers.create(helper);player.getFoodData().setFoodLevel(10);treat.finishUsingItem(helper.getLevel(),player);helper.assertValueEqual(player.getFoodData().getFoodLevel(),16,"Eating treat did not restore six hunger");
        helper.assertTrue(InfusionFood.smeltingBonus(new ItemStack(Items.BEEF)).is(InfusionFood.NUGGETS.get("beef").get())&&InfusionFood.smeltingBonus(new ItemStack(Items.COOKED_BEEF)).isEmpty(),"Smelting bonus must inspect raw input");
        var nuggetFood=beef.get(DataComponents.FOOD);helper.assertTrue(nuggetFood.nutrition()==1&&nuggetFood.saturation()==.6f&&nuggetFood.eatSeconds()==.5f,"Original ten-tick nugget food");helper.succeed();
    }
    private static InfusionRecipe recipe(GameTestHelper helper,String id){return helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:"+id)).map(r->(InfusionRecipe)r.value()).orElseThrow();}
    private InfusionRecipeGameTests(){}
}
