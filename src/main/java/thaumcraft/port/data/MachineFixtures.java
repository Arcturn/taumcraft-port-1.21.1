package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.*;
import thaumcraft.port.machine.*;
import thaumcraft.port.research.ResearchCatalog;

/** Real registered objects and codecs, with no server, world, or mocked Minecraft classes. */
public final class MachineFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception{
        int checks=0;
        var silver=thaumcraft.port.research.LegacyRequirement.parse("thaumcraft:nitor_silver;3;0");
        var lightGray=MachineContent.NITOR.get("nitor_light_gray").get().asItem();
        require(silver.craftKey().equals("thaumcraft:nitor_light_gray")&&silver.count()==3&&silver.matches().test(new ItemStack(lightGray)),"Legacy silver nitor resolves to the registered light-gray item");checks++;
        require(!silver.matches().test(new ItemStack(MachineContent.NITOR.get("nitor_gray").get())),"Legacy silver nitor must not accept dark gray");checks++;
        var silverIcon=thaumcraft.port.client.BookAssets.stack("thaumcraft:nitor_silver;3;0");
        require(silverIcon.is(lightGray)&&silverIcon.getCount()==3,"Book uses real nitor item with its colour handler, not empty legacy block geometry");checks++;
        JsonElement json;try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:recipe/thaumometer.json")).openAsReader()){json=JsonParser.parseReader(in);}
        var recipe=(ArcaneRecipe)Recipe.CODEC.parse(ops,json).getOrThrow();
        require(recipe.vis()==20&&recipe.crystals().size()==6&&recipe.crystals().values().stream().allMatch(n->n==1),"Thaumometer costs");checks++;
        var grid=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
        for(int i:new int[]{1,3,5,7})grid.set(i,new ItemStack(Items.GOLD_INGOT));grid.set(4,new ItemStack(Items.GLASS_PANE));
        require(recipe.matches(CraftingInput.of(3,3,grid),null),"Original thaumometer pattern");checks++;
        grid.set(1,ItemStack.EMPTY);require(!recipe.matches(CraftingInput.of(3,3,grid),null),"Incomplete pattern rejected");checks++;
        var bench=new WorkbenchEntity(BlockPos.ZERO,MachineContent.WORKBENCH.get().defaultBlockState());
        bench.setItem(0,new ItemStack(Items.GOLD_INGOT,7));bench.setItem(9,CrystalItem.stack(Aspect.AIR));
        CompoundTag saved=bench.saveCustomOnly(registries);
        var restored=new WorkbenchEntity(BlockPos.ZERO,MachineContent.WORKBENCH.get().defaultBlockState());restored.loadCustomOnly(saved,registries);
        require(restored.getItem(0).is(Items.GOLD_INGOT)&&restored.getItem(0).getCount()==7&&CrystalItem.aspect(restored.getItem(9))==Aspect.AIR,"Workbench inventory/component save");checks++;
        // Existing alpha saves use a different crystal order from TC6 ShardType.
        for(int i=0;i<6;i++)bench.setItem(9+i,CrystalItem.stack(WorkbenchMenu.PRIMALS.get(i)));
        restored.loadCustomOnly(bench.saveCustomOnly(registries),registries);
        var menu=new WorkbenchMenu(1,new net.minecraft.world.entity.player.Inventory(null),restored,BlockPos.ZERO);
        require(menu.slots.size()==52&&menu.getSlot(0).x==160&&menu.getSlot(0).y==64&&!menu.getSlot(0).mayPickup(null),"Original result position remains server protected");checks++;
        int[][] crystalPositions={{64,13},{17,93},{17,35},{112,35},{112,93},{64,115}};
        for(int i=0;i<6;i++){
            var slot=menu.getSlot(10+i);var aspect=WorkbenchMenu.PRIMALS.get(i);
            require(slot.x==crystalPositions[i][0]&&slot.y==crystalPositions[i][1]&&CrystalItem.aspect(slot.getItem())==aspect,"Original crystal position preserves existing save slot: "+aspect);checks++;
            require(slot.mayPlace(CrystalItem.stack(aspect))&&!slot.mayPlace(CrystalItem.stack(WorkbenchMenu.PRIMALS.get((i+1)%6))),"Crystal filters follow the moved slots");checks++;
            menu.setData(2+i,i+1);require(menu.crystalCost(i)==i+1,"Synchronized cost for crystal halo");checks++;
        }
        for(int i=0;i<9;i++){var slot=menu.getSlot(1+i);require(slot.x==40+(i%3)*24&&slot.y==40+(i/3)*24,"Original spaced crafting grid");checks++;}
        require(menu.getSlot(16).x==16&&menu.getSlot(16).y==151&&menu.getSlot(43).y==209,"Original workbench player inventory positions");checks++;
        var furnace=new thaumcraft.port.essentia.SmelterMenu(2,new net.minecraft.world.entity.player.Inventory(null));
        require(furnace.slots.size()==38&&furnace.getSlot(0).x==80&&furnace.getSlot(0).y==8&&furnace.getSlot(1).x==80&&furnace.getSlot(1).y==48&&furnace.getSlot(2).y==84&&furnace.getSlot(29).y==142,"Original smelter slot layout");checks++;
        var crucible=new CrucibleEntity(BlockPos.ZERO,MachineContent.CRUCIBLE.get().defaultBlockState());
        crucible.state.restore(750,180,-150,Map.of(Aspect.FLUX,3,Aspect.FIRE,10));
        var copy=new CrucibleEntity(BlockPos.ZERO,MachineContent.CRUCIBLE.get().defaultBlockState());copy.loadCustomOnly(crucible.saveCustomOnly(registries),registries);
        require(copy.state.water()==750&&copy.state.heat()==180&&copy.state.total()==13&&copy.state.timer()==-150,"Crucible save");checks++;
        require(BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.parse("thaumcraft:scan"))&&BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.parse("thaumcraft:runicshieldcharge")),"Original sound and corrected alias registered");checks++;
        for(var entry:ResearchCatalog.read("/thaumcraft-port/vanilla-aspect-tags.json").entrySet()){
            var key=entry.getKey();var id=ResourceLocation.parse(key.substring(key.indexOf(':')+1));
            require(key.startsWith("item:")?BuiltInRegistries.ITEM.containsKey(id):BuiltInRegistries.BLOCK.containsKey(id),"Flattened aspect subject exists: "+key);checks++;
        }
        var dust=new SalisMundusRecipe(CraftingBookCategory.MISC);
        var dustInput=CraftingInput.of(3,2,List.of(CrystalItem.stack(Aspect.AIR),CrystalItem.stack(Aspect.FIRE),CrystalItem.stack(Aspect.WATER),new ItemStack(Items.BOWL),new ItemStack(Items.FLINT),new ItemStack(Items.REDSTONE)));
        require(dust.matches(dustInput,null),"Salis Mundus matches distinct crystal aspects");checks++;
        var remainders=dust.getRemainingItems(dustInput);
        require(remainders.get(3).is(Items.BOWL)&&remainders.get(4).is(Items.FLINT)&&remainders.stream().filter(s->!s.isEmpty()).count()==2,"Reusable flint and bowl retained");checks++;
        return checks;
    }
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    private MachineFixtures(){}
}
