/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.item.Item
 *  net.minecraft.item.crafting.IRecipe
 *  net.minecraft.potion.Potion
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.world.biome.Biome
 *  net.minecraft.world.biome.Biome$BiomeProperties
 *  net.minecraftforge.common.BiomeManager
 *  net.minecraftforge.common.BiomeManager$BiomeEntry
 *  net.minecraftforge.common.BiomeManager$BiomeType
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.EventPriority
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.registry.EntityEntry
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package thaumcraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigRecipes;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.potions.PotionBlurredVision;
import thaumcraft.common.lib.potions.PotionDeathGaze;
import thaumcraft.common.lib.potions.PotionInfectiousVisExhaust;
import thaumcraft.common.lib.potions.PotionSunScorned;
import thaumcraft.common.lib.potions.PotionThaumarhia;
import thaumcraft.common.lib.potions.PotionUnnaturalHunger;
import thaumcraft.common.lib.potions.PotionWarpWard;
import thaumcraft.common.world.biomes.BiomeGenEerie;
import thaumcraft.common.world.biomes.BiomeGenEldritch;
import thaumcraft.common.world.biomes.BiomeGenMagicalForest;
import thaumcraft.common.world.biomes.BiomeHandler;
import thaumcraft.proxies.ProxyBlock;

@Mod.EventBusSubscriber
public final class Registrar {
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ConfigBlocks.initBlocks((IForgeRegistry<Block>)event.getRegistry());
        ConfigBlocks.initTileEntities();
        ConfigBlocks.initMisc();
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void registerBlocksClient(RegistryEvent.Register<Block> event) {
        ProxyBlock.setupBlocksClient((IForgeRegistry<Block>)event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ConfigItems.preInitSeals();
        ConfigItems.initItems((IForgeRegistry<Item>)event.getRegistry());
        ConfigItems.initMisc();
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void registerItemsClient(RegistryEvent.Register<Item> event) {
        ConfigItems.initModelsAndVariants();
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        ConfigEntities.initEntities((IForgeRegistry<EntityEntry>)event.getRegistry());
    }

    @SubscribeEvent
    public static void registerVanillaRecipes(RegistryEvent.Register<IRecipe> event) {
        ModConfig.modCompatibility();
        ConfigRecipes.initializeNormalRecipes((IForgeRegistry<IRecipe>)event.getRegistry());
        ConfigRecipes.initializeArcaneRecipes((IForgeRegistry<IRecipe>)event.getRegistry());
        ConfigRecipes.initializeInfusionRecipes();
        ConfigRecipes.initializeAlchemyRecipes();
        ConfigRecipes.initializeCompoundRecipes();
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        PotionFluxTaint.instance = (Potion)new PotionFluxTaint(true, 0x663377).setRegistryName("fluxTaint");
        PotionVisExhaust.instance = (Potion)new PotionVisExhaust(true, 0x664477).setRegistryName("visExhaust");
        PotionInfectiousVisExhaust.instance = (Potion)new PotionInfectiousVisExhaust(true, 0x665577).setRegistryName("infectiousVisExhaust");
        PotionUnnaturalHunger.instance = (Potion)new PotionUnnaturalHunger(true, 0x446633).setRegistryName("unnaturalHunger");
        PotionWarpWard.instance = (Potion)new PotionWarpWard(false, 14742263).setRegistryName("warpWard");
        PotionDeathGaze.instance = (Potion)new PotionDeathGaze(true, 0x664433).setRegistryName("deathGaze");
        PotionBlurredVision.instance = (Potion)new PotionBlurredVision(true, 0x808080).setRegistryName("blurredVision");
        PotionSunScorned.instance = (Potion)new PotionSunScorned(true, 16308330).setRegistryName("sunScorned");
        PotionThaumarhia.instance = (Potion)new PotionThaumarhia(true, 0x664477).setRegistryName("thaumarhia");
        event.getRegistry().register((IForgeRegistryEntry)PotionFluxTaint.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionVisExhaust.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionInfectiousVisExhaust.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionUnnaturalHunger.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionWarpWard.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionDeathGaze.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionBlurredVision.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionSunScorned.instance);
        event.getRegistry().register((IForgeRegistryEntry)PotionThaumarhia.instance);
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        BiomeHandler.MAGICAL_FOREST = new BiomeGenMagicalForest(new Biome.BiomeProperties("Magical Forest").func_185398_c(0.2f).func_185400_d(0.3f).func_185410_a(0.8f).func_185395_b(0.4f));
        event.getRegistry().register((IForgeRegistryEntry)BiomeHandler.MAGICAL_FOREST);
        BiomeHandler.EERIE = new BiomeGenEerie(new Biome.BiomeProperties("Eerie").func_185398_c(0.125f).func_185400_d(0.4f).func_185410_a(0.8f).func_185396_a());
        event.getRegistry().register((IForgeRegistryEntry)BiomeHandler.EERIE);
        BiomeHandler.ELDRITCH = new BiomeGenEldritch(new Biome.BiomeProperties("Outer Lands").func_185398_c(0.125f).func_185400_d(0.15f).func_185410_a(0.8f).func_185395_b(0.2f));
        event.getRegistry().register((IForgeRegistryEntry)BiomeHandler.ELDRITCH);
        BiomeHandler.registerBiomes();
        if (ModConfig.CONFIG_WORLD.generateMagicForest) {
            BiomeManager.addBiome((BiomeManager.BiomeType)BiomeManager.BiomeType.WARM, (BiomeManager.BiomeEntry)new BiomeManager.BiomeEntry(BiomeHandler.MAGICAL_FOREST, ModConfig.CONFIG_WORLD.biomeMagicalForestWeight));
            BiomeManager.addBiome((BiomeManager.BiomeType)BiomeManager.BiomeType.COOL, (BiomeManager.BiomeEntry)new BiomeManager.BiomeEntry(BiomeHandler.MAGICAL_FOREST, ModConfig.CONFIG_WORLD.biomeMagicalForestWeight));
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        SoundsTC.registerSounds(event);
        SoundsTC.registerSoundTypes();
    }
}

