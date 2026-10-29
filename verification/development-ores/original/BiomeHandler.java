/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.biome.Biome
 *  net.minecraftforge.common.BiomeDictionary
 *  net.minecraftforge.common.BiomeDictionary$Type
 */
package thaumcraft.common.world.biomes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;

public class BiomeHandler {
    public static Biome EERIE;
    public static Biome MAGICAL_FOREST;
    public static Biome ELDRITCH;
    public static HashMap<BiomeDictionary.Type, List> biomeInfo;
    public static Collection<Aspect> c;
    public static ArrayList<Aspect> basicAspects;
    public static ArrayList<Aspect> complexAspects;
    public static HashMap<Integer, Integer> dimensionBlacklist;
    public static HashMap<Integer, Integer> biomeBlacklist;

    public static void registerBiomeInfo(BiomeDictionary.Type type, float auraLevel, Aspect tag, boolean greatwood, float greatwoodchance) {
        biomeInfo.put(type, Arrays.asList(Float.valueOf(auraLevel), tag, greatwood, Float.valueOf(greatwoodchance)));
    }

    public static float getBiomeAuraModifier(Biome biome) {
        try {
            Set types = BiomeDictionary.getTypes((Biome)biome);
            float average = 0.0f;
            int count = 0;
            for (BiomeDictionary.Type type : types) {
                average += ((Float)biomeInfo.get(type).get(0)).floatValue();
                ++count;
            }
            return average / (float)count;
        }
        catch (Exception exception) {
            return 0.5f;
        }
    }

    public static Aspect getRandomBiomeTag(int biomeId, Random random) {
        try {
            Set types = BiomeDictionary.getTypes((Biome)Biome.func_150568_d((int)biomeId));
            BiomeDictionary.Type type = types.toArray(new BiomeDictionary.Type[0])[random.nextInt(types.size())];
            return (Aspect)biomeInfo.get(type).get(1);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public static float getBiomeSupportsGreatwood(int biomeId) {
        try {
            Set types = BiomeDictionary.getTypes((Biome)Biome.func_150568_d((int)biomeId));
            for (BiomeDictionary.Type type : types) {
                if (!((Boolean)biomeInfo.get(type).get(2)).booleanValue()) continue;
                return ((Float)biomeInfo.get(type).get(3)).floatValue();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return 0.0f;
    }

    public static int getFirstFreeBiomeSlot(int old) {
        for (int a = 0; a < Biome.field_185377_q.func_148742_b().size() * 2; ++a) {
            if (Biome.func_150568_d((int)a) != null) continue;
            Thaumcraft.log.warn("Biome slot " + old + " already occupied. Using first free biome slot at " + a);
            return a;
        }
        return -1;
    }

    public static void addDimBlacklist(int dim, int level) {
        dimensionBlacklist.put(dim, level);
    }

    public static int getDimBlacklist(int dim) {
        if (!dimensionBlacklist.containsKey(dim)) {
            return -1;
        }
        return dimensionBlacklist.get(dim);
    }

    public static void addBiomeBlacklist(int biome, int level) {
        biomeBlacklist.put(biome, level);
    }

    public static int getBiomeBlacklist(int biome) {
        if (!biomeBlacklist.containsKey(biome)) {
            return -1;
        }
        return biomeBlacklist.get(biome);
    }

    public static void registerBiomes() {
        BiomeDictionary.addTypes((Biome)EERIE, (BiomeDictionary.Type[])new BiomeDictionary.Type[]{BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.SPOOKY});
        BiomeDictionary.addTypes((Biome)ELDRITCH, (BiomeDictionary.Type[])new BiomeDictionary.Type[]{BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.SPOOKY, BiomeDictionary.Type.END});
        BiomeDictionary.addTypes((Biome)MAGICAL_FOREST, (BiomeDictionary.Type[])new BiomeDictionary.Type[]{BiomeDictionary.Type.MAGICAL, BiomeDictionary.Type.FOREST});
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.WATER, 0.33f, Aspect.WATER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.OCEAN, 0.33f, Aspect.WATER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.RIVER, 0.4f, Aspect.WATER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.WET, 0.4f, Aspect.WATER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.LUSH, 0.5f, Aspect.WATER, true, 0.5f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.HOT, 0.33f, Aspect.FIRE, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.DRY, 0.25f, Aspect.FIRE, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.NETHER, 0.125f, Aspect.FIRE, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.MESA, 0.33f, Aspect.FIRE, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SPOOKY, 0.5f, Aspect.FIRE, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.DENSE, 0.4f, Aspect.ORDER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SNOWY, 0.25f, Aspect.ORDER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.COLD, 0.25f, Aspect.ORDER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.MUSHROOM, 0.75f, Aspect.ORDER, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.MAGICAL, 0.75f, Aspect.ORDER, true, 1.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.CONIFEROUS, 0.33f, Aspect.EARTH, true, 0.2f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.FOREST, 0.5f, Aspect.EARTH, true, 1.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SANDY, 0.25f, Aspect.EARTH, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.BEACH, 0.3f, Aspect.EARTH, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.JUNGLE, 0.6f, Aspect.EARTH, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SAVANNA, 0.25f, Aspect.AIR, true, 0.2f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.MOUNTAIN, 0.3f, Aspect.AIR, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.HILLS, 0.33f, Aspect.AIR, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.PLAINS, 0.3f, Aspect.AIR, true, 0.2f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.END, 0.125f, Aspect.AIR, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.DRY, 0.125f, Aspect.ENTROPY, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SPARSE, 0.2f, Aspect.ENTROPY, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.SWAMP, 0.5f, Aspect.ENTROPY, true, 0.2f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.WASTELAND, 0.125f, Aspect.ENTROPY, false, 0.0f);
        BiomeHandler.registerBiomeInfo(BiomeDictionary.Type.DEAD, 0.1f, Aspect.ENTROPY, false, 0.0f);
    }

    static {
        biomeInfo = new HashMap();
        c = Aspect.aspects.values();
        basicAspects = new ArrayList();
        complexAspects = new ArrayList();
        dimensionBlacklist = new HashMap();
        biomeBlacklist = new HashMap();
    }
}

