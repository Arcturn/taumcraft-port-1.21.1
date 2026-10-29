package thaumcraft.port.world;
import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import terrablender.api.*;
public final class MagicalForestRegion extends Region {
    public MagicalForestRegion(){super(WorldContent.id("magical_forest"),RegionType.OVERWORLD,WorldConfig.FOREST_WEIGHT.get());}
    @Override public void addBiomes(Registry<Biome> registry,Consumer<Pair<Climate.ParameterPoint,ResourceKey<Biome>>> mapper){
        // Retain vanilla climate/terrain mappings outside temperate forests within this region.
        var forest=ResourceKey.create(Registries.BIOME,WorldContent.id("magical_forest"));
        addModifiedVanillaOverworldBiomes(mapper,b->{b.replaceBiome(Biomes.FOREST,forest);b.replaceBiome(Biomes.BIRCH_FOREST,forest);b.replaceBiome(Biomes.OLD_GROWTH_BIRCH_FOREST,forest);});
    }
}
