package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.crafting.*;
import thaumcraft.port.essentia.*;
import thaumcraft.port.machine.MachineContent;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class IngredientMatchingGameTests {
    private static final ResourceLocation MISSING=ResourceLocation.parse("thaumcraft:test_missing_ingredient_tag");
    @GameTest(template="empty",timeoutTicks=60)
    public static void emptyTagPlaceholdersNeverMatchButExplicitBarrierIngredientsStillDo(GameTestHelper h){
        var missing=Ingredient.of(TagKey.create(Registries.ITEM,MISSING));var barrier=new ItemStack(Items.BARRIER);
        var alternatives=CompoundIngredient.of(missing,Ingredient.of(Items.STONE));
        h.assertFalse(IngredientMatching.test(missing,barrier),"Empty item tag accepted its display barrier");
        h.assertFalse(IngredientMatching.test(alternatives,barrier),"Empty-tag alternative accepted its display barrier");
        h.assertTrue(IngredientMatching.test(alternatives,new ItemStack(Items.STONE)),"Real alternative stopped matching");
        h.assertValueEqual(IngredientMatching.getItems(missing).length,0,"Unavailable tag leaked into recipe display");
        h.assertValueEqual(IngredientMatching.getItems(alternatives).length,1,"Mixed alternative retained a fake display ingredient");
        var explicit=Ingredient.of(Items.BARRIER);
        var difference=DifferenceIngredient.of(explicit,missing);
        h.assertTrue(IngredientMatching.test(explicit,barrier),"Explicit barrier recipe was incorrectly banned");
        h.assertTrue(IngredientMatching.test(difference,barrier),"Empty subtracted tag excluded a valid explicit item");
        h.assertFalse(IngredientMatching.test(new IntersectionIngredient(List.of(explicit,missing)).toVanilla(),barrier),"Intersection accepted an empty tag");
        h.assertFalse(IngredientMatching.test(new BlockTagIngredient(TagKey.create(Registries.BLOCK,MISSING)).toVanilla(),barrier),"Empty block tag accepted its display barrier");
        var input=CraftingInput.of(1,1,List.of(barrier));
        for(boolean shapeless:List.of(false,true)){
            var forbidden=new ArcaneRecipe("",0,Map.of(),ShapedRecipePattern.of(Map.of('I',alternatives),"I"),new ItemStack(Items.DIAMOND),shapeless);
            var allowed=new ArcaneRecipe("",0,Map.of(),ShapedRecipePattern.of(Map.of('I',difference),"I"),new ItemStack(Items.DIAMOND),shapeless);
            h.assertFalse(forbidden.matches(input,h.getLevel()),"Arcane matching used empty-tag display placeholder, shapeless="+shapeless);
            h.assertTrue(allowed.matches(input,h.getLevel()),"Arcane matching rejected explicit barrier, shapeless="+shapeless);
        }
        h.assertFalse(new CrucibleRecipe("",alternatives,Map.of("ignis",1),new ItemStack(Items.DIAMOND)).matches(new SingleRecipeInput(barrier),h.getLevel()),"Crucible accepted empty-tag alternative");
        h.assertTrue(new CrucibleRecipe("",explicit,Map.of("ignis",1),new ItemStack(Items.DIAMOND)).matches(new SingleRecipeInput(barrier),h.getLevel()),"Crucible rejected explicit barrier ingredient");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void thaumatoriumChoicesAndSelectionUseActualTagMembership(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var original=List.copyOf(manager.getRecipes());var recipes=new ArrayList<RecipeHolder<?>>(original);
        var missingId=ResourceLocation.parse("thaumcraft:test_missing_catalyst");var explicitId=ResourceLocation.parse("thaumcraft:test_explicit_barrier");
        recipes.add(new RecipeHolder<>(missingId,new CrucibleRecipe("",Ingredient.of(TagKey.create(Registries.ITEM,MISSING)),Map.of("ignis",1),new ItemStack(Items.DIAMOND))));
        recipes.add(new RecipeHolder<>(explicitId,new CrucibleRecipe("",Ingredient.of(Items.BARRIER),Map.of("ignis",1),new ItemStack(Items.IRON_INGOT))));
        var pos=new BlockPos(1,3,1);h.setBlock(pos.below(),MachineContent.CRUCIBLE.get());h.setBlock(pos,EssentiaContent.THAUMATORIUM.get());h.setBlock(pos.above(),EssentiaContent.THAUMATORIUM_TOP.get());
        var machine=(ThaumatoriumEntity)h.getBlockEntity(pos);var player=GameTestPlayers.create(h);
        player.setPos(machine.getBlockPos().getX()+.5,machine.getBlockPos().getY(),machine.getBlockPos().getZ()+.5);
        try{
            manager.replaceRecipes(recipes);machine.setItem(0,new ItemStack(Items.BARRIER));
            h.assertFalse(machine.choices(player).stream().anyMatch(r->r.id().equals(missingId)),"Thaumatorium offered an unavailable tag recipe");
            h.assertTrue(machine.choices(player).stream().anyMatch(r->r.id().equals(explicitId)),"Thaumatorium hid an explicit barrier recipe");
            machine.choose(player,missingId);h.assertTrue(machine.selected.isEmpty(),"Thaumatorium selected an empty tag through its placeholder");
            machine.choose(player,explicitId);h.assertTrue(machine.selected.containsKey(explicitId),"Thaumatorium rejected a valid explicit barrier recipe");
        }finally{manager.replaceRecipes(original);}
        h.succeed();
    }
    private IngredientMatchingGameTests(){}
}
