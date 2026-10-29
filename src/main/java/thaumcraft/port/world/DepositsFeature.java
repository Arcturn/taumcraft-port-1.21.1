package thaumcraft.port.world;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.port.aspect.Aspect;

/** Original upper-world density, plus a separate proportional pass below Y=0. */
public final class DepositsFeature extends Feature<NoneFeatureConfiguration>{
    public DepositsFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();var r=c.random();int cx=c.origin().getX()&~15,cz=c.origin().getZ()&~15;
        if(l.getLevel().dimension()!=Level.OVERWORLD)return false;
        boolean changed=false;int density=WorldConfig.DENSITY.get();
        if(WorldConfig.ORES.get())for(String ore:new String[]{"ore_cinnabar","ore_quartz","ore_amber"}){
            if(ore.equals("ore_amber")){
                for(int i=0;i<DepositRules.legacyCount(20,density);i++){
                    int x=cx+2+r.nextInt(13),z=cz+2+r.nextInt(13);
                    int y=surface(l,x,z)-r.nextInt(25);
                    changed|=ore(l,new BlockPos(x,y,z),ore);
                }
            }else{
                int layers=ore.equals("ore_cinnabar")?51:64;
                changed|=ores(l,r,cx,cz,ore,DepositRules.upperOre(layers,l.getMinBuildHeight(),l.getMaxBuildHeight(),density));
                changed|=ores(l,r,cx,cz,ore,DepositRules.lowerOre(layers,l.getMinBuildHeight(),l.getMaxBuildHeight(),density));
            }
        }
        if(WorldConfig.CRYSTALS.get()){
            int referenceSurface=surface(l,cx+8,cz+8);
            var upper=DepositRules.upperCrystals(l.getMinBuildHeight(),l.getMaxBuildHeight(),referenceSurface,density);
            changed|=crystals(l,r,cx,cz,false,upper.attempts(),upper.budget());
            // Modern terrain is not a vertical copy of 1.12.2. The center-column height
            // sets the additional band's density; each candidate still uses its own surface.
            var lower=DepositRules.lowerCrystals(l.getMinBuildHeight(),l.getMaxBuildHeight(),referenceSurface,density);
            changed|=crystals(l,r,cx,cz,true,lower.attempts(),lower.budget());
        }
        return changed;
    }
    private static int surface(WorldGenLevel level,int x,int z){
        // TC6 used World.getHeight, which includes water rather than the ocean floor.
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
    }
    private static boolean ores(WorldGenLevel level,RandomSource random,int cx,int cz,String id,DepositRules.Band band){
        if(band.layers()==0)return false;
        boolean changed=false;
        for(int i=0;i<band.attempts();i++){
            int x=cx+2+random.nextInt(13),y=band.minY()+random.nextInt(band.layers()),z=cz+2+random.nextInt(13);
            changed|=ore(level,new BlockPos(x,y,z),id);
        }
        return changed;
    }
    private static boolean ore(WorldGenLevel level,BlockPos pos,String id){
        if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos))return false;
        var old=level.getBlockState(pos);
        // Hosts, not altitude, decide the variant: the transition layer contains both.
        boolean deep=old.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        if(!deep&&!old.is(BlockTags.STONE_ORE_REPLACEABLES))return false;
        var state=BuiltInRegistries.BLOCK.get(WorldContent.id(deep?"deepslate_"+id:id)).defaultBlockState();
        return level.setBlock(pos,state,2);
    }
    private static final Aspect[] PRIMALS={Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY};
    private static boolean crystals(WorldGenLevel level,RandomSource random,int cx,int cz,boolean lower,int attempts,int budget){
        int units=0;boolean changed=false;
        for(int i=0;i<attempts;i++){
            int x=cx+2+random.nextInt(13),z=cz+2+random.nextInt(13),height=surface(level,x,z);
            int low=lower?level.getMinBuildHeight()+1:Math.max(0,level.getMinBuildHeight());
            int high=lower?Math.min(0,Math.min(level.getMaxBuildHeight()-1,height-5))
                    :Math.min(level.getMaxBuildHeight(),DepositRules.crystalLegacyLayers(height));
            if(high<=low)continue;
            var center=new BlockPos(x,low+random.nextInt(high-low),z);
            Aspect aspect=PRIMALS[random.nextInt(6)];
            if(random.nextInt(3)==0){
                aspect=CrystalBiomeAffinity.select(level.getBiome(center),random);
                if(aspect==null)aspect=PRIMALS[random.nextInt(6)];
            }
            var crystal=WorldContent.CRYSTALS.get(aspect).get();
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
                var pos=center.offset(dx,dy,dz);
                if(random.nextInt(3)==0||level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)
                        ||!level.getBlockState(pos).canBeReplaced()||!level.getFluidState(pos).isEmpty())continue;
                var state=crystal.attached(level,pos,crystal.defaultBlockState());
                if(!state.canSurvive(level,pos))continue;
                int size=1+random.nextInt(3);
                if(level.setBlock(pos,state.setValue(CrystalClusterBlock.SIZE,size),2)){units+=size;changed=true;}
            }
            // TC6 counts SIZE (not the SIZE+1 item drop) and finishes the whole cube.
            if(units>budget)break;
        }
        return changed;
    }
}
