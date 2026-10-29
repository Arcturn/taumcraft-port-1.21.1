package thaumcraft.port.world;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.port.aspect.Aspect;

/** Replaces the removed Forge BiomeDictionary with data-pack-extensible biome tags. */
public record BiomeAffinity(double life, Aspect aspect, double greatwood) {
    private static boolean tag(Holder<Biome> biome, String id) {
        return biome.is(TagKey.create(Registries.BIOME,ResourceLocation.parse(id)));
    }
    public static BiomeAffinity of(Holder<Biome> b) {
        if (tag(b,"thaumcraft:magical")) return new BiomeAffinity(.75,Aspect.ORDER,1);
        if (tag(b,"minecraft:is_nether")) return new BiomeAffinity(.125,Aspect.FIRE,0);
        if (tag(b,"minecraft:is_end")) return new BiomeAffinity(.125,Aspect.AIR,0);
        if (tag(b,"minecraft:is_ocean")) return new BiomeAffinity(.33,Aspect.WATER,0);
        if (tag(b,"minecraft:is_river")) return new BiomeAffinity(.4,Aspect.WATER,0);
        if (tag(b,"c:is_mushroom")) return new BiomeAffinity(.75,Aspect.ORDER,0);
        if (tag(b,"c:is_swamp")) return new BiomeAffinity(.5,Aspect.ENTROPY,.2);
        if (tag(b,"minecraft:is_jungle")) return new BiomeAffinity(.6,Aspect.EARTH,0);
        if (tag(b,"c:is_cold/overworld")) return new BiomeAffinity(.25,Aspect.ORDER,0);
        if (tag(b,"minecraft:is_taiga")) return new BiomeAffinity(.33,Aspect.EARTH,.2);
        if (tag(b,"minecraft:is_forest")) return new BiomeAffinity(.5,Aspect.EARTH,1);
        if (tag(b,"minecraft:is_badlands")) return new BiomeAffinity(.33,Aspect.FIRE,0);
        if (tag(b,"c:is_desert")) return new BiomeAffinity(.125,Aspect.ENTROPY,0);
        if (tag(b,"minecraft:is_savanna")) return new BiomeAffinity(.25,Aspect.AIR,.2);
        if (tag(b,"minecraft:is_mountain") || tag(b,"minecraft:is_hill")) return new BiomeAffinity(.3,Aspect.AIR,0);
        if (tag(b,"c:is_plains")) return new BiomeAffinity(.3,Aspect.AIR,.2);
        return new BiomeAffinity(.5,Aspect.AIR,0);
    }
}
