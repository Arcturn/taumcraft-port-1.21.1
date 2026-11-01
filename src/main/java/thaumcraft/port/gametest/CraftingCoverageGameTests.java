package thaumcraft.port.gametest;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.*;
import thaumcraft.port.data.CraftingRecipeFixtures;
import thaumcraft.port.machine.*;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

/** Every original recipe is exercised through the live manager and the actual crafting transaction. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CraftingCoverageGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void everyOriginalArcaneRecipeCraftsAndPaysExactlyOnce(GameTestHelper h) {
        var relative=new BlockPos(1,2,1);h.setBlock(relative,MachineContent.WORKBENCH.get());
        var table=(WorkbenchEntity)h.getBlockEntity(relative);var pos=h.absolutePos(relative);var player=GameTestPlayers.create(h);
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
        ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        var menu=new WorkbenchMenu(1,player.getInventory(),table,pos);var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var aura=AuraSystem.get(h.getLevel(),pos);int crafted=0;
        for(var expected:CraftingRecipeFixtures.entries("arcane")) {
            String id=expected.get("recipe").getAsString();var holder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
            h.assertTrue(holder.value() instanceof ArcaneRecipe,"Original recipe absent from live arcane registry: "+id);var recipe=(ArcaneRecipe)holder.value();
            var original=CraftingRecipeFixtures.grid(expected,ops);int width=expected.get("width").getAsInt(),height=expected.get("height").getAsInt();
            table.clearContent();player.getInventory().clearContent();menu.setCarried(ItemStack.EMPTY);
            var before=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
            // Place at the bottom/right edge: real menu must normalize the grid before matching/payment.
            int left=3-width,top=3-height;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int slot=left+x+(top+y)*3;var item=original.get(x+y*width).copy();table.setItem(slot,item);before.set(slot,item.copy());
            }
            var costs=CraftingRecipeFixtures.amounts(expected.getAsJsonObject("crystals"));
            for(int i=0;i<WorkbenchMenu.PRIMALS.size();i++) {
                var aspect=WorkbenchMenu.PRIMALS.get(i);int cost=costs.getOrDefault(aspect.id(),0);
                if(cost>0)table.setItem(9+i,CrystalItem.stack(aspect).copyWithCount(cost));
            }
            aura.drain(false,Float.MAX_VALUE,false);aura.addVis(1000);
            menu.broadcastChanges();CraftingRecipeFixtures.verifyOutput(expected,menu.getSlot(0).getItem(),ops);
            menu.clicked(0,0,ClickType.PICKUP,player);
            CraftingRecipeFixtures.verifyOutput(expected,menu.getCarried(),ops);
            h.assertValueEqual(aura.vis(),1000.0f-expected.get("vis").getAsInt(),"Original vis was not charged exactly once: "+id);
            for(int i=9;i<15;i++)h.assertTrue(table.getItem(i).isEmpty(),"Original crystal cost was not fully consumed: "+id);
            for(int i=0;i<9;i++) {
                var remainder=before.get(i).isEmpty()?ItemStack.EMPTY:before.get(i).getCraftingRemainingItem();
                h.assertTrue(ItemStack.matches(table.getItem(i),remainder),"Input was not consumed exactly once or its container was lost: "+id+" slot "+i);
            }
            h.assertTrue(recipe.matches(CraftingInput.of(width,height,original),h.getLevel()),"Live recipe no longer matches its independent original fixture: "+id);
            crafted++;
        }
        h.assertValueEqual(crafted,89,"Not every original arcane recipe reached its real transaction");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void everyAvailableOriginalCrucibleRecipeCraftsAndPaysExactlyOnce(GameTestHelper h) {
        var relative=new BlockPos(1,2,1);h.setBlock(relative,MachineContent.CRUCIBLE.get());var crucible=(CrucibleEntity)h.getBlockEntity(relative);
        var pos=h.absolutePos(relative);var player=GameTestPlayers.create(h);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
        ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID()).completeCatalog();
        var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);int crafted=0,unavailable=0;
        for(var expected:CraftingRecipeFixtures.entries("crucible")) {
            String id=expected.get("recipe").getAsString();var holder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow();
            h.assertTrue(holder.value() instanceof CrucibleRecipe,"Original recipe absent from live crucible registry: "+id);var recipe=(CrucibleRecipe)holder.value();
            var input=CraftingRecipeFixtures.sample(expected.getAsJsonObject("catalyst"),ops);
            if(input.isEmpty()) {
                var items=CraftingRecipeFixtures.conditionalSamples(expected.getAsJsonObject("catalyst"));if(!items.isEmpty())input=items.getFirst().copy();
                else {h.assertTrue(expected.get("conditional").getAsBoolean(),"Required catalyst is unavailable: "+id);unavailable++;continue;}
            }
            input.setCount(2);var original=input.copy();var aspects=new LinkedHashMap<Aspect,Integer>();
            CraftingRecipeFixtures.amounts(expected.getAsJsonObject("aspects")).forEach((key,value)->aspects.put(Aspect.byId(key).orElseThrow(),value));
            crucible.state.restore(1000,200,-100,aspects);
            h.assertTrue(crucible.acceptItem(input,player),"Original crucible transaction rejected: "+id);
            h.assertTrue(input.getCount()==1&&ItemStack.isSameItemSameComponents(input,original),"Crucible did not consume exactly one catalyst: "+id);
            h.assertValueEqual(crucible.state.water(),950,"Crucible did not charge exactly 50mB for "+id);
            h.assertValueEqual(crucible.state.total(),0,"Crucible did not charge the original aspect costs: "+id);
            var result=CraftingRecipeFixtures.stack(expected.getAsJsonObject("result"),ops);
            var outputs=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1),item->item.isAlive()&&item.getTags().contains(SpecialItemEntity.CRUCIBLE_OUTPUT_TAG));
            h.assertValueEqual(outputs.stream().mapToInt(item->item.getItem().getCount()).sum(),result.getCount(),"Crucible output count changed: "+id);
            for(var output:outputs) {
                h.assertTrue(ItemStack.isSameItemSameComponents(output.getItem(),result),"Crucible output identity/components changed: "+id);output.discard();
            }
            crafted++;
        }
        h.assertValueEqual(crafted+unavailable,78,"Not every original crucible recipe was checked");
        h.assertTrue(crafted>=75&&unavailable<=3,"Only tin/silver/lead may be absent in the base game");h.succeed();
    }
    private CraftingCoverageGameTests() {}
}
