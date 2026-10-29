/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.crafting.FurnaceRecipes
 *  net.minecraft.potion.PotionType
 *  net.minecraft.potion.PotionUtils
 *  net.minecraft.util.ResourceLocation
 *  net.minecraftforge.common.IPlantable
 *  net.minecraftforge.common.config.Config
 *  net.minecraftforge.common.config.Config$Comment
 *  net.minecraftforge.common.config.Config$LangKey
 *  net.minecraftforge.common.config.Config$RangeInt
 *  net.minecraftforge.common.config.Config$RequiresMcRestart
 *  net.minecraftforge.common.config.Config$Type
 *  net.minecraftforge.fml.common.registry.ForgeRegistries
 *  net.minecraftforge.fml.common.registry.GameRegistry
 *  net.minecraftforge.oredict.OreDictionary
 */
package thaumcraft.common.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigRecipes;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;
import thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;
import thaumcraft.common.entities.monster.boss.EntityEldritchWarden;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.common.lib.utils.CropUtils;
import thaumcraft.common.lib.utils.Utils;

public class ModConfig {
    public static final float auraSize = 4.0f;
    public static ArrayList<Aspect> aspectOrder = new ArrayList();
    public static boolean foundCopperIngot = false;
    public static boolean foundTinIngot = false;
    public static boolean foundSilverIngot = false;
    public static boolean foundLeadIngot = false;
    public static boolean foundCopperOre = false;
    public static boolean foundTinOre = false;
    public static boolean foundSilverOre = false;
    public static boolean foundLeadOre = false;
    public static boolean isHalloween;

