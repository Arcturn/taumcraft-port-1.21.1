package thaumcraft.port.gametest;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.machine.WorkbenchEntity;
import thaumcraft.port.machine.WorkbenchMenu;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

/** Real result clicks, including payment, event dispatch and returned containers. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WorkbenchCraftingGameTests {
    @GameTest(template="empty",timeoutTicks=60)
    public static void anUnavailableArcaneRecipeDoesNotBlockTheOrdinaryRecipe(GameTestHelper h) {
        var f=fixture(h,100);var manager=h.getLevel().getRecipeManager();
        var existing=List.copyOf(manager.getRecipes());var augmented=new ArrayList<RecipeHolder<?>>(existing);
        // A deliberate overlap exercises the original workbench's ordinary fallback.
        augmented.add(new RecipeHolder<>(ResourceLocation.parse("thaumcraft:test_arcane_overlap"),new ArcaneRecipe("UNLOCKARTIFICE",10,Map.of("aer",1),
            ShapedRecipePattern.of(Map.of('L',Ingredient.of(Items.OAK_LOG)),"L"),new ItemStack(Items.DIAMOND),false,-1)));
        try {
            manager.replaceRecipes(augmented);
            f.table().setItem(8,new ItemStack(Items.OAK_LOG,3));
            f.menu().clicked(0,0,ClickType.PICKUP,f.player());
            h.assertTrue(f.menu().getCarried().is(Items.OAK_PLANKS)&&f.menu().getCarried().getCount()==4,"Missing arcane crystals blocked the ordinary recipe");
            f.menu().setCarried(ItemStack.EMPTY);crystals(f,Aspect.AIR,1);
            f.menu().clicked(0,0,ClickType.PICKUP,f.player());
            h.assertTrue(f.menu().getCarried().is(Items.OAK_PLANKS)&&f.menu().getCarried().getCount()==4,"Locked arcane research blocked the ordinary recipe");
            h.assertValueEqual(f.table().getItem(9).getCount(),1,"Ordinary fallback spent arcane crystals");
            h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),100.0f,"Ordinary fallback spent vis");
            f.menu().setCarried(ItemStack.EMPTY);
            ResearchStorage.get(h.getLevel().getServer()).player(f.player().getUUID()).flag("UNLOCKARTIFICE");
            f.menu().clicked(0,0,ClickType.PICKUP,f.player());
            h.assertTrue(f.menu().getCarried().is(Items.DIAMOND),"An available arcane recipe must take priority over the ordinary recipe");
            h.assertTrue(f.table().getItem(9).isEmpty(),"Arcane recipe failed to consume its crystal");
            h.assertTrue(f.table().getItem(8).isEmpty(),"Three transactions did not consume exactly three inputs");
            h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),90.0f,"Arcane recipe failed to charge its vis");
        } finally {manager.replaceRecipes(existing);}
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void shiftCraftingKeepsOriginalRecipeOrderAndStopsWhenTheOutputChanges(GameTestHelper h) {
        var f=fixture(h,100);
        ResearchStorage.get(h.getLevel().getServer()).player(f.player().getUUID()).flag("TUBES");
        f.table().setItem(8,new ItemStack(item("tube"),3));
        crystals(f,Aspect.EARTH,2);crystals(f,Aspect.WATER,4);
        var observer=new CraftedObserver(f.player());NeoForge.EVENT_BUS.register(observer);
        try {
            f.menu().broadcastChanges();
            h.assertTrue(f.menu().getSlot(0).getItem().is(item("tube_restrict")),"Original registry chooses the restrictor before the one-way tube when both crystal costs fit");
            f.menu().clicked(0,0,ClickType.QUICK_MOVE,f.player());
            h.assertValueEqual(count(f.player(),item("tube_restrict")),2,"Shift-click must repeat the affordable recipe");
            h.assertValueEqual(count(f.player(),item("tube_oneway")),0,"The same shift-click must stop when the output changes");
            h.assertValueEqual(f.table().getItem(8).getCount(),1,"Shift-click consumed too many or too few catalysts");
            h.assertTrue(f.table().getItem(10).isEmpty(),"Every repeated craft must consume an earth crystal");
            h.assertValueEqual(f.table().getItem(12).getCount(),4,"Restrictor crafting consumed the other recipe's water crystals");
            h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),80.0f,"Repeated crafting did not charge ten vis per craft");
            h.assertValueEqual(observer.count,2,"Each crafted output must fire exactly one crafting event");
            h.assertValueEqual(observer.lastCatalystCount,2,"Crafting event must retain the pre-consumption input matrix");
            h.assertTrue(f.menu().getSlot(0).getItem().is(item("tube_oneway")),"Exhausting earth crystals must reveal the water-crystal recipe");
            var holder=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:tube_restrict")).orElseThrow();
            h.assertTrue(f.player().getRecipeBook().contains(holder),"Successful workbench crafting must award its recipe");
        } finally {NeoForge.EVENT_BUS.unregister(observer);}
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void shiftCraftingStopsAtVisAndRefusesAFullInventoryWithoutPayment(GameTestHelper h) {
        var f=fixture(h,25);
        ResearchStorage.get(h.getLevel().getServer()).player(f.player().getUUID()).flag("TUBES");
        f.table().setItem(0,new ItemStack(item("tube"),4));crystals(f,Aspect.EARTH,4);
        for(int i=0;i<f.player().getInventory().items.size();i++)f.player().getInventory().setItem(i,new ItemStack(Items.STONE,64));
        f.menu().clicked(0,0,ClickType.QUICK_MOVE,f.player());
        h.assertValueEqual(f.table().getItem(0).getCount(),4,"A full inventory consumed input");
        h.assertValueEqual(f.table().getItem(10).getCount(),4,"A full inventory consumed crystals");
        h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),25.0f,"A full inventory consumed vis");
        f.player().getInventory().setItem(0,ItemStack.EMPTY);
        f.menu().clicked(0,0,ClickType.QUICK_MOVE,f.player());
        h.assertValueEqual(count(f.player(),item("tube_restrict")),2,"Shift-click must stop when the next craft lacks vis");
        h.assertValueEqual(f.table().getItem(0).getCount(),2,"Unaffordable repeat consumed its ingredient");
        h.assertValueEqual(f.table().getItem(10).getCount(),2,"Unaffordable repeat consumed its crystal");
        h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),5.0f,"Unaffordable repeat consumed the remaining aura");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void crystalCostsRequireTheOriginalCompleteTagMatch(GameTestHelper h) {
        var f=fixture(h,20);
        ResearchStorage.get(h.getLevel().getServer()).player(f.player().getUUID()).flag("TUBES");
        f.table().setItem(0,new ItemStack(item("tube")));
        var named=CrystalItem.stack(Aspect.EARTH);named.set(DataComponents.CUSTOM_NAME,Component.literal("Renamed crystal"));
        f.table().setItem(10,named);
        f.menu().clicked(0,0,ClickType.PICKUP,f.player());
        h.assertTrue(f.menu().getCarried().isEmpty(),"Arcane crystal payment ignored the original strict tag comparison");
        h.assertTrue(f.table().getItem(0).is(item("tube")),"Rejected crystal payment consumed the catalyst");
        h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),20.0f,"Rejected crystal payment consumed vis");
        crystals(f,Aspect.EARTH,1);
        f.menu().clicked(0,0,ClickType.PICKUP,f.player());
        h.assertTrue(f.menu().getCarried().is(item("tube_restrict")),"Canonical crystal must satisfy the same recipe");
        h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),10.0f,"Canonical crystal craft charged incorrect vis");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void voidJarUpgradePreservesTheWholeJarComponentPatch(GameTestHelper h) {
        var f=fixture(h,100);
        ResearchStorage.get(h.getLevel().getServer()).player(f.player().getUUID()).flag("WARDEDJARS");
        var jar=new ItemStack(item("jar_normal"));
        jar.set(DataComponents.CUSTOM_NAME,Component.literal("Named essentia reserve"));
        var blockData=new CompoundTag();blockData.putString("id","thaumcraft:jar");blockData.putString("aspect","aer");blockData.putInt("amount",17);
        jar.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(blockData));
        var extra=new CompoundTag();extra.putString("owner_note","keep this too");
        jar.set(DataComponents.CUSTOM_DATA,CustomData.of(extra));
        var expected=jar.transmuteCopy(item("jar_void"),1);
        f.table().setItem(8,jar);crystals(f,Aspect.ENTROPY,1);
        f.menu().broadcastChanges();
        h.assertTrue(ItemStack.matches(f.menu().getSlot(0).getItem(),expected),"Void-jar preview lost source components");
        f.menu().clicked(0,0,ClickType.PICKUP,f.player());
        h.assertTrue(ItemStack.matches(f.menu().getCarried(),expected),"Void-jar transaction lost its name, contents, or custom data");
        h.assertTrue(f.table().getItem(8).isEmpty(),"Offset jar input was not consumed");
        h.assertTrue(f.table().getItem(14).isEmpty(),"Void-jar upgrade did not consume entropy crystal");
        h.assertValueEqual(AuraSystem.get(h.getLevel(),f.pos()).vis(),50.0f,"Void-jar upgrade charged incorrect vis");
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void ordinaryCakeCraftReturnsBucketsAndFiresOneEvent(GameTestHelper h) {
        var f=fixture(h,0);
        for(int i=0;i<3;i++)f.table().setItem(i,new ItemStack(Items.MILK_BUCKET));
        f.table().setItem(3,new ItemStack(Items.SUGAR));f.table().setItem(4,new ItemStack(Items.EGG));f.table().setItem(5,new ItemStack(Items.SUGAR));
        for(int i=6;i<9;i++)f.table().setItem(i,new ItemStack(Items.WHEAT));
        var observer=new CraftedObserver(f.player());NeoForge.EVENT_BUS.register(observer);
        try {
            f.menu().clicked(0,0,ClickType.PICKUP,f.player());
            h.assertTrue(f.menu().getCarried().is(Items.CAKE),"Ordinary recipes must work on the arcane workbench without vis or research");
            for(int i=0;i<3;i++)h.assertTrue(f.table().getItem(i).is(Items.BUCKET)&&f.table().getItem(i).getCount()==1,"Milk bucket remainder was lost: "+i);
            for(int i=3;i<9;i++)h.assertTrue(f.table().getItem(i).isEmpty(),"Cake ingredient was not consumed: "+i);
            h.assertValueEqual(observer.count,1,"Ordinary workbench crafting must fire exactly one event");
            h.assertTrue(observer.output.is(Items.CAKE),"Crafting event reported the wrong output");
        } finally {NeoForge.EVENT_BUS.unregister(observer);}
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60)
    public static void shiftOutputKeepsItsReservedSpaceWhenBottlesReturn(GameTestHelper h) {
        var f=fixture(h,0);
        for(int i:List.of(4,5,7,8))f.table().setItem(i,new ItemStack(Items.HONEY_BOTTLE,2));
        for(int i=0;i<f.player().getInventory().items.size();i++)f.player().getInventory().setItem(i,new ItemStack(Items.STONE,64));
        f.player().getInventory().setItem(0,ItemStack.EMPTY);
        f.menu().clicked(0,0,ClickType.QUICK_MOVE,f.player());
        h.assertValueEqual(count(f.player(),Items.HONEY_BLOCK),2,"Returning bottles occupied the reserved output slot and lost crafted blocks");
        for(int i:List.of(4,5,7,8))h.assertTrue(f.table().getItem(i).is(Items.GLASS_BOTTLE)&&f.table().getItem(i).getCount()==1,"Offset remainder went into the wrong slot: "+i);
        int dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.pos()).inflate(4)).stream().filter(e->e.getItem().is(Items.GLASS_BOTTLE)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertValueEqual(dropped,4,"First-craft bottles must be dropped when the ingredient slots and inventory are full");
        h.succeed();
    }

    public static final class CraftedObserver {
        private final ServerPlayer player;
        int count,lastCatalystCount;
        ItemStack output=ItemStack.EMPTY;
        CraftedObserver(ServerPlayer player){this.player=player;}
        @SubscribeEvent public void crafted(PlayerEvent.ItemCraftedEvent event){
            if(event.getEntity()!=player)return;
            count++;output=event.getCrafting().copy();
            for(int i=0;i<event.getInventory().getContainerSize();i++)if(event.getInventory().getItem(i).is(item("tube")))lastCatalystCount=event.getInventory().getItem(i).getCount();
        }
    }
    private record Fixture(ServerPlayer player,WorkbenchEntity table,WorkbenchMenu menu,BlockPos pos){}
    private static Fixture fixture(GameTestHelper h,int vis){
        var relative=new BlockPos(1,2,1);h.setBlock(relative,MachineContent.WORKBENCH.get());
        var table=(WorkbenchEntity)h.getBlockEntity(relative);var player=GameTestPlayers.create(h);var pos=h.absolutePos(relative);
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
        var aura=AuraSystem.get(h.getLevel(),pos);aura.drain(false,Float.MAX_VALUE,false);aura.addVis(vis);
        return new Fixture(player,table,new WorkbenchMenu(1,player.getInventory(),table,pos),pos);
    }
    private static void crystals(Fixture f,Aspect aspect,int count){f.table().setItem(9+WorkbenchMenu.PRIMALS.indexOf(aspect),CrystalItem.stack(aspect).copyWithCount(count));}
    private static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));}
    private static int count(ServerPlayer player,Item item){return player.getInventory().items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private WorkbenchCraftingGameTests(){}
}
