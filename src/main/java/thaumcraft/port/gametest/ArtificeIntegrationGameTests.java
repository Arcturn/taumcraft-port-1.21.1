package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.artifice.construct.ConstructContent;
import thaumcraft.port.research.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ArtificeIntegrationGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void amberRecipesProvideSurvivalLampMaterialWithoutConversionLoss(GameTestHelper h) {
        var amber=item("amber");
        var block=craft(h,"amber_to_block",2,List.of(amber,amber.copy(),amber.copy(),amber.copy()));
        h.assertTrue(block.is(item("amber_block").getItem()),"Four amber gems must produce the lamp's amber block");
        var gems=craft(h,"amber_block_to_amber",1,List.of(block));
        h.assertValueEqual(gems.getCount(),4,"Amber compression/decompression lost or created gems");
        var bricks=craft(h,"amber_block_to_brick",2,List.of(block,block.copy(),block.copy(),block.copy()));
        h.assertValueEqual(bricks.getCount(),4,"Four amber blocks must yield four bricks");
        var single=bricks.copyWithCount(1);
        var restored=craft(h,"amber_brick_to_block",2,List.of(single,single.copy(),single.copy(),single.copy()));
        h.assertTrue(restored.is(block.getItem())&&restored.getCount()==4,"Brick conversion must return four amber blocks");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void allThreeOwnedConstructsUnlockOriginalConstructDiscovery(GameTestHelper h) {
        var player=GameTestPlayers.create(h);
        for(var type:List.of(ConstructContent.BASIC.get(),ConstructContent.ADVANCED.get(),ConstructContent.BORE.get())) {
            var entity=type.create(h.getLevel());
            var flags=ScanDiscoveries.entityFlags(entity);
            h.assertTrue(flags.contains("f_golem"),"Construct scan lost ConfigResearch's EntityOwnedConstruct discovery: "+type);
            Scanning.scan(player,new ScanObservation("entity:"+BuiltInRegistries.ENTITY_TYPE.getKey(type),java.util.Map.of(),flags),false);
        }
        h.assertTrue(ResearchStorage.get(player.getServer()).player(player.getUUID()).knows("f_golem"),"Construct discovery did not reach persistent player research");
        h.succeed();
    }
    private static ItemStack craft(GameTestHelper h,String id,int width,List<ItemStack> items) {
        var holder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:"+id)).orElseThrow();
        var recipe=(CraftingRecipe)holder.value();var input=CraftingInput.of(width,items.size()/width,items);
        h.assertTrue(recipe.matches(input,h.getLevel()),"Survival crafting input rejected: "+id);
        return recipe.assemble(input,h.getLevel().registryAccess());
    }
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id)));}
    private ArtificeIntegrationGameTests() {}
}
