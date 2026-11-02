package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.auromancy.pech.*;
import thaumcraft.port.research.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class PechGameTests {
    private static PechEntity pech(GameTestHelper helper,BlockPos pos){var pech=helper.spawn(PechContent.PECH.get(),pos);pech.setNoAi(true);return pech;}
    @GameTest(template="elemental_tools_empty")
    public static void pearlTamesByConsumingOneThenLootPackPersists(GameTestHelper helper){
        var pech=pech(helper,new BlockPos(5,3,5));var pearls=new ItemStack(Items.ENDER_PEARL,3);
        var rest=pech.pickupItem(pearls);helper.assertTrue(pech.isTamed(),"Value fifteen pearl must always beat nextInt(10)");helper.assertValueEqual(rest.getCount(),2,"Taming must consume one pearl, not the entire stack");helper.assertTrue(pech.loot.stream().allMatch(ItemStack::isEmpty),"Consumed friendship gift entered loot pack");
        helper.assertTrue(pech.pickupItem(rest).isEmpty(),"Friendly pech did not store the remaining stack");helper.assertValueEqual(pech.loot.getFirst().getCount(),2,"Friendly pech consumed another taming gift");pech.setPechType(1);
        var saved=new CompoundTag();pech.addAdditionalSaveData(saved);var loaded=PechContent.PECH.get().create(helper.getLevel());loaded.readAdditionalSaveData(saved);
        helper.assertTrue(loaded.isTamed(),"Reload lost friendship");helper.assertValueEqual(loaded.getPechType(),1,"Reload lost profession");helper.assertValueEqual(loaded.loot.getFirst().getCount(),2,"Reload lost collected loot");pech.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void attackedPechAlertsNearbyFriendsAndEndsTrading(GameTestHelper helper){
        var first=pech(helper,new BlockPos(5,3,5));var second=pech(helper,new BlockPos(9,3,5));first.setTamed(true);second.setTamed(true);var player=GameTestPlayers.create(helper);
        first.hurt(player.damageSources().playerAttack(player),1);
        helper.assertTrue(first.getAnger()>=400&&first.getAnger()<800,"Pech anger timer differs from source");helper.assertTrue(second.getAnger()>=400&&second.getAnger()<800,"Nearby pech did not join revenge");helper.assertTrue(first.getTarget()==player&&second.getTarget()==player,"Revenge target not shared");helper.assertFalse(first.isTamed()||second.isTamed(),"Attack did not end friendships");first.discard();second.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void mageBarterCanYieldRealWandAndCannotRollOverOccupiedOutputs(GameTestHelper helper){
        var pech=pech(helper,new BlockPos(5,3,5));pech.setPechType(1);pech.setTamed(true);var player=GameTestPlayers.create(helper);player.setPos(pech.position());var menu=new PechMenu(7,player.getInventory(),pech);
        helper.getLevel().random.setSeed(4834);boolean found=false;
        for(int roll=0;roll<256&&!found;roll++){
            pech.setTamed(true);menu.tradeInventory().setItem(0,new ItemStack(Items.ENDER_PEARL,2));for(int slot=1;slot<5;slot++)menu.tradeInventory().setItem(slot,ItemStack.EMPTY);
            helper.assertTrue(menu.clickMenuButton(player,0),"Valid pech trade button rejected");helper.assertValueEqual(menu.tradeInventory().getItem(0).getCount(),1,"A trade did not consume exactly one valued item");
            for(int slot=1;slot<5;slot++)found|=menu.tradeInventory().getItem(slot).is(PechContent.WAND.get());
        }
        helper.assertTrue(found,"Original mage pool never yielded the real registered Pech Wand");pech.setTamed(true);menu.tradeInventory().setItem(1,new ItemStack(Items.DIRT));int before=menu.tradeInventory().getItem(0).getCount();menu.clickMenuButton(player,0);helper.assertValueEqual(menu.tradeInventory().getItem(0).getCount(),before,"Occupied output allowed another paid roll");
        menu.removed(player);helper.assertFalse(pech.trading,"Closed menu retained trading lock");helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,pech.getBoundingBox().inflate(4)).stream().allMatch(item->item.getPersistentData().getBoolean("PechDrop")),"Returned trade items can be immediately stolen back");pech.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void unvaluedTradeAndDistantButtonCannotConsumeItems(GameTestHelper helper){
        var pech=pech(helper,new BlockPos(5,3,5));pech.setTamed(true);var player=GameTestPlayers.create(helper);player.setPos(pech.position());var menu=new PechMenu(8,player.getInventory(),pech);menu.tradeInventory().setItem(0,new ItemStack(Items.COBBLESTONE,3));menu.clickMenuButton(player,0);helper.assertValueEqual(menu.tradeInventory().getItem(0).getCount(),3,"Ordinary cobblestone was accepted as valuable");
        menu.tradeInventory().setItem(0,new ItemStack(Items.ENDER_PEARL,3));player.setPos(pech.position().add(20,0,0));helper.assertFalse(menu.clickMenuButton(player,0),"Distant forged trade packet was accepted");helper.assertValueEqual(menu.tradeInventory().getItem(0).getCount(),3,"Distant forged trade consumed input");menu.removed(player);pech.discard();helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void pechWandScanAndConsumptionAreDistinctResearchSteps(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());var wand=new ItemStack(PechContent.WAND.get(),2);player.setItemInHand(InteractionHand.OFF_HAND,wand);
        Scanning.scan(player,new ScanObservation(Scanning.itemSubject(wand),Map.of(),ScanDiscoveries.itemFlags(wand)),false);
        helper.assertTrue(state.knows("!Pechwand"),"Scanning actual wand did not unlock Curse prerequisite");helper.assertFalse(state.knows("FOCUSPECH"),"Scanning consumed-curio research was awarded prematurely");
        PechContent.WAND.get().use(helper.getLevel(),player,InteractionHand.OFF_HAND);helper.assertValueEqual(wand.getCount(),2,"Wand was consumed before Base Auromancy");helper.assertFalse(state.knows("FOCUSPECH"),"Unknown base research was bypassed");
        state.flag("BASEAUROMANCY");var before=new HashMap<>(state.knowledge());PechContent.WAND.get().use(helper.getLevel(),player,InteractionHand.OFF_HAND);
        helper.assertValueEqual(wand.getCount(),1,"Offhand research curio consumed wrong stack");helper.assertTrue(state.knows("FOCUSPECH"),"Consumption did not grant original FOCUSPECH flag");
        int observation=0,theory=0;for(String category:ResearchCatalog.CATEGORIES){observation+=state.knowledge("OBSERVATION",category)-before.getOrDefault("OBSERVATION;"+category,0);theory+=state.knowledge("THEORY",category)-before.getOrDefault("THEORY;"+category,0);}
        helper.assertTrue(observation>=5&&observation<=8,"Wand observation grant differs from 16/3 .. 16/2");helper.assertTrue(theory>=6&&theory<=8,"Wand theory grant differs from 32/5 .. 32/4");helper.succeed();
    }
    @GameTest(template="elemental_tools_empty")
    public static void professionAspectsAndActualWeightedTradePoolsMatchSource(GameTestHelper helper){
        var pech=pech(helper,new BlockPos(5,3,5));for(int type=0;type<3;type++){
            pech.setPechType(type);var aspects=AspectTags.forEntity(pech);helper.assertValueEqual(aspects.get(Aspect.MAN),10,"Pech lost human aspect");helper.assertValueEqual(aspects.get(type==0?Aspect.DESIRE:type==1?Aspect.AVERSION:Aspect.MAGIC),5,"Profession aspect differs from source NBT tag");
            var entries=PechTrades.entries(helper.getLevel(),type);for(int tier=1;tier<=5;tier++){int t=tier;helper.assertTrue(entries.stream().anyMatch(entry->entry.tier()==t),"Pech profession missing trade tier "+tier);}
            helper.assertValueEqual(entries.stream().filter(entry->entry.tier()==3&&entry.stack().is(Items.EXPERIENCE_BOTTLE)).count(),2L,"Duplicate experience weighting was removed");
        }
        var mage=PechTrades.entries(helper.getLevel(),1);var found=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:amulet_vis_found"));helper.assertTrue(mage.stream().anyMatch(entry->entry.tier()==5&&entry.stack().is(found)&&found!=Items.AIR),"Mage trade substituted crafted amulet for found variant");
        var stalker=PechTrades.entries(helper.getLevel(),2);helper.assertTrue(stalker.stream().anyMatch(entry->entry.tier()==1&&entry.stack().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:candle_black")))),"Original dye-damage ordering omitted black candle");helper.assertFalse(stalker.stream().anyMatch(entry->entry.tier()==1&&entry.stack().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:candle_white")))),"Original fifteen-color table should exclude white");
        var forager=PechTrades.entries(helper.getLevel(),0);helper.assertTrue(forager.stream().anyMatch(entry->entry.tier()==5&&entry.stack().is(Items.TOTEM_OF_UNDYING)),"Original cross-pool totem entry moved to wrong profession");helper.assertTrue(PechTrades.entries(helper.getLevel(),2).stream().anyMatch(entry->entry.tier()==4&&entry.stack().isEmpty()),"Unassigned original eldritch-eye trade was invented");pech.discard();helper.succeed();
    }
    private PechGameTests(){}
}