    public static void postInitLoot() {
        boolean COMMON = false;
        boolean UNCOMMON = true;
        int RARE = 2;
        Random rand = new Random(System.currentTimeMillis());
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151074_bl, 1), 2500, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151074_bl, 2), 2250, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151074_bl, 3), 2000, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.salisMundus), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.salisMundus), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.salisMundus), 9, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_185161_cS), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151111_aL), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151106_aX), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1, 7), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1, 7), 3, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1, 6), 1, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1, 5), 9, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1, 3), 3, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.primordialPearl, 1), 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151156_bN), 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151045_i), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151045_i), 50, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151166_bC), 15, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151166_bC), 75, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151043_k), 100, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151079_bi), 100, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.amuletVis, 1, 0), 6, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 0), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 1), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 2), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 3), 5, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 4), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 5), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ItemsTC.baubles, 1, 6), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151062_by), 5, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151062_by), 10, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151062_by), 20, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 1), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 1), 2, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 1), 3, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 0), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 0), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151153_ao, 1, 0), 9, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.field_151122_aG), 10, 0, 1, 2);
        for (PotionType pt : PotionType.field_185176_a) {
            ThaumcraftApi.addLootBagItem(PotionUtils.func_185188_a((ItemStack)new ItemStack((Item)Items.field_151068_bn), (PotionType)pt), 2, 0, 1, 2);
            ThaumcraftApi.addLootBagItem(PotionUtils.func_185188_a((ItemStack)new ItemStack((Item)Items.field_185155_bH), (PotionType)pt), 2, 0, 1, 2);
            ThaumcraftApi.addLootBagItem(PotionUtils.func_185188_a((ItemStack)new ItemStack((Item)Items.field_185156_bI), (PotionType)pt), 2, 1, 2);
        }
        ItemStack[] commonLoot = new ItemStack[]{new ItemStack(ItemsTC.lootBag, 1, 0), new ItemStack(ItemsTC.ingots), new ItemStack(ItemsTC.amber)};
        ItemStack[] uncommonLoot = new ItemStack[]{new ItemStack(ItemsTC.lootBag, 1, 1), new ItemStack(ItemsTC.baubles, 1, 0), new ItemStack(ItemsTC.baubles, 1, 1), new ItemStack(ItemsTC.baubles, 1, 2)};
        ItemStack[] rareLoot = new ItemStack[]{new ItemStack(ItemsTC.lootBag, 1, 2), new ItemStack(ItemsTC.thaumonomicon), new ItemStack(ItemsTC.thaumiumSword), new ItemStack(ItemsTC.thaumiumAxe), new ItemStack(ItemsTC.thaumiumHoe), new ItemStack(ItemsTC.thaumiumPick), new ItemStack(ItemsTC.baubles, 1, 3), new ItemStack(ItemsTC.baubles, 1, 4), new ItemStack(ItemsTC.baubles, 1, 5), new ItemStack(ItemsTC.baubles, 1, 6), new ItemStack(ItemsTC.amuletVis, 1, 0)};
    }

    public static void modCompatibility() {
        Thaumcraft.log.info("Checking for mod & oredict compatibilities");
        ResourceLocation defaultGroup = new ResourceLocation("");
        try {
            if (OreDictionary.doesOreNameExist((String)"oreIron") && OreDictionary.getOres((String)"oreIron", (boolean)false).size() > 1) {
                for (Iterator is : OreDictionary.getOres((String)"oreIron", (boolean)false)) {
                    if (is.func_77973_b() == Item.func_150898_a((Block)Blocks.field_150366_p)) continue;
                    Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 0), 1.0f);
                }
            }
            if (OreDictionary.doesOreNameExist((String)"oreGold") && OreDictionary.getOres((String)"oreGold", (boolean)false).size() > 1) {
                for (Iterator is : OreDictionary.getOres((String)"oreGold", (boolean)false)) {
                    if (is.func_77973_b() == Item.func_150898_a((Block)Blocks.field_150352_o)) continue;
                    Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 1), 1.0f);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (OreDictionary.doesOreNameExist((String)"oreCopper")) {
            for (Iterator is : OreDictionary.getOres((String)"oreCopper", (boolean)false)) {
                Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 2), 1.0f);
                foundCopperOre = true;
            }
        }
        if (OreDictionary.doesOreNameExist((String)"ingotCopper")) {
            boolean first = true;
            for (ItemStack is : OreDictionary.getOres((String)"ingotCopper", (boolean)false)) {
                if (is.func_190916_E() > 1) {
                    is.func_190920_e(1);
                }
                foundCopperIngot = true;
                GameRegistry.addShapedRecipe((ResourceLocation)new ResourceLocation("thaumcraft", "coppertonuggets"), (ResourceLocation)defaultGroup, (ItemStack)new ItemStack(ItemsTC.nuggets, 9, 1), (Object[])new Object[]{"#", Character.valueOf('#'), is});
                if (!first) continue;
                first = false;
                FurnaceRecipes.func_77602_a().func_151394_a(new ItemStack(ItemsTC.clusters, 1, 2), new ItemStack(is.func_77973_b(), 2, is.func_77952_i()), 1.0f);
                ConfigRecipes.oreDictRecipe("coppernuggetstoingot", defaultGroup, is, new Object[]{"###", "###", "###", Character.valueOf('#'), new ItemStack(ItemsTC.nuggets, 1, 1)});
            }
        }
        if (OreDictionary.doesOreNameExist((String)"oreTin")) {
            for (Iterator is : OreDictionary.getOres((String)"oreTin", (boolean)false)) {
                Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 3), 1.0f);
                foundTinOre = true;
            }
        }
        if (OreDictionary.doesOreNameExist((String)"ingotTin")) {
            boolean first = true;
            for (ItemStack is : OreDictionary.getOres((String)"ingotTin", (boolean)false)) {
                if (is.func_190916_E() > 1) {
                    is.func_190920_e(1);
                }
                foundTinIngot = true;
                GameRegistry.addShapedRecipe((ResourceLocation)new ResourceLocation("thaumcraft", "tintonuggets"), (ResourceLocation)defaultGroup, (ItemStack)new ItemStack(ItemsTC.nuggets, 9, 2), (Object[])new Object[]{"#", Character.valueOf('#'), is});
                if (!first) continue;
                first = false;
                FurnaceRecipes.func_77602_a().func_151394_a(new ItemStack(ItemsTC.clusters, 1, 3), new ItemStack(is.func_77973_b(), 2, is.func_77952_i()), 1.0f);
                ConfigRecipes.oreDictRecipe("tinnuggetstoingot", defaultGroup, is, new Object[]{"###", "###", "###", Character.valueOf('#'), new ItemStack(ItemsTC.nuggets, 1, 2)});
            }
        }
        if (OreDictionary.doesOreNameExist((String)"oreSilver")) {
            for (Iterator is : OreDictionary.getOres((String)"oreSilver", (boolean)false)) {
                Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 4), 1.0f);
                foundSilverOre = true;
            }
        }
        if (OreDictionary.doesOreNameExist((String)"ingotSilver")) {
            boolean first = true;
            for (ItemStack is : OreDictionary.getOres((String)"ingotSilver", (boolean)false)) {
                if (is.func_190916_E() > 1) {
                    is.func_190920_e(1);
                }
                foundSilverIngot = true;
                GameRegistry.addShapedRecipe((ResourceLocation)new ResourceLocation("thaumcraft", "silvertonuggets"), (ResourceLocation)defaultGroup, (ItemStack)new ItemStack(ItemsTC.nuggets, 9, 3), (Object[])new Object[]{"#", Character.valueOf('#'), is});
                if (!first) continue;
                first = false;
                FurnaceRecipes.func_77602_a().func_151394_a(new ItemStack(ItemsTC.clusters, 1, 4), new ItemStack(is.func_77973_b(), 2, is.func_77952_i()), 1.0f);
                ConfigRecipes.oreDictRecipe("silvernuggetstoingot", defaultGroup, is, new Object[]{"###", "###", "###", Character.valueOf('#'), new ItemStack(ItemsTC.nuggets, 1, 3)});
            }
        }
        if (OreDictionary.doesOreNameExist((String)"oreLead")) {
            for (Iterator is : OreDictionary.getOres((String)"oreLead", (boolean)false)) {
                Utils.addSpecialMiningResult((ItemStack)is, new ItemStack(ItemsTC.clusters, 1, 5), 1.0f);
                foundLeadOre = true;
            }
        }
        if (OreDictionary.doesOreNameExist((String)"ingotLead")) {
            boolean first = true;
            for (ItemStack is : OreDictionary.getOres((String)"ingotLead", (boolean)false)) {
                if (is.func_190916_E() > 1) {
                    is.func_190920_e(1);
                }
                foundLeadIngot = true;
                GameRegistry.addShapedRecipe((ResourceLocation)new ResourceLocation("thaumcraft", "leadtonuggets"), (ResourceLocation)defaultGroup, (ItemStack)new ItemStack(ItemsTC.nuggets, 9, 4), (Object[])new Object[]{"#", Character.valueOf('#'), is});
                if (!first) continue;
                first = false;
                FurnaceRecipes.func_77602_a().func_151394_a(new ItemStack(ItemsTC.clusters, 1, 5), new ItemStack(is.func_77973_b(), 2, is.func_77952_i()), 1.0f);
                ConfigRecipes.oreDictRecipe("leadnuggetstoingot", defaultGroup, is, new Object[]{"###", "###", "###", Character.valueOf('#'), new ItemStack(ItemsTC.nuggets, 1, 4)});
            }
        }
        Thaumcraft.log.info("Adding entities to MFR safari net blacklist.");
        ModConfig.registerSafariNetBlacklist(EntityOwnedConstruct.class);
        ModConfig.registerSafariNetBlacklist(EntityFallingTaint.class);
        ModConfig.registerSafariNetBlacklist(EntityWisp.class);
        ModConfig.registerSafariNetBlacklist(EntityPech.class);
        ModConfig.registerSafariNetBlacklist(EntityEldritchGuardian.class);
        ModConfig.registerSafariNetBlacklist(EntityEldritchWarden.class);
        ModConfig.registerSafariNetBlacklist(EntityEldritchGolem.class);
        ModConfig.registerSafariNetBlacklist(EntityCultistCleric.class);
        ModConfig.registerSafariNetBlacklist(EntityCultistKnight.class);
        ModConfig.registerSafariNetBlacklist(EntityCultistLeader.class);
        ModConfig.registerSafariNetBlacklist(EntityCultistPortalGreater.class);
        ModConfig.registerSafariNetBlacklist(EntityCultistPortalLesser.class);
        ModConfig.registerSafariNetBlacklist(EntityEldritchCrab.class);
        ModConfig.registerSafariNetBlacklist(EntityInhabitedZombie.class);
    }

    public static void registerSafariNetBlacklist(Class<?> blacklistedEntity) {
        try {
            Class<?> registry = Class.forName("powercrystals.minefactoryreloaded.MFRRegistry");
            if (registry != null) {
                Method reg = registry.getMethod("registerSafariNetBlacklist", Class.class);
                reg.invoke(registry, blacklistedEntity);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void postInitMisc() {
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item == null || !(item instanceof IPlantable)) continue;
            try {
                IBlockState bs = ((IPlantable)item).getPlant(null, null);
                if (bs == null) continue;
                ThaumcraftApi.registerSeed(bs.func_177230_c(), new ItemStack(item));
            }
            catch (Exception bs) {}
        }
        CropUtils.addStandardCrop(Blocks.field_150440_ba, Short.MAX_VALUE);
        CropUtils.addStandardCrop(Blocks.field_150423_aK, Short.MAX_VALUE);
        CropUtils.addStackedCrop((Block)Blocks.field_150436_aH, Short.MAX_VALUE);
        CropUtils.addStackedCrop((Block)Blocks.field_150434_aF, Short.MAX_VALUE);
        CropUtils.addStandardCrop(Blocks.field_150388_bm, 3);
        ThaumcraftApi.registerSeed(Blocks.field_150375_by, new ItemStack(Items.field_151100_aR, 1, EnumDyeColor.BROWN.func_176767_b()));
        Utils.addSpecialMiningResult(new ItemStack(Blocks.field_150366_p), new ItemStack(ItemsTC.clusters, 1, 0), 1.0f);
        Utils.addSpecialMiningResult(new ItemStack(Blocks.field_150352_o), new ItemStack(ItemsTC.clusters, 1, 1), 1.0f);
        Utils.addSpecialMiningResult(new ItemStack(BlocksTC.oreCinnabar), new ItemStack(ItemsTC.clusters, 1, 6), 1.0f);
        Utils.addSpecialMiningResult(new ItemStack(Items.field_151128_bU), new ItemStack(ItemsTC.clusters, 1, 7), 1.0f);
        Collection<Aspect> pa = Aspect.aspects.values();
        for (Aspect aspect : pa) {
            aspectOrder.add(aspect);
        }
    }

    @Config.LangKey(value="thaumcraft.config.misc")
    @Config(modid="thaumcraft", type=Config.Type.INSTANCE, name="thaumcraft_misc")
    public static class CONFIG_MISC {
        @Config.Comment(value={"Setting this to true will make you get the recipe book for salis mundus without having to sleep first."})
        public static boolean noSleep = false;
        @Config.Comment(value={"Setting this to true disables Warp, Taint spread and similar mechanics. You wuss."})
        public static boolean wussMode = false;
        @Config.Comment(value={"Enables a version of the Thauminomicon in creative mode that grants you all the research when you first use it."})
        public static boolean allowCheatSheet = false;
        @Config.Comment(value={"How many milliseconds passes between runic shielding recharge ticks.", "Lower values equals faster recharge. Minimum of 500."})
        @Config.RangeInt(min=500, max=500000)
        public static int shieldRecharge = 2000;
        @Config.Comment(value={"How many milliseconds passes after a shield has been reduced to zero", "before it can start recharging again. Minimum of 0."})
        @Config.RangeInt(min=0, max=50000)
        public static int shieldWait = 4000;
        @Config.Comment(value={"How much vis it costs to reacharge a single unit of shielding. Minimum of 0."})
        @Config.RangeInt(min=0, max=500)
        public static int shieldCost = 1;
    }

    @Config.LangKey(value="thaumcraft.config.world")
    @Config(modid="thaumcraft", type=Config.Type.INSTANCE, name="thaumcraft_world")
    public static class CONFIG_WORLD {
        @Config.Comment(value={"The dimension considered to be your 'overworld'. Certain TC structures will only spawn in this dim."})
        @Config.RequiresMcRestart
        public static int overworldDim = 0;
        @Config.Comment(value={"Outer lands dimension id"})
        @Config.RequiresMcRestart
        public static int dimensionOuterId = -42;
        @Config.Comment(value={"The % of normal ore amounts that will be spawned. For example 50 will spawn half", "the ores while 200 will spawn double. Default 100"})
        @Config.RangeInt(min=1, max=500)
        @Config.RequiresMcRestart
        public static int oreDensity = 100;
        public static boolean generateMagicForest = true;
        @Config.Comment(value={"Higher values increases number of magical forest biomes. If you are using biome", "addon mods you probably want to increase this weight quite a bit"})
        @Config.RangeInt(min=0, max=100)
        @Config.RequiresMcRestart
        public static int biomeMagicalForestWeight = 5;
        @Config.Comment(value={"The % chance of taint fibres spreading on a block tick.", "Setting this to 0 will effectively stop taint fibre spread."})
        @Config.RangeInt(min=0, max=500)
        public static float taintSpreadRate = 100.0f;
        @Config.Comment(value={"The range at which taint can spread from a taint seed.", "This value is only a base and will be modified by flux levels."})
        @Config.RangeInt(min=8, max=256)
        public static int taintSpreadArea = 32;
        public static boolean generateAura = true;
        public static boolean generateStructure = true;
        public static boolean generateCinnabar = true;
        public static boolean generateAmber = true;
        public static boolean generateQuartz = true;
        public static boolean generateCrystals = true;
        public static boolean generateTrees = true;
        @Config.Comment(value={"This key is used to keep track of which chunk have been generated/regenerated.", "Changing it will cause the regeneration code to run again, so only change it if you want it to happen.", "Useful to regen only one world feature at a time."})
        public static String regenKey = "DEFAULT";
        public static boolean regenAura = false;
        public static boolean regenStructure = false;
        public static boolean regenCinnabar = false;
        public static boolean regenAmber = false;
        public static boolean regenQuartz = false;
        public static boolean regenCrystals = false;
        public static boolean regenTrees = false;
        public static boolean allowSpawnAngryZombie = true;
        public static boolean allowSpawnFireBat = true;
        public static boolean allowSpawnWisp = true;
        public static boolean allowSpawnTaintacle = true;
        public static boolean allowSpawnPech = true;
        public static boolean allowSpawnElder = true;
        @Config.Comment(value={"Setting this to false will disable spawning champion mobs. Even when false they will still", "have a greatly reduced chance of spawning in certain dangerous places."})
        public static boolean allowChampionMobs = true;
    }

    @Config.LangKey(value="thaumcraft.config.graphics")
    @Config(modid="thaumcraft", type=Config.Type.INSTANCE, name="thaumcraft_graphics")
    public static class CONFIG_GRAPHICS {
        @Config.Comment(value={"Setting this to true will make the amount text in aspect tags twice as large. Useful for certain resolutions and custom fonts."})
        public static boolean largeTagText = false;
        @Config.Comment(value={"This setting will disable certain thaumcraft shaders for those who experience FPS drops."})
        public static boolean disableShaders = false;
        @Config.Comment(value={"Set to true to disable anxiety triggers like the heartbeat sound."})
        public static boolean nostress = false;
        @Config.Comment(value={"Hate crooked labels, kittens, puppies and all things awesome? If yes, set this to false."})
        public static boolean crooked = true;
        @Config.Comment(value={"Set to true to have the wand dial display in the bottom left instead of the top left."})
        public static boolean dialBottom = false;
        @Config.Comment(value={"Item aspects are hidden by default and pressing shift reveals them.", "Changing this setting to 'true' will reverse this behaviour and always", "display aspects unless shift is pressed."})
        public static boolean showTags = false;
        @Config.Comment(value={"Set this to true to get the old blue magical forest back."})
        public static boolean blueBiome = false;
        @Config.Comment(value={"Will golems display emote particles if they recieve orders or encounter problems"})
        public static boolean showGolemEmotes = true;
    }
}

