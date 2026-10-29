/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockRailPowered
 *  net.minecraft.block.BlockSlab
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.Material
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.EnumRarity
 *  net.minecraft.item.ItemBlock
 *  net.minecraft.item.ItemSlab
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.fluids.Fluid
 *  net.minecraftforge.fluids.FluidRegistry
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 *  net.minecraftforge.fml.common.registry.GameRegistry
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package thaumcraft.common.config;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.blocks.basic.BlockBannerTC;
import thaumcraft.common.blocks.basic.BlockBannerTCItem;
import thaumcraft.common.blocks.basic.BlockCandle;
import thaumcraft.common.blocks.basic.BlockMetalTC;
import thaumcraft.common.blocks.basic.BlockPavingStone;
import thaumcraft.common.blocks.basic.BlockPillar;
import thaumcraft.common.blocks.basic.BlockPlanksTC;
import thaumcraft.common.blocks.basic.BlockSlabTC;
import thaumcraft.common.blocks.basic.BlockStairsTC;
import thaumcraft.common.blocks.basic.BlockStonePorous;
import thaumcraft.common.blocks.basic.BlockStoneTC;
import thaumcraft.common.blocks.basic.BlockTable;
import thaumcraft.common.blocks.basic.BlockTranslucent;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbench;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbenchCharger;
import thaumcraft.common.blocks.crafting.BlockCrucible;
import thaumcraft.common.blocks.crafting.BlockFocalManipulator;
import thaumcraft.common.blocks.crafting.BlockGolemBuilder;
import thaumcraft.common.blocks.crafting.BlockInfusionMatrix;
import thaumcraft.common.blocks.crafting.BlockPatternCrafter;
import thaumcraft.common.blocks.crafting.BlockResearchTable;
import thaumcraft.common.blocks.crafting.BlockThaumatorium;
import thaumcraft.common.blocks.crafting.BlockVoidSiphon;
import thaumcraft.common.blocks.devices.BlockArcaneEar;
import thaumcraft.common.blocks.devices.BlockArcaneEarToggle;
import thaumcraft.common.blocks.devices.BlockBellows;
import thaumcraft.common.blocks.devices.BlockBrainBox;
import thaumcraft.common.blocks.devices.BlockCondenser;
import thaumcraft.common.blocks.devices.BlockCondenserLattice;
import thaumcraft.common.blocks.devices.BlockDioptra;
import thaumcraft.common.blocks.devices.BlockHungryChest;
import thaumcraft.common.blocks.devices.BlockInfernalFurnace;
import thaumcraft.common.blocks.devices.BlockInlay;
import thaumcraft.common.blocks.devices.BlockLamp;
import thaumcraft.common.blocks.devices.BlockLevitator;
import thaumcraft.common.blocks.devices.BlockMirror;
import thaumcraft.common.blocks.devices.BlockMirrorItem;
import thaumcraft.common.blocks.devices.BlockPedestal;
import thaumcraft.common.blocks.devices.BlockPotionSprayer;
import thaumcraft.common.blocks.devices.BlockRechargePedestal;
import thaumcraft.common.blocks.devices.BlockRedstoneRelay;
import thaumcraft.common.blocks.devices.BlockSpa;
import thaumcraft.common.blocks.devices.BlockStabilizer;
import thaumcraft.common.blocks.devices.BlockVisBattery;
import thaumcraft.common.blocks.devices.BlockVisGenerator;
import thaumcraft.common.blocks.devices.BlockWaterJug;
import thaumcraft.common.blocks.essentia.BlockAlembic;
import thaumcraft.common.blocks.essentia.BlockCentrifuge;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;
import thaumcraft.common.blocks.essentia.BlockJar;
import thaumcraft.common.blocks.essentia.BlockJarBrainItem;
import thaumcraft.common.blocks.essentia.BlockJarItem;
import thaumcraft.common.blocks.essentia.BlockSmelter;
import thaumcraft.common.blocks.essentia.BlockSmelterAux;
import thaumcraft.common.blocks.essentia.BlockSmelterVent;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.blocks.misc.BlockBarrier;
import thaumcraft.common.blocks.misc.BlockEffect;
import thaumcraft.common.blocks.misc.BlockFlesh;
import thaumcraft.common.blocks.misc.BlockFluidDeath;
import thaumcraft.common.blocks.misc.BlockFluidPure;
import thaumcraft.common.blocks.misc.BlockHole;
import thaumcraft.common.blocks.misc.BlockNitor;
import thaumcraft.common.blocks.misc.BlockPlaceholder;
import thaumcraft.common.blocks.world.BlockGrassAmbient;
import thaumcraft.common.blocks.world.BlockLoot;
import thaumcraft.common.blocks.world.ore.BlockCrystal;
import thaumcraft.common.blocks.world.ore.BlockOreTC;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.blocks.world.plants.BlockLeavesTC;
import thaumcraft.common.blocks.world.plants.BlockLogsTC;
import thaumcraft.common.blocks.world.plants.BlockPlantCinderpearl;
import thaumcraft.common.blocks.world.plants.BlockPlantShimmerleaf;
import thaumcraft.common.blocks.world.plants.BlockPlantVishroom;
import thaumcraft.common.blocks.world.plants.BlockSaplingTC;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;
import thaumcraft.common.blocks.world.taint.BlockTaint;
import thaumcraft.common.blocks.world.taint.BlockTaintFeature;
import thaumcraft.common.blocks.world.taint.BlockTaintFibre;
import thaumcraft.common.blocks.world.taint.BlockTaintLog;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.crafting.TilePatternCrafter;
import thaumcraft.common.tiles.crafting.TilePedestal;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TileThaumatoriumTop;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;
import thaumcraft.common.tiles.devices.TileArcaneEar;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.common.tiles.devices.TileCondenser;
import thaumcraft.common.tiles.devices.TileDioptra;
import thaumcraft.common.tiles.devices.TileHungryChest;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;
import thaumcraft.common.tiles.devices.TileJarBrain;
import thaumcraft.common.tiles.devices.TileLampArcane;
import thaumcraft.common.tiles.devices.TileLampFertility;
import thaumcraft.common.tiles.devices.TileLampGrowth;
import thaumcraft.common.tiles.devices.TileLevitator;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.common.tiles.devices.TilePotionSprayer;
import thaumcraft.common.tiles.devices.TileRechargePedestal;
import thaumcraft.common.tiles.devices.TileRedstoneRelay;
import thaumcraft.common.tiles.devices.TileSpa;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.common.tiles.devices.TileVisGenerator;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.common.tiles.essentia.TileAlembic;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileEssentiaInput;
import thaumcraft.common.tiles.essentia.TileEssentiaOutput;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.tiles.essentia.TileSmelter;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileTubeFilter;
import thaumcraft.common.tiles.essentia.TileTubeOneway;
import thaumcraft.common.tiles.essentia.TileTubeRestrict;
import thaumcraft.common.tiles.essentia.TileTubeValve;
import thaumcraft.common.tiles.misc.TileBanner;
import thaumcraft.common.tiles.misc.TileBarrierStone;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.common.tiles.misc.TileNitor;

