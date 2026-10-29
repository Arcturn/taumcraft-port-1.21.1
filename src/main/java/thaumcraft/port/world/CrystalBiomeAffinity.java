package thaumcraft.port.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.port.aspect.Aspect;

/**
 * TC6 chooses a random biome type for its one-third crystal bias. Modern common
 * tags replace the removed BiomeDictionary; overlapping types retain their weight.
 * This does not change the separate aura or tree suitability rules.
 */
public final class CrystalBiomeAffinity {
    private record Type(Aspect aspect,List<TagKey<Biome>> aliases) {}
    private static Type type(Aspect aspect,String... aliases) {
        return new Type(aspect,Arrays.stream(aliases)
                .map(id->TagKey.create(Registries.BIOME,ResourceLocation.parse(id))).toList());
    }
    private static final List<Type> TYPES=List.of(
            type(Aspect.WATER,"c:is_aquatic"),
            type(Aspect.WATER,"c:is_ocean","minecraft:is_ocean"),
            type(Aspect.WATER,"c:is_river","minecraft:is_river"),
            type(Aspect.WATER,"c:is_wet","c:is_wet/overworld","c:is_wet/nether","c:is_wet/end"),
            type(Aspect.WATER,"c:is_lush"),
            type(Aspect.FIRE,"c:is_hot","c:is_hot/overworld","c:is_hot/nether","c:is_hot/end"),
            type(Aspect.FIRE,"c:is_nether","minecraft:is_nether"),
            type(Aspect.FIRE,"c:is_badlands","minecraft:is_badlands"),
            type(Aspect.FIRE,"c:is_spooky"),
            type(Aspect.ORDER,"c:is_dense_vegetation","c:is_dense_vegetation/overworld","c:is_dense_vegetation/nether","c:is_dense_vegetation/end"),
            type(Aspect.ORDER,"c:is_snowy"),
            type(Aspect.ORDER,"c:is_cold","c:is_cold/overworld","c:is_cold/nether","c:is_cold/end"),
            type(Aspect.ORDER,"c:is_mushroom"),
            type(Aspect.ORDER,"c:is_magical","thaumcraft:magical"),
            type(Aspect.EARTH,"c:is_tree/coniferous","c:is_taiga","minecraft:is_taiga"),
            type(Aspect.EARTH,"c:is_forest","minecraft:is_forest","c:is_taiga","minecraft:is_taiga"),
            type(Aspect.EARTH,"c:is_sandy"),
            type(Aspect.EARTH,"c:is_beach","minecraft:is_beach"),
            type(Aspect.EARTH,"c:is_jungle","minecraft:is_jungle"),
            type(Aspect.AIR,"c:is_savanna","minecraft:is_savanna"),
            type(Aspect.AIR,"c:is_mountain","minecraft:is_mountain"),
            type(Aspect.AIR,"c:is_hill","minecraft:is_hill"),
            type(Aspect.AIR,"c:is_plains"),
            type(Aspect.AIR,"c:is_end","minecraft:is_end"),
            // BiomeHandler registers DRY twice; the final TC6 entry is ENTROPY.
            type(Aspect.ENTROPY,"c:is_dry","c:is_dry/overworld","c:is_dry/nether","c:is_dry/end"),
            type(Aspect.ENTROPY,"c:is_sparse_vegetation","c:is_sparse_vegetation/overworld","c:is_sparse_vegetation/nether","c:is_sparse_vegetation/end"),
            type(Aspect.ENTROPY,"c:is_swamp"),
            type(Aspect.ENTROPY,"c:is_wasteland"),
            type(Aspect.ENTROPY,"c:is_dead"),
            // Forge had these types, but TC6 assigned them no aspect: selecting one falls back.
            type(null,"c:is_rare"),
            type(null,"c:is_void"));

    private CrystalBiomeAffinity() {}

    /** Returns null for an unmapped biome/type; the caller then rolls a fresh random primal, as TC6 did. */
    public static Aspect select(Holder<Biome> biome,RandomSource random) {
        return select(biome::is,random);
    }

    static Aspect select(Predicate<TagKey<Biome>> hasTag,RandomSource random) {
        var candidates=new ArrayList<Aspect>();
        for(var type:TYPES) if(type.aliases().stream().anyMatch(hasTag)) candidates.add(type.aspect());
        return candidates.isEmpty()?null:candidates.get(random.nextInt(candidates.size()));
    }
}
