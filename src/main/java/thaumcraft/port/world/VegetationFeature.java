package thaumcraft.port.world;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class VegetationFeature extends Feature<NoneFeatureConfiguration>{
    public VegetationFeature(){super(NoneFeatureConfiguration.CODEC);}
    private static BlockPos surface(WorldGenLevel l,int x,int z){return new BlockPos(x,l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z),z);}
    public static void flowers(WorldGenLevel l,RandomSource r,BlockPos p,Block flower,int attempts){
        for(int i=0;i<attempts;i++){
            var q=p.offset(r.nextInt(8)-r.nextInt(8),r.nextInt(4)-r.nextInt(4),r.nextInt(8)-r.nextInt(8));
            if(l.ensureCanWrite(q)&&l.isEmptyBlock(q)&&flower.defaultBlockState().canSurvive(l,q))l.setBlock(q,flower.defaultBlockState(),2);
        }
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();var r=c.random();if(c.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource||!WorldConfig.TREES.get()||l.getLevel().dimension()!=Level.OVERWORLD)return false;
        int x=c.origin().getX()&~15,z=c.origin().getZ()&~15;
        var center=surface(l,x+8,z+8);var biome=l.getBiome(center);
        boolean forest=biome.is(ResourceKey.create(Registries.BIOME,WorldContent.id("magical_forest")));
        if(forest){
            if(l.getBlockState(center.below()).is(Blocks.GRASS_BLOCK))l.setBlock(center.below(),WorldContent.block("grass_ambient").defaultBlockState(),2);
            for(int i=0,n=VegetationRules.magicalForestTreeAttempts(r);i<n;i++){
                var p=surface(l,x+r.nextInt(16),z+r.nextInt(16));
                switch(VegetationRules.magicalForestTree(r)){
                    case SILVERWOOD -> MagicTrees.silverwood(l,r,p,true,true);
                    case GREATWOOD -> MagicTrees.greatwood(l,r,p,r.nextInt(8)==0);
                    case BIG_MAGIC -> MagicTrees.bigMagic(l,r,p);
                }
            }
            for(int i=0,n=r.nextInt(3);i<n;i++){
                var p=surface(l,x+r.nextInt(16),z+r.nextInt(16)).below();
                if(!l.getBlockState(p).is(BlockTags.DIRT))continue;
                for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))
                    if(q.distSqr(p)<=2.5)l.setBlock(q,Blocks.MOSSY_COBBLESTONE.defaultBlockState(),2);
            }
            for(int i=0;i<16;i++)if(r.nextInt(40)==0)placeVanilla(c,surface(l,x+r.nextInt(16),z+r.nextInt(16)),r.nextBoolean()?"huge_brown_mushroom":"huge_red_mushroom");
            for(int i=0;i<8;i++){
                var p=surface(l,x+r.nextInt(16),z+r.nextInt(16));if(!l.isEmptyBlock(p))continue;
                boolean wood=false;for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))if(l.getBlockState(q).is(BlockTags.LOGS)){wood=true;break;}
                var plant=WorldContent.block("vishroom").defaultBlockState();if(wood&&plant.canSurvive(l,p))l.setBlock(p,plant,2);
            }
        }
        // TC6's global vegetation pass also runs in magical forest, after its biome decorator.
        if(VegetationRules.rollSilverwood(r)){
            var p=surface(l,VegetationRules.rareTreeCoordinate(x,r),VegetationRules.rareTreeCoordinate(z,r));
            if(r.nextFloat()<BiomeAffinity.of(l.getBiome(p)).greatwood()*.5)MagicTrees.silverwood(l,r,p,true,false);
        }
        if(VegetationRules.rollGreatwood(r)){
            var p=surface(l,VegetationRules.rareTreeCoordinate(x,r),VegetationRules.rareTreeCoordinate(z,r));
            if(r.nextFloat()<BiomeAffinity.of(l.getBiome(p)).greatwood())MagicTrees.greatwood(l,r,p,r.nextInt(8)==0);
        }
        if(r.nextInt(30)==0&&l.getBlockState(center.below()).is(BlockTags.SAND))flowers(l,r,center,WorldContent.block("cinderpearl"),10);
        return true;
    }
    private static void placeVanilla(FeaturePlaceContext<?> c,BlockPos pos,String id){
        c.level().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOptional(ResourceLocation.withDefaultNamespace(id))
                .ifPresent(f->f.place(c.level(),c.chunkGenerator(),c.random(),pos));
    }
}