public class ConfigBlocks {
    public static void initMisc() {
        BlocksTC.oreAmber.setHarvestLevel("pickaxe", 1);
        BlocksTC.oreCinnabar.setHarvestLevel("pickaxe", 2);
        BlockUtils.portableHoleBlackList.add("minecraft:bed");
        BlockUtils.portableHoleBlackList.add("minecraft:piston");
        BlockUtils.portableHoleBlackList.add("minecraft:piston_head");
        BlockUtils.portableHoleBlackList.add("minecraft:sticky_piston");
        BlockUtils.portableHoleBlackList.add("minecraft:piston_extension");
        BlockUtils.portableHoleBlackList.add("minecraft:wooden_door");
        BlockUtils.portableHoleBlackList.add("minecraft:spruce_door");
        BlockUtils.portableHoleBlackList.add("minecraft:birch_door");
        BlockUtils.portableHoleBlackList.add("minecraft:jungle_door");
        BlockUtils.portableHoleBlackList.add("minecraft:acacia_door");
        BlockUtils.portableHoleBlackList.add("minecraft:dark_oak_door");
        BlockUtils.portableHoleBlackList.add("minecraft:iron_door");
        BlockUtils.portableHoleBlackList.add("thaumcraft:infernal_furnace");
    }

    public static void initBlocks(IForgeRegistry<Block> iForgeRegistry) {
        BlocksTC.oreAmber = ConfigBlocks.registerBlock(new BlockOreTC("ore_amber").func_149711_c(1.5f));
        BlocksTC.oreCinnabar = ConfigBlocks.registerBlock(new BlockOreTC("ore_cinnabar").func_149711_c(2.0f));
        BlocksTC.oreQuartz = ConfigBlocks.registerBlock(new BlockOreTC("ore_quartz").func_149711_c(3.0f));
        BlocksTC.crystalAir = ConfigBlocks.registerBlock(new BlockCrystal("crystal_aer", Aspect.AIR));
        BlocksTC.crystalFire = ConfigBlocks.registerBlock(new BlockCrystal("crystal_ignis", Aspect.FIRE));
        BlocksTC.crystalWater = ConfigBlocks.registerBlock(new BlockCrystal("crystal_aqua", Aspect.WATER));
        BlocksTC.crystalEarth = ConfigBlocks.registerBlock(new BlockCrystal("crystal_terra", Aspect.EARTH));
        BlocksTC.crystalOrder = ConfigBlocks.registerBlock(new BlockCrystal("crystal_ordo", Aspect.ORDER));
        BlocksTC.crystalEntropy = ConfigBlocks.registerBlock(new BlockCrystal("crystal_perditio", Aspect.ENTROPY));
        BlocksTC.crystalTaint = ConfigBlocks.registerBlock(new BlockCrystal("crystal_vitium", Aspect.FLUX));
        ShardType.AIR.setOre(BlocksTC.crystalAir);
        ShardType.FIRE.setOre(BlocksTC.crystalFire);
        ShardType.WATER.setOre(BlocksTC.crystalWater);
        ShardType.EARTH.setOre(BlocksTC.crystalEarth);
        ShardType.ORDER.setOre(BlocksTC.crystalOrder);
        ShardType.ENTROPY.setOre(BlocksTC.crystalEntropy);
        ShardType.FLUX.setOre(BlocksTC.crystalTaint);
        BlocksTC.stoneArcane = ConfigBlocks.registerBlock(new BlockStoneTC("stone_arcane", true));
        BlocksTC.stoneArcaneBrick = ConfigBlocks.registerBlock(new BlockStoneTC("stone_arcane_brick", true));
        BlocksTC.stoneAncient = ConfigBlocks.registerBlock(new BlockStoneTC("stone_ancient", true));
        BlocksTC.stoneAncientTile = ConfigBlocks.registerBlock(new BlockStoneTC("stone_ancient_tile", false));
        BlocksTC.stoneAncientRock = ConfigBlocks.registerBlock((BlockTC)new BlockStoneTC("stone_ancient_rock", false).func_149711_c(-1.0f));
        BlocksTC.stoneAncientGlyphed = ConfigBlocks.registerBlock(new BlockStoneTC("stone_ancient_glyphed", false));
        BlocksTC.stoneAncientDoorway = ConfigBlocks.registerBlock((BlockTC)new BlockStoneTC("stone_ancient_doorway", false).func_149711_c(-1.0f));
        BlocksTC.stoneEldritchTile = ConfigBlocks.registerBlock((BlockTC)new BlockStoneTC("stone_eldritch_tile", true).func_149711_c(15.0f).func_149752_b(1000.0f));
        BlocksTC.stonePorous = ConfigBlocks.registerBlock(new BlockStonePorous());
        BlocksTC.stairsArcane = ConfigBlocks.registerBlock((Block)new BlockStairsTC("stairs_arcane", BlocksTC.stoneArcane.func_176223_P()));
        BlocksTC.stairsArcaneBrick = ConfigBlocks.registerBlock((Block)new BlockStairsTC("stairs_arcane_brick", BlocksTC.stoneArcaneBrick.func_176223_P()));
        BlocksTC.stairsAncient = ConfigBlocks.registerBlock((Block)new BlockStairsTC("stairs_ancient", BlocksTC.stoneAncient.func_176223_P()));
        BlocksTC.slabArcaneStone = (BlockSlab)new BlockSlabTC.Half("slab_arcane_stone", null, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.doubleSlabArcaneStone = (BlockSlab)new BlockSlabTC.Double("slab_double_arcane_stone", (Block)BlocksTC.slabArcaneStone, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.slabArcaneBrick = (BlockSlab)new BlockSlabTC.Half("slab_arcane_brick", null, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.doubleSlabArcaneBrick = (BlockSlab)new BlockSlabTC.Double("slab_double_arcane_brick", (Block)BlocksTC.slabArcaneBrick, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.slabAncient = (BlockSlab)new BlockSlabTC.Half("slab_ancient", null, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.doubleSlabAncient = (BlockSlab)new BlockSlabTC.Double("slab_double_ancient", (Block)BlocksTC.slabAncient, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.slabEldritch = (BlockSlab)new BlockSlabTC.Half("slab_eldritch", null, false).func_149711_c(2.0f).func_149752_b(10.0f);
        BlocksTC.doubleSlabEldritch = (BlockSlab)new BlockSlabTC.Double("slab_double_eldritch", (Block)BlocksTC.slabEldritch, false).func_149711_c(2.0f).func_149752_b(10.0f);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabArcaneStone);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabArcaneStone);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabArcaneBrick);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabArcaneBrick);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabAncient);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabAncient);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabEldritch);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabEldritch);
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabArcaneStone, BlocksTC.slabArcaneStone, BlocksTC.doubleSlabArcaneStone).setRegistryName(BlocksTC.slabArcaneStone.getRegistryName()));
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabArcaneBrick, BlocksTC.slabArcaneBrick, BlocksTC.doubleSlabArcaneBrick).setRegistryName(BlocksTC.slabArcaneBrick.getRegistryName()));
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabAncient, BlocksTC.slabAncient, BlocksTC.doubleSlabAncient).setRegistryName(BlocksTC.slabAncient.getRegistryName()));
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabEldritch, BlocksTC.slabEldritch, BlocksTC.doubleSlabEldritch).setRegistryName(BlocksTC.slabEldritch.getRegistryName()));
        BlocksTC.saplingGreatwood = ConfigBlocks.registerBlock((Block)new BlockSaplingTC("sapling_greatwood"));
        BlocksTC.saplingSilverwood = ConfigBlocks.registerBlock((Block)new BlockSaplingTC("sapling_silverwood"));
        BlocksTC.logGreatwood = ConfigBlocks.registerBlock(new BlockLogsTC("log_greatwood"));
        BlocksTC.logSilverwood = ConfigBlocks.registerBlock(new BlockLogsTC("log_silverwood"));
        BlocksTC.leafGreatwood = ConfigBlocks.registerBlock((Block)new BlockLeavesTC("leaves_greatwood"));
        BlocksTC.leafSilverwood = ConfigBlocks.registerBlock((Block)new BlockLeavesTC("leaves_silverwood"));
        BlocksTC.shimmerleaf = ConfigBlocks.registerBlock((Block)new BlockPlantShimmerleaf());
        BlocksTC.cinderpearl = ConfigBlocks.registerBlock((Block)new BlockPlantCinderpearl());
        BlocksTC.vishroom = ConfigBlocks.registerBlock((Block)new BlockPlantVishroom());
        BlocksTC.plankGreatwood = ConfigBlocks.registerBlock(new BlockPlanksTC("plank_greatwood"));
        BlocksTC.plankSilverwood = ConfigBlocks.registerBlock(new BlockPlanksTC("plank_silverwood"));
        BlocksTC.stairsGreatwood = ConfigBlocks.registerBlock((Block)new BlockStairsTC("stairs_greatwood", BlocksTC.plankGreatwood.func_176223_P()));
        BlocksTC.stairsSilverwood = ConfigBlocks.registerBlock((Block)new BlockStairsTC("stairs_silverwood", BlocksTC.plankSilverwood.func_176223_P()));
        BlocksTC.slabGreatwood = (BlockSlab)new BlockSlabTC.Half("slab_greatwood", null, true).func_149711_c(1.2f).func_149752_b(2.0f);
        BlocksTC.doubleSlabGreatwood = (BlockSlab)new BlockSlabTC.Double("slab_double_greatwood", (Block)BlocksTC.slabGreatwood, true).func_149711_c(1.2f).func_149752_b(2.0f);
        BlocksTC.slabSilverwood = (BlockSlab)new BlockSlabTC.Half("slab_silverwood", null, true).func_149711_c(1.0f).func_149752_b(2.0f);
        BlocksTC.doubleSlabSilverwood = (BlockSlab)new BlockSlabTC.Double("slab_double_silverwood", (Block)BlocksTC.slabSilverwood, true).func_149711_c(1.0f).func_149752_b(2.0f);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabGreatwood);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabGreatwood);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.slabSilverwood);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.doubleSlabSilverwood);
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabGreatwood, BlocksTC.slabGreatwood, BlocksTC.doubleSlabGreatwood).setRegistryName(BlocksTC.slabGreatwood.getRegistryName()));
        ForgeRegistries.ITEMS.register(new ItemSlab((Block)BlocksTC.slabSilverwood, BlocksTC.slabSilverwood, BlocksTC.doubleSlabSilverwood).setRegistryName(BlocksTC.slabSilverwood.getRegistryName()));
        BlocksTC.amberBlock = ConfigBlocks.registerBlock(new BlockTranslucent("amber_block"));
        BlocksTC.amberBrick = ConfigBlocks.registerBlock(new BlockTranslucent("amber_brick"));
        BlocksTC.fleshBlock = ConfigBlocks.registerBlock(new BlockFlesh());
        BlocksTC.lootCrateCommon = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151575_d, "loot_crate_common", BlockLoot.LootType.COMMON));
        BlocksTC.lootCrateUncommon = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151575_d, "loot_crate_uncommon", BlockLoot.LootType.UNCOMMON));
        BlocksTC.lootCrateRare = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151575_d, "loot_crate_rare", BlockLoot.LootType.RARE));
        BlocksTC.lootUrnCommon = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151576_e, "loot_urn_common", BlockLoot.LootType.COMMON));
        BlocksTC.lootUrnUncommon = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151576_e, "loot_urn_uncommon", BlockLoot.LootType.UNCOMMON));
        BlocksTC.lootUrnRare = ConfigBlocks.registerBlock(new BlockLoot(Material.field_151576_e, "loot_urn_rare", BlockLoot.LootType.RARE));
        BlocksTC.taintFibre = ConfigBlocks.registerBlock(new BlockTaintFibre());
        BlocksTC.taintCrust = ConfigBlocks.registerBlock(new BlockTaint("taint_crust"));
        BlocksTC.taintSoil = ConfigBlocks.registerBlock(new BlockTaint("taint_soil"));
        BlocksTC.taintRock = ConfigBlocks.registerBlock(new BlockTaint("taint_rock"));
        BlocksTC.taintGeyser = ConfigBlocks.registerBlock(new BlockTaint("taint_geyser"));
        BlocksTC.taintFeature = ConfigBlocks.registerBlock(new BlockTaintFeature());
        BlocksTC.taintLog = ConfigBlocks.registerBlock(new BlockTaintLog());
        BlocksTC.grassAmbient = ConfigBlocks.registerBlock((Block)new BlockGrassAmbient());
        BlocksTC.tableWood = ConfigBlocks.registerBlock(new BlockTable(Material.field_151575_d, "table_wood", SoundType.field_185848_a).func_149711_c(2.0f));
        BlocksTC.tableStone = ConfigBlocks.registerBlock(new BlockTable(Material.field_151576_e, "table_stone", SoundType.field_185851_d).func_149711_c(2.5f));
        BlocksTC.pedestalArcane = ConfigBlocks.registerBlock(new BlockPedestal("pedestal_arcane"));
        BlocksTC.pedestalAncient = ConfigBlocks.registerBlock(new BlockPedestal("pedestal_ancient"));
        BlocksTC.pedestalEldritch = ConfigBlocks.registerBlock(new BlockPedestal("pedestal_eldritch"));
        BlocksTC.metalBlockBrass = ConfigBlocks.registerBlock(new BlockMetalTC("metal_brass"));
        BlocksTC.metalBlockThaumium = ConfigBlocks.registerBlock(new BlockMetalTC("metal_thaumium"));
        BlocksTC.metalBlockVoid = ConfigBlocks.registerBlock(new BlockMetalTC("metal_void"));
        BlocksTC.metalAlchemical = ConfigBlocks.registerBlock(new BlockMetalTC("metal_alchemical"));
        BlocksTC.metalAlchemicalAdvanced = ConfigBlocks.registerBlock(new BlockMetalTC("metal_alchemical_advanced"));
        BlocksTC.pavingStoneTravel = ConfigBlocks.registerBlock(new BlockPavingStone("paving_stone_travel"));
        BlocksTC.pavingStoneBarrier = ConfigBlocks.registerBlock(new BlockPavingStone("paving_stone_barrier"));
        BlocksTC.pillarArcane = ConfigBlocks.registerBlock(new BlockPillar("pillar_arcane"));
        BlocksTC.pillarAncient = ConfigBlocks.registerBlock(new BlockPillar("pillar_ancient"));
        BlocksTC.pillarEldritch = ConfigBlocks.registerBlock(new BlockPillar("pillar_eldritch"));
        BlocksTC.matrixSpeed = ConfigBlocks.registerBlock(new BlockStoneTC("matrix_speed", false));
        BlocksTC.matrixCost = ConfigBlocks.registerBlock(new BlockStoneTC("matrix_cost", false));
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            BlocksTC.candles.put(dye, ConfigBlocks.registerBlock(new BlockCandle("candle_" + dye.func_176762_d().toLowerCase(), dye)));
        }
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            BlockBannerTC block = new BlockBannerTC("banner_" + dye.func_176762_d().toLowerCase(), dye);
            ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)block);
            ForgeRegistries.ITEMS.register(new BlockBannerTCItem(block).setRegistryName(block.getRegistryName()));
            BlocksTC.banners.put(dye, block);
        }
        BlocksTC.bannerCrimsonCult = new BlockBannerTC("banner_crimson_cult", null);
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)BlocksTC.bannerCrimsonCult);
        ForgeRegistries.ITEMS.register(new BlockBannerTCItem((BlockBannerTC)BlocksTC.bannerCrimsonCult).setRegistryName(BlocksTC.bannerCrimsonCult.getRegistryName()));
        for (EnumDyeColor dye : EnumDyeColor.values()) {
            BlocksTC.nitor.put(dye, ConfigBlocks.registerBlock(new BlockNitor("nitor_" + dye.func_176762_d().toLowerCase(), dye)));
        }
        BlocksTC.visBattery = ConfigBlocks.registerBlock(new BlockVisBattery());
        BlocksTC.inlay = ConfigBlocks.registerBlock(new BlockInlay());
        BlocksTC.arcaneWorkbench = ConfigBlocks.registerBlock(new BlockArcaneWorkbench());
        BlocksTC.arcaneWorkbenchCharger = ConfigBlocks.registerBlock(new BlockArcaneWorkbenchCharger());
        BlocksTC.dioptra = ConfigBlocks.registerBlock(new BlockDioptra());
        BlocksTC.researchTable = ConfigBlocks.registerBlock(new BlockResearchTable());
        BlocksTC.crucible = ConfigBlocks.registerBlock(new BlockCrucible());
        BlocksTC.arcaneEar = ConfigBlocks.registerBlock(new BlockArcaneEar("arcane_ear"));
        BlocksTC.arcaneEarToggle = ConfigBlocks.registerBlock(new BlockArcaneEarToggle());
        BlocksTC.lampArcane = ConfigBlocks.registerBlock(new BlockLamp(TileLampArcane.class, "lamp_arcane"));
        BlocksTC.lampFertility = ConfigBlocks.registerBlock(new BlockLamp(TileLampFertility.class, "lamp_fertility"));
        BlocksTC.lampGrowth = ConfigBlocks.registerBlock(new BlockLamp(TileLampGrowth.class, "lamp_growth"));
        BlocksTC.levitator = ConfigBlocks.registerBlock(new BlockLevitator());
        BlocksTC.centrifuge = ConfigBlocks.registerBlock(new BlockCentrifuge());
        BlocksTC.bellows = ConfigBlocks.registerBlock(new BlockBellows());
        BlocksTC.smelterBasic = ConfigBlocks.registerBlock(new BlockSmelter("smelter_basic"));
        BlocksTC.smelterThaumium = ConfigBlocks.registerBlock(new BlockSmelter("smelter_thaumium"));
        BlocksTC.smelterVoid = ConfigBlocks.registerBlock(new BlockSmelter("smelter_void"));
        BlocksTC.smelterAux = ConfigBlocks.registerBlock(new BlockSmelterAux());
        BlocksTC.smelterVent = ConfigBlocks.registerBlock(new BlockSmelterVent());
        BlocksTC.alembic = ConfigBlocks.registerBlock(new BlockAlembic());
        BlocksTC.rechargePedestal = ConfigBlocks.registerBlock(new BlockRechargePedestal());
        BlocksTC.wandWorkbench = ConfigBlocks.registerBlock(new BlockFocalManipulator());
        BlocksTC.hungryChest = ConfigBlocks.registerBlock((Block)new BlockHungryChest());
        BlocksTC.tube = ConfigBlocks.registerBlock(new BlockTube(TileTube.class, "tube"));
        BlocksTC.tubeValve = ConfigBlocks.registerBlock(new BlockTube(TileTubeValve.class, "tube_valve"));
        BlocksTC.tubeRestrict = ConfigBlocks.registerBlock(new BlockTube(TileTubeRestrict.class, "tube_restrict"));
        BlocksTC.tubeOneway = ConfigBlocks.registerBlock(new BlockTube(TileTubeOneway.class, "tube_oneway"));
        BlocksTC.tubeFilter = ConfigBlocks.registerBlock(new BlockTube(TileTubeFilter.class, "tube_filter"));
        BlocksTC.tubeBuffer = ConfigBlocks.registerBlock(new BlockTube(TileTubeBuffer.class, "tube_buffer"));
        BlocksTC.jarNormal = ConfigBlocks.registerBlock((Block)new BlockJar(TileJarFillable.class, "jar_normal"), BlockJarItem.class);
        BlocksTC.jarVoid = ConfigBlocks.registerBlock((Block)new BlockJar(TileJarFillableVoid.class, "jar_void"), BlockJarItem.class);
        BlocksTC.jarBrain = ConfigBlocks.registerBlock((Block)new BlockJar(TileJarBrain.class, "jar_brain"), BlockJarBrainItem.class);
        BlocksTC.infusionMatrix = ConfigBlocks.registerBlock(new BlockInfusionMatrix());
        BlocksTC.infernalFurnace = ConfigBlocks.registerBlock(new BlockInfernalFurnace());
        BlocksTC.everfullUrn = ConfigBlocks.registerBlock(new BlockWaterJug());
        BlocksTC.thaumatorium = ConfigBlocks.registerBlock(new BlockThaumatorium(false));
        BlocksTC.thaumatoriumTop = ConfigBlocks.registerBlock(new BlockThaumatorium(true));
        BlocksTC.brainBox = ConfigBlocks.registerBlock(new BlockBrainBox());
        BlocksTC.spa = ConfigBlocks.registerBlock(new BlockSpa());
        BlocksTC.golemBuilder = ConfigBlocks.registerBlock(new BlockGolemBuilder());
        BlocksTC.mirror = ConfigBlocks.registerBlock((Block)new BlockMirror(TileMirror.class, "mirror"), BlockMirrorItem.class);
        BlocksTC.mirrorEssentia = ConfigBlocks.registerBlock((Block)new BlockMirror(TileMirrorEssentia.class, "mirror_essentia"), BlockMirrorItem.class);
        BlocksTC.essentiaTransportInput = ConfigBlocks.registerBlock(new BlockEssentiaTransport(TileEssentiaInput.class, "essentia_input"));
        BlocksTC.essentiaTransportOutput = ConfigBlocks.registerBlock(new BlockEssentiaTransport(TileEssentiaOutput.class, "essentia_output"));
        BlocksTC.redstoneRelay = ConfigBlocks.registerBlock(new BlockRedstoneRelay());
        BlocksTC.patternCrafter = ConfigBlocks.registerBlock(new BlockPatternCrafter());
        BlocksTC.potionSprayer = ConfigBlocks.registerBlock(new BlockPotionSprayer());
        BlocksTC.activatorRail = ConfigBlocks.registerBlock(((Block)new BlockRailPowered().func_149711_c(0.7f).func_149647_a(ConfigItems.TABTC).setRegistryName("thaumcraft", "activator_rail")).func_149663_c("activator_rail"));
        BlocksTC.stabilizer = ConfigBlocks.registerBlock(new BlockStabilizer());
        BlocksTC.visGenerator = ConfigBlocks.registerBlock(new BlockVisGenerator());
        BlocksTC.condenser = ConfigBlocks.registerBlock(new BlockCondenser());
        BlocksTC.condenserlattice = ConfigBlocks.registerBlock(new BlockCondenserLattice(false));
        BlocksTC.condenserlatticeDirty = ConfigBlocks.registerBlock(new BlockCondenserLattice(true));
        BlocksTC.voidSiphon = ConfigBlocks.registerBlock(new BlockVoidSiphon());
        FluidRegistry.registerFluid((Fluid)FluidFluxGoo.instance);
        BlocksTC.fluxGoo = new BlockFluxGoo();
        iForgeRegistry.register((IForgeRegistryEntry)BlocksTC.fluxGoo);
        FluidRegistry.registerFluid((Fluid)FluidDeath.instance);
        FluidRegistry.addBucketForFluid((Fluid)FluidDeath.instance);
        BlocksTC.liquidDeath = new BlockFluidDeath();
        iForgeRegistry.register((IForgeRegistryEntry)BlocksTC.liquidDeath);
        FluidRegistry.registerFluid((Fluid)FluidPure.instance);
        FluidRegistry.addBucketForFluid((Fluid)FluidPure.instance);
        BlocksTC.purifyingFluid = new BlockFluidPure();
        iForgeRegistry.register((IForgeRegistryEntry)BlocksTC.purifyingFluid);
        BlocksTC.hole = ConfigBlocks.registerBlock((Block)new BlockHole());
        BlocksTC.effectShock = ConfigBlocks.registerBlock(new BlockEffect("effect_shock"));
        BlocksTC.effectSap = ConfigBlocks.registerBlock(new BlockEffect("effect_sap"));
        BlocksTC.effectGlimmer = ConfigBlocks.registerBlock(new BlockEffect("effect_glimmer"));
        BlocksTC.placeholderNetherbrick = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_brick"));
        BlocksTC.placeholderObsidian = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_obsidian"));
        BlocksTC.placeholderBars = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_bars"));
        BlocksTC.placeholderAnvil = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_anvil"));
        BlocksTC.placeholderCauldron = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_cauldron"));
        BlocksTC.placeholderTable = ConfigBlocks.registerBlock(new BlockPlaceholder("placeholder_table"));
        BlocksTC.empty = ConfigBlocks.registerBlock(new BlockTranslucent("empty"));
        BlocksTC.barrier = ConfigBlocks.registerBlock(new BlockBarrier());
    }

    public static void initTileEntities() {
        GameRegistry.registerTileEntity(TileArcaneWorkbench.class, (String)"thaumcraft:TileArcaneWorkbench");
        GameRegistry.registerTileEntity(TileDioptra.class, (String)"thaumcraft:TileDioptra");
        GameRegistry.registerTileEntity(TileArcaneEar.class, (String)"thaumcraft:TileArcaneEar");
        GameRegistry.registerTileEntity(TileLevitator.class, (String)"thaumcraft:TileLevitator");
        GameRegistry.registerTileEntity(TileCrucible.class, (String)"thaumcraft:TileCrucible");
        GameRegistry.registerTileEntity(TileNitor.class, (String)"thaumcraft:TileNitor");
        GameRegistry.registerTileEntity(TileFocalManipulator.class, (String)"thaumcraft:TileFocalManipulator");
        GameRegistry.registerTileEntity(TilePedestal.class, (String)"thaumcraft:TilePedestal");
        GameRegistry.registerTileEntity(TileRechargePedestal.class, (String)"thaumcraft:TileRechargePedestal");
        GameRegistry.registerTileEntity(TileResearchTable.class, (String)"thaumcraft:TileResearchTable");
        GameRegistry.registerTileEntity(TileTube.class, (String)"thaumcraft:TileTube");
        GameRegistry.registerTileEntity(TileTubeValve.class, (String)"thaumcraft:TileTubeValve");
        GameRegistry.registerTileEntity(TileTubeFilter.class, (String)"thaumcraft:TileTubeFilter");
        GameRegistry.registerTileEntity(TileTubeRestrict.class, (String)"thaumcraft:TileTubeRestrict");
        GameRegistry.registerTileEntity(TileTubeOneway.class, (String)"thaumcraft:TileTubeOneway");
        GameRegistry.registerTileEntity(TileTubeBuffer.class, (String)"thaumcraft:TileTubeBuffer");
        GameRegistry.registerTileEntity(TileHungryChest.class, (String)"thaumcraft:TileChestHungry");
        GameRegistry.registerTileEntity(TileCentrifuge.class, (String)"thaumcraft:TileCentrifuge");
        GameRegistry.registerTileEntity(TileJarFillable.class, (String)"thaumcraft:TileJar");
        GameRegistry.registerTileEntity(TileJarFillableVoid.class, (String)"thaumcraft:TileJarVoid");
        GameRegistry.registerTileEntity(TileJarBrain.class, (String)"thaumcraft:TileJarBrain");
        GameRegistry.registerTileEntity(TileBellows.class, (String)"thaumcraft:TileBellows");
        GameRegistry.registerTileEntity(TileSmelter.class, (String)"thaumcraft:TileSmelter");
        GameRegistry.registerTileEntity(TileAlembic.class, (String)"thaumcraft:TileAlembic");
        GameRegistry.registerTileEntity(TileInfusionMatrix.class, (String)"thaumcraft:TileInfusionMatrix");
        GameRegistry.registerTileEntity(TileWaterJug.class, (String)"thaumcraft:TileWaterJug");
        GameRegistry.registerTileEntity(TileInfernalFurnace.class, (String)"thaumcraft:TileInfernalFurnace");
        GameRegistry.registerTileEntity(TileThaumatorium.class, (String)"thaumcraft:TileThaumatorium");
        GameRegistry.registerTileEntity(TileThaumatoriumTop.class, (String)"thaumcraft:TileThaumatoriumTop");
        GameRegistry.registerTileEntity(TileSpa.class, (String)"thaumcraft:TileSpa");
        GameRegistry.registerTileEntity(TileLampGrowth.class, (String)"thaumcraft:TileLampGrowth");
        GameRegistry.registerTileEntity(TileLampArcane.class, (String)"thaumcraft:TileLampArcane");
        GameRegistry.registerTileEntity(TileLampFertility.class, (String)"thaumcraft:TileLampFertility");
        GameRegistry.registerTileEntity(TileMirror.class, (String)"thaumcraft:TileMirror");
        GameRegistry.registerTileEntity(TileMirrorEssentia.class, (String)"thaumcraft:TileMirrorEssentia");
        GameRegistry.registerTileEntity(TileRedstoneRelay.class, (String)"thaumcraft:TileRedstoneRelay");
        GameRegistry.registerTileEntity(TileGolemBuilder.class, (String)"thaumcraft:TileGolemBuilder");
        GameRegistry.registerTileEntity(TileEssentiaInput.class, (String)"thaumcraft:TileEssentiaInput");
        GameRegistry.registerTileEntity(TileEssentiaOutput.class, (String)"thaumcraft:TileEssentiaOutput");
        GameRegistry.registerTileEntity(TilePatternCrafter.class, (String)"thaumcraft:TilePatternCrafter");
        GameRegistry.registerTileEntity(TilePotionSprayer.class, (String)"thaumcraft:TilePotionSprayer");
        GameRegistry.registerTileEntity(TileVisGenerator.class, (String)"thaumcraft:TileVisGenerator");
        GameRegistry.registerTileEntity(TileStabilizer.class, (String)"thaumcraft:TileStabilizer");
        GameRegistry.registerTileEntity(TileCondenser.class, (String)"thaumcraft:TileCondenser");
        GameRegistry.registerTileEntity(TileVoidSiphon.class, (String)"thaumcraft:TileVoidSiphon");
        GameRegistry.registerTileEntity(TileBanner.class, (String)"thaumcraft:TileBanner");
        GameRegistry.registerTileEntity(TileHole.class, (String)"thaumcraft:TileHole");
        GameRegistry.registerTileEntity(TileBarrierStone.class, (String)"thaumcraft:TileBarrierStone");
    }

    private static Block registerBlock(Block block) {
        return ConfigBlocks.registerBlock(block, new ItemBlock(block));
    }

    private static Block registerBlock(Block block, ItemBlock itemBlock) {
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)block);
        itemBlock.setRegistryName(block.getRegistryName());
        ForgeRegistries.ITEMS.register((IForgeRegistryEntry)itemBlock);
        Thaumcraft.proxy.registerModel(itemBlock);
        return block;
    }

    private static Block registerBlock(Block block, Class clazz) {
        ForgeRegistries.BLOCKS.register((IForgeRegistryEntry)block);
        try {
            ItemBlock itemBlock = (ItemBlock)clazz.getConstructors()[0].newInstance(block);
            itemBlock.setRegistryName(block.getRegistryName());
            ForgeRegistries.ITEMS.register((IForgeRegistryEntry)itemBlock);
            Thaumcraft.proxy.registerModel(itemBlock);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return block;
    }

    public static final class FluidFluxGoo
    extends Fluid {
        public static final String name = "flux_goo";
        public static final FluidFluxGoo instance = new FluidFluxGoo();

        private FluidFluxGoo() {
            super(name, new ResourceLocation("thaumcraft:blocks/flux_goo"), new ResourceLocation("thaumcraft:blocks/flux_goo"));
            this.setViscosity(6000);
            this.setDensity(8);
        }
    }

    public static final class FluidDeath
    extends Fluid {
        public static final String name = "liquid_death";
        public static final FluidDeath instance = new FluidDeath();

        private FluidDeath() {
            super(name, new ResourceLocation("thaumcraft:blocks/animatedglow"), new ResourceLocation("thaumcraft:blocks/animatedglow"));
            this.setViscosity(1500);
            this.setRarity(EnumRarity.RARE);
        }

        public int getColor() {
            return -263978855;
        }
    }

    public static final class FluidPure
    extends Fluid {
        public static final String name = "purifying_fluid";
        public static final FluidPure instance = new FluidPure();

        private FluidPure() {
            super(name, new ResourceLocation("blocks/water_still"), new ResourceLocation("blocks/water_flow"));
            this.setLuminosity(5);
            this.setRarity(EnumRarity.RARE);
        }

        public int getColor() {
            return 2013252778;
        }
    }
}

