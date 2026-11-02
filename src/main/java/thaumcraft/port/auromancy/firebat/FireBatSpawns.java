package thaumcraft.port.auromancy.firebat;

import com.mojang.serialization.MapCodec;
import java.time.LocalDate;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** ConfigEntities' Nether spawns and October 31 Overworld addition, evaluated when biomes load. */
public record FireBatSpawns() implements BiomeModifier {
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase!=Phase.ADD)return;
        int weight=weight(biome.is(BiomeTags.IS_NETHER),biome.is(BiomeTags.IS_OVERWORLD),LocalDate.now());
        if(weight>0)builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(FireBatContent.FIREBAT.get(),weight,1,2));
    }
    public static int weight(boolean nether,boolean overworld,LocalDate date){return nether?10:overworld&&date.getMonthValue()==10&&date.getDayOfMonth()==31?5:0;}
    @Override public MapCodec<? extends BiomeModifier> codec(){return FireBatContent.SPAWNS.get();}
}
