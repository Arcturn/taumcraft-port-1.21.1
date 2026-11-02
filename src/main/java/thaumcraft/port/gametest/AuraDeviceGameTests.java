package thaumcraft.port.gametest;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.device.VisBatteryBlock;
import thaumcraft.port.auromancy.device.WorkbenchAura;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.machine.WorkbenchEntity;
import thaumcraft.port.machine.WorkbenchMenu;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraState;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AuraDeviceGameTests {
    @GameTest(template="empty",timeoutTicks=60)
    public static void batteryUsesStrictThresholdsAndRedstoneOverridesCharging(GameTestHelper h){
        var level=h.getLevel();var relative=new BlockPos(1,2,1);var pos=h.absolutePos(relative);
        var battery=(VisBatteryBlock)block("vis_battery");h.setBlock(relative,battery);
        var old=level.getChunkAt(pos).getData(AuraSystem.AURA);
        try {
            level.getChunkAt(pos).setData(AuraSystem.AURA,new AuraState(100,90,0));
            battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            h.assertValueEqual(level.getBlockState(pos).getValue(VisBatteryBlock.CHARGE),0,"Exactly 90% must not charge");
            AuraSystem.add(level,pos,1,false);battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            h.assertValueEqual(level.getBlockState(pos).getValue(VisBatteryBlock.CHARGE),1,"Surplus aura must charge one point");
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),90f,"Battery did not pay exactly one vis");
            level.getChunkAt(pos).setData(AuraSystem.AURA,new AuraState(100,75,0));
            battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            h.assertValueEqual(level.getBlockState(pos).getValue(VisBatteryBlock.CHARGE),1,"Exactly 75% must not discharge");
            AuraSystem.drain(level,pos,1,false,false);battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            h.assertValueEqual(level.getBlockState(pos).getValue(VisBatteryBlock.CHARGE),0,"Low aura must recover stored charge");
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),75f,"Discharge did not return one vis");
            level.setBlockAndUpdate(pos,battery.defaultBlockState().setValue(VisBatteryBlock.CHARGE,10));
            level.getChunkAt(pos).setData(AuraSystem.AURA,new AuraState(100,100,0));
            battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),100f,"A full battery consumed vis");
            h.setBlock(relative.east(),Blocks.REDSTONE_BLOCK);
            h.assertTrue(level.getBlockTicks().hasScheduledTick(pos,battery),"Power must schedule a battery update");
            battery.updateCharge(level.getBlockState(pos),level,pos,level.random);
            var state=level.getBlockState(pos);
            h.assertValueEqual(state.getValue(VisBatteryBlock.CHARGE),9,"Redstone must discharge even in abundant aura");
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),101f,"Powered battery lost stored vis");
            h.assertValueEqual(state.getAnalogOutputSignal(level,pos),9,"Comparator must report charge without rescaling");
            h.assertValueEqual(state.getLightEmission(level,pos),9,"Emitted light must equal stored charge");
        } finally {level.getChunkAt(pos).setData(AuraSystem.AURA,old);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void chargerLetsAnActualCraftUseNeighbouringAuraExactlyOnce(GameTestHelper h){
        var relative=new BlockPos(1,2,1);h.setBlock(relative,MachineContent.WORKBENCH.get());var pos=h.absolutePos(relative);var level=h.getLevel();
        var saved=new LinkedHashMap<BlockPos,AuraState>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var chunkPos=pos.offset(x*16,0,z*16);var chunk=level.getChunkAt(chunkPos);saved.put(chunkPos,chunk.getData(AuraSystem.AURA));chunk.setData(AuraSystem.AURA,new AuraState(100,0,0));
        }
        try {
            AuraSystem.add(level,pos.offset(16,0,0),100,false);
            var player=GameTestPlayers.create(h);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
            ResearchStorage.get(level.getServer()).player(player.getUUID()).flag("TUBES");
            var table=(WorkbenchEntity)h.getBlockEntity(relative);var menu=new WorkbenchMenu(1,player.getInventory(),table,pos);
            table.setItem(8,new ItemStack(block("tube")));table.setItem(10,CrystalItem.stack(Aspect.EARTH));
            menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().isEmpty(),"Workbench without charger spent another chunk's aura");
            var charger=block("arcane_workbench_charger");h.setBlock(relative.above(),charger);
            h.assertTrue(level.getBlockState(pos.above()).canSurvive(level,pos.above()),"Charger rejected the arcane workbench");
            h.assertValueEqual(WorkbenchAura.available(level,pos),100,"Charger did not reveal neighbouring aura");
            menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().is(block("tube_restrict").asItem()),"Charger failed to unlock the paid workbench transaction");
            h.assertTrue(table.getItem(8).isEmpty()&&table.getItem(10).isEmpty(),"Craft failed to consume its catalyst and crystal");
            h.assertValueEqual(AuraSystem.get(level,pos.offset(16,0,0)).vis(),90f,"Charger did not charge exactly ten vis");
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),0f,"Charger manufactured local aura");
            h.setBlock(relative,Blocks.AIR);
            h.assertTrue(level.getBlockState(pos.above()).isAir(),"Charger did not drop when its station disappeared");
        } finally {saved.forEach((p,a)->level.getChunkAt(p).setData(AuraSystem.AURA,a));}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void focalDrainDoesNotRedistributeMissingChunkShares(GameTestHelper h){
        var relative=new BlockPos(1,2,1);h.setBlock(relative,MachineContent.WORKBENCH.get());h.setBlock(relative.above(),block("arcane_workbench_charger"));
        var level=h.getLevel();var pos=h.absolutePos(relative);var saved=new LinkedHashMap<BlockPos,AuraState>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var p=pos.offset(x*16,0,z*16);var chunk=level.getChunkAt(p);saved.put(p,chunk.getData(AuraSystem.AURA));chunk.setData(AuraSystem.AURA,new AuraState(100,x==0&&z==0?100:0,0));
        }
        try {
            h.assertValueEqual(WorkbenchAura.drainFocal(level,pos,18),2f,"Focal charger must take one ninth per chunk in a single pass");
            h.assertValueEqual(AuraSystem.get(level,pos).vis(),98f,"Focal charger redistributed empty neighbours' shares into its own chunk");
            for(var p:saved.keySet())level.getChunkAt(p).setData(AuraSystem.AURA,new AuraState(100,1.9f,0));
            h.assertValueEqual(WorkbenchAura.available(level,pos),9,"Workbench aura display must truncate each chunk's fractional vis separately");
        } finally {saved.forEach((p,a)->level.getChunkAt(p).setData(AuraSystem.AURA,a));}
        h.succeed();
    }
    private static Block block(String path){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:"+path));}
    private AuraDeviceGameTests(){}
}
